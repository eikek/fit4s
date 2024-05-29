package fit4s.data

final class TrainingStressScore(val tss: Double) extends AnyVal:
  def /(div: Double): TrainingStressScore = new TrainingStressScore(tss / div)

  override def toString = s"Tss($tss)"

object TrainingStressScore:
  def tss(tss: Double): TrainingStressScore = new TrainingStressScore(tss)

  given Numeric[TrainingStressScore] =
    NumericFrom[TrainingStressScore, Double](_.tss, TrainingStressScore.tss)
