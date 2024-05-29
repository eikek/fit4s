package fit4s.activities

import fit4s.activities.data.ActivityId
import fit4s.data.FileId

final case class StravaExternalId(
    activityId: ActivityId,
    fileId: FileId
):

  def asString: String =
    s"fit4s_${activityId.id}_${fileId.asString}"

object StravaExternalId:

  def fromString(str: String): Either[String, StravaExternalId] =
    str.trim.split('_').toList match
      case "fit4s" :: aId :: fId_ :: Nil =>
        for {
          actId <- aId.toLongOption.toRight(s"Invalid activity id: $aId")
          fId = if (fId_.toLowerCase.endsWith(".fit")) fId_.dropRight(4) else fId_
          fileId <- FileId.fromString(fId)
        } yield StravaExternalId(ActivityId(actId), fileId)

      case _ =>
        Left(s"Invalid external strava id: $str")
