package fit4s.data

final class TrainingStressScore(val tss: Double) extends AnyVal {

  override def toString = s"Tss($tss)"
}

object TrainingStressScore {
  def tss(tss: Double): TrainingStressScore = new TrainingStressScore(tss)

  implicit val ordering: Ordering[TrainingStressScore] =
    Ordering.by(_.tss)
}
