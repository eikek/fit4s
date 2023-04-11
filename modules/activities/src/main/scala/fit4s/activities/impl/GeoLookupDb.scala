package fit4s.activities.impl

import cats.effect._
import cats.effect.std.Semaphore
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.GeoLookup
import fit4s.activities.data.GeoPlaceId
import fit4s.activities.records.RGeoPlace
import fit4s.data.Position
import fit4s.geocode.{Place, ReverseLookup}

class GeoLookupDb[F[_]: Sync](
    reverseLookup: ReverseLookup[F],
    xa: Transactor[F],
    writeSem: Semaphore[F]
) extends GeoLookup[F] {

  private val distanceThresholdKm = 1.5d

  def lookup(position: Position): F[Option[RGeoPlace]] =
    for {
      fromDB <- RGeoPlace.findByPosition(position).transact(xa)
      fromWs <- fromDB match {
        case Some((p, dst)) if dst < distanceThresholdKm => p.some.pure[F]
        case _ =>
          reverseLookup.lookup(position).flatMap {
            case Some(p) => insertPlace(p)
            case None    => Option.empty[RGeoPlace].pure[F]
          }
      }
    } yield fromWs

  private def insertPlace(p: Place) =
    writeSem.permit.use { _ =>
      // response may have different position which may already in db
      RGeoPlace.findByPlace(p).transact(xa).flatMap {
        case Some(r) => r.some.pure[F]
        case None =>
          RGeoPlace.fromPlace(GeoPlaceId(-1), p) match {
            case Some(record) =>
              RGeoPlace
                .insert(record)
                .transact(xa)
                .map(id => record.copy(id = id).some)

            case None =>
              Option.empty[RGeoPlace].pure[F]
          }
      }
    }
}

object GeoLookupDb {
  def apply[F[_]: Async](
      reverseLookup: ReverseLookup[F],
      xa: Transactor[F]
  ): F[GeoLookup[F]] =
    Semaphore[F](1).map(sem => new GeoLookupDb[F](reverseLookup, xa, sem))
}
