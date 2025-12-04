package fit4s.core.internal

import java.time.Instant

import fit4s.core.data.*
import fit4s.core.{MessageReader as MR, *}
import fit4s.profile.CommonMsg
import fit4s.profile.CoursePointMsg
import fit4s.profile.FileType
import fit4s.profile.RecordMsg
import fit4s.profile.SegmentPointMsg

object GpsTrack:
  def fromFitByType(fit: Fit, within: Timespan): Either[String, Vector[LatLng]] =
    fit.fileId.map(_.fileType.ordinal) match
      case Some(FileType.activity) => fromRecordMsg(fit, within)
      case Some(FileType.segment)  => fromSegmentPointMsg(fit, within)
      case Some(FileType.course)   => fromCoursePointMsg(fit, within)
      case _                       => fromAll(fit, within)

  def fromAll(fit: Fit, within: Timespan): Either[String, Vector[LatLng]] =
    for
      frec <- fromRecordMsg(fit, within)
      fcp <- if frec.isEmpty then fromCoursePointMsg(fit, within) else Right(frec)
      fsp <- if fcp.isEmpty then fromSegmentPointMsg(fit, within) else Right(fcp)
    yield fsp

  def fromRecordMsg(fit: Fit, within: Timespan): Either[String, Vector[LatLng]] =
    val reader = MR.field(RecordMsg.timestamp).as[Instant].option ::
      Position
        .reader(RecordMsg.positionLat, RecordMsg.positionLong)
        .tuple
    collect(reader, within, _.toLatLng)(fit.getMessages(RecordMsg))

  def fromCoursePointMsg(fit: Fit, timespan: Timespan): Either[String, Vector[LatLng]] =
    val reader = MR.field(CoursePointMsg.timestamp).as[Instant].option ::
      Position
        .reader(CoursePointMsg.positionLat, CoursePointMsg.positionLong)
        .tuple
    collect(reader, timespan, _.toLatLng)(fit.getMessages(CoursePointMsg))

  def fromSegmentPointMsg(fit: Fit, timespan: Timespan): Either[String, Vector[LatLng]] =
    val reader = MR.field(CommonMsg.timestamp).as[Instant].option ::
      Position
        .reader(SegmentPointMsg.positionLat, SegmentPointMsg.positionLong)
        .tuple
    collect(reader, timespan, _.toLatLng)(fit.getMessages(SegmentPointMsg))

  private def collect[A](
      reader: MR[(Option[Instant], Position)],
      filter: Timespan,
      f: Position => A
  )(list: LazyList[FitMessage]): Either[String, Vector[A]] =
    val buffer = Vector.newBuilder[A]
    val noFilter = filter.isAll
    val iter = list.iterator
    while (iter.hasNext) {
      val fm = iter.next
      reader.read(fm) match
        case Right(Some((tsOpt, pos))) =>
          if (noFilter || tsOpt.forall(filter.contains)) {
            buffer.addOne(f(pos))
          }
        case err @ Left(_) =>
          return err.asInstanceOf[Either[String, Vector[A]]]
        case _ => ()
    }
    Right(buffer.result())
