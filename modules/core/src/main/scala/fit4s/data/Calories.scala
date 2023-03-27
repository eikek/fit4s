package fit4s.data

final class Calories private (val kcal: Double) extends AnyVal {

  def +(c: Calories): Calories = new Calories(kcal + c.kcal)

  override def toString: String = f"$kcal%.2fkcal"
}

object Calories {
  val zero: Calories = new Calories(0)

  def kcal(n: Double): Calories = new Calories(n)
  def cal(n: Double): Calories = new Calories(n / 1000.0)

  implicit val ordering: Ordering[Calories] =
    Ordering.by(_.kcal)
}
