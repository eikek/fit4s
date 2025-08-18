import java.math.MathContext

def countDigits(d: Double, res: Int): Int =
  if (math.abs(d) > 1) countDigits(d / 10, res + 1)
  else res

def round(num: Double) =
  val digits = countDigits(num, 0)
  BigDecimal.binary(num, MathContext(digits + 5))

val d = 38.49999996833503
val d2 = 38.4999286489
val d3 = 38.4999886489
val d4 = -179.9832104

// val bd1 = round(d)
// val bd2 = round(d2)
// val bd3 = round(d3)
val bd4 = round(d4)
