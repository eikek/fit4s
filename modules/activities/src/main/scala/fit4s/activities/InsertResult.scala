package fit4s.activities

import cats.Monoid
import cats.data.{NonEmptyList, NonEmptySet}

trait InsertResult extends Product {

  def combine(other: InsertResult): InsertResult

}

object InsertResult {

  final case object Success extends InsertResult {
    def combine(other: InsertResult): InsertResult =
      other
  }
  final case class Failure(reasons: NonEmptyList[FailureReason]) extends InsertResult {
    def combine(other: InsertResult): InsertResult =
      other match {
        case Success       => this
        case Failure(errs) => Failure(reasons.concatNel(errs))
      }
  }

  sealed trait FailureReason extends Product
  object FailureReason {
    final case class Duplicate(ids: NonEmptyList[String]) extends FailureReason
    final case class Error(errors: NonEmptyList[Throwable]) extends FailureReason

    def error(ex: Throwable): FailureReason = Error(NonEmptyList.of(ex))
    def duplicate(id: String): FailureReason = Duplicate(NonEmptyList.of(id))
  }

  implicit val monoid: Monoid[InsertResult] =
    Monoid.instance(Success, _ combine _)
}
