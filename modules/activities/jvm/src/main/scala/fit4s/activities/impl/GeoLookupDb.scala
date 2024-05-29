package fit4s.activities.impl

import cats.effect.*
import cats.effect.std.Semaphore
import cats.syntax.all.*

import fit4s.activities.GeoLookup
import fit4s.activities.data.{GeoPlace, GeoPlaceId}
import fit4s.activities.records.RGeoPlace
import fit4s.data.Position
import fit4s.geocode.ReverseLookup
import fit4s.geocode.data.Place

import doobie.*
import doobie.implicits.*

class GeoLookupDb[F[_]: Sync](
    reverseLookup: ReverseLookup[F],
    xa: Transactor[F],
    writeSem: Semaphore[F]
) extends GeoLookup[F]:
  private val logger = scribe.cats.effect[F]

  private val distanceThresholdKm = 1.5d

  def lookup(position: Position): F[Option[GeoPlace]] =
    for {
      fromDB <- RGeoPlace.findByPosition(position).transact(xa)
      fromWs <- fromDB match
        case Some((p, dst)) if dst < distanceThresholdKm => p.some.pure[F]
        case _ =>
          reverseLookup
            .lookup(position)
            .flatMap:
              case Some(p) => insertPlace(p)
              case None =>
                logger
                  .info(s"GeoLookup empty for position: $position")
                  .as(Option.empty[GeoPlace])
    } yield fromWs

  private def insertPlace(p: Place) =
    writeSem.permit.use { _ =>
      // response may have different position which may already in db
      RGeoPlace
        .findByPlace(p)
        .transact(xa)
        .flatMap:
          case Some(r) => r.some.pure[F]
          case None =>
            RGeoPlace.fromPlace(GeoPlaceId(-1), p) match
              case Some(record) =>
                RGeoPlace
                  .insert(record)
                  .transact(xa)
                  .map(id => record.copy(id = id).some)

              case None =>
                Option.empty[GeoPlace].pure[F]
    }

object GeoLookupDb:
  def apply[F[_]: Async](
      reverseLookup: ReverseLookup[F],
      xa: Transactor[F]
  ): F[GeoLookup[F]] =
    Semaphore[F](1).map(sem => new GeoLookupDb[F](reverseLookup, xa, sem))
