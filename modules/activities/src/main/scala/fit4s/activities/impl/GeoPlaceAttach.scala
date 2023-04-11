package fit4s.activities.impl

import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.GeoLookup
import fit4s.activities.data._
import fit4s.activities.records.{RActivityGeoPlace, RActivitySession}
import fit4s.data.Position

final class GeoPlaceAttach[F[_]: Async](xa: Transactor[F], geoLookup: GeoLookup[F]) {

  def attachGeoPlace(id: ActivityId): F[List[ActivityGeoPlaceId]] =
    RActivityGeoPlace.findByActivity(id).transact(xa).flatMap {
      case Nil =>
        PositionName.all.toList.flatTraverse(attachPosition(id, _))

      case end :: Nil if end.name == PositionName.End =>
        attachPosition(id, PositionName.Start).map(end.id :: _)

      case start :: Nil if start.name == PositionName.Start =>
        attachPosition(id, PositionName.End).map(start.id :: _)

      case list => list.map(_.id).pure[F]
    }

  private def attachPosition(id: ActivityId, name: PositionName) =
    for {
      pos <- getPositions(id, name)
      pIds <- pos.traverse(t => geoLookup.lookup(t._2).map(t._1 -> _))
      res <- pIds.flatTraverse { case (sessionId, optPlace) =>
        optPlace match {
          case Some(p) =>
            (RActivityGeoPlace.delete(sessionId, name.some) *>
              RActivityGeoPlace.insert(sessionId, p.id, name))
              .transact(xa)
              .map(List(_))
          case None =>
            List.empty[ActivityGeoPlaceId].pure[F]
        }
      }
    } yield res

  private def getPositions(
      id: ActivityId,
      name: PositionName
  ): F[List[(ActivitySessionId, Position)]] =
    name match {
      case PositionName.Start => RActivitySession.getStartPositions(id).transact(xa)
      case PositionName.End   => RActivitySession.getEndPositions(id).transact(xa)
    }
}
