package fit4s.activities

import cats.Show
import cats.syntax.all.*

import fit4s.ActivityReader
import fit4s.activities.ImportResult.Failure
import fit4s.activities.ImportResult.FailureReason.{ActivityDecodeError, FitReadError}
import fit4s.activities.data.ActivityId
import fit4s.data.FileId

import scodec.Err

trait ImportResult[+A] extends Product:
  def map[B](f: A => B): ImportResult[B]

  def toEither: Either[Failure, A]

object ImportResult:

  final case class Success[A](value: A) extends ImportResult[A]:
    def map[B](f: A => B): ImportResult[B] = Success(f(value))
    def toEither: Either[Failure, A] = Right(value)

  final case class Failure(reason: FailureReason) extends ImportResult[Nothing]:
    def map[B](f: Nothing => B): ImportResult[B] = this
    def toEither: Either[Failure, Nothing] = Left(this)
    def messages: String =
      reason.toString

  sealed trait FailureReason extends Product
  object FailureReason:
    final case class Duplicate(id: ActivityId, fileId: FileId, path: String)
        extends FailureReason
    final case class FitReadError(err: Err) extends FailureReason
    final case class TcxError(err: Throwable) extends FailureReason
    final case class ActivityDecodeError(err: ActivityReader.Failure)
        extends FailureReason

    implicit val show: Show[FailureReason] =
      Show.show:
        case _: Duplicate      => "Duplicate"
        case FitReadError(err) => err.messageWithContext
        case ActivityDecodeError(_: ActivityReader.Failure.NoActivityFound) =>
          "No activity"
        case ActivityDecodeError(_: ActivityReader.Failure.NoFileIdFound) =>
          "No file_id message"
        case ActivityDecodeError(ActivityReader.Failure.GeneralError(msg)) => msg
        case TcxError(err) =>
          s"TCX file failed to read: ${err.getMessage}"

  def duplicate[A](
      activityId: ActivityId,
      fileId: FileId,
      path: String
  ): ImportResult[A] = Failure(
    FailureReason.Duplicate(activityId, fileId, path)
  )

  def readFitError[A](err: Err): ImportResult[A] = Failure(FitReadError(err))

  def activityDecodeError[A](err: ActivityReader.Failure): ImportResult[A] = Failure(
    ActivityDecodeError(err)
  )

  def tcxError[A](ex: Throwable): ImportResult[A] = Failure(FailureReason.TcxError(ex))

  def success[A](value: A): ImportResult[A] = Success(value)

  implicit def show[A]: Show[ImportResult[A]] =
    Show.show:
      case _: Success[_]   => "Ok"
      case Failure(reason) => reason.show
