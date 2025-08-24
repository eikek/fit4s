package fit4s.core
package data

import fit4s.profile.MeasurementUnit

opaque type TrainingStressScore = Double

object TrainingStressScore:
  val zero: TrainingStressScore = 0
  def tss(tss: Double): TrainingStressScore = tss

  extension (self: TrainingStressScore)
    def value: Double = self
    def +(s: TrainingStressScore): TrainingStressScore = s + self
    def /(d: Double): TrainingStressScore = self / d
    def asString = s"${self}tss"
    private def ord: Ordered[TrainingStressScore] =
      Ordered.orderingToOrdered(self)(using Ordering[TrainingStressScore])
    export ord.*

  given Numeric[TrainingStressScore] = Numeric.DoubleIsFractional
  given FieldReader[TrainingStressScore] =
    for
      _ <- FieldReader.unit(MeasurementUnit.TrainingStressScore)
      v <- FieldReader.firstAsDouble
    yield v
  given Display[TrainingStressScore] = Display.instance(_.asString)
