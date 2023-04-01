package fit4s.activities

import fit4s.activities.ImportResult.Failure
import fit4s.activities.ImportResult.FailureReason.{ActivityDecodeError, FitReadError}
import fit4s.data.FileId
import scodec.Err

trait ImportResult[+A] extends Product {
  def map[B](f: A => B): ImportResult[B]

  def toEither: Either[Failure, A]
}

object ImportResult {

  final case class Success[A](value: A) extends ImportResult[A] {
    def map[B](f: A => B): ImportResult[B] = Success(f(value))
    def toEither: Either[Failure, A] = Right(value)
  }

  final case class Failure(reason: FailureReason) extends ImportResult[Nothing] {
    def map[B](f: Nothing => B): ImportResult[B] = this
    def toEither: Either[Failure, Nothing] = Left(this)
    def messages: String =
      reason.toString
  }

  sealed trait FailureReason extends Product
  object FailureReason {
    final case class Duplicate(ids: FileId, path: String) extends FailureReason
    final case class FitReadError(err: Err) extends FailureReason
    final case class ActivityDecodeError(message: String) extends FailureReason
  }

  def duplicate[A](id: FileId, path: String): ImportResult[A] = Failure(
    FailureReason.Duplicate(id, path)
  )

  def readFitError[A](err: Err): ImportResult[A] = Failure(FitReadError(err))

  def activityDecodeError[A](msg: String): ImportResult[A] = Failure(
    ActivityDecodeError(msg)
  )

  def success[A](value: A): ImportResult[A] = Success(value)
}
