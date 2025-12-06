package fit4s.codec

sealed trait ValueAdjust:
  def toOption: Option[(Double, Double)]
  def apply(bt: FitBaseType)(v: FitBaseValue): FitBaseValue =
    if bt == FitBaseType.Enum then v
    else
      this match
        case ValueAdjust.None                  => v
        case ValueAdjust.Adjust(scale, offset) =>
          v match
            case n: Byte   => (n / scale) - offset
            case n: Int    => (n / scale) - offset
            case n: Long   => (n / scale) - offset
            case f: Double => (f / scale) - offset
            case s: String => s

  def reverse(bt: FitBaseType)(v: FitBaseValue): FitBaseValue =
    if bt == FitBaseType.Enum then v
    else
      this match
        case ValueAdjust.None                  => v
        case ValueAdjust.Adjust(scale, offset) =>
          v match
            case n: Byte   => (n + offset) * scale
            case n: Int    => (n + offset) * scale
            case n: Long   => (n + offset) * scale
            case f: Double => (f + offset) * scale
            case s: String => s

object ValueAdjust:
  private case object None extends ValueAdjust {
    val toOption: Option[(Double, Double)] = Option.empty
  }
  private case class Adjust(scale: Double, offset: Double) extends ValueAdjust {
    val toOption: Option[(Double, Double)] = Some((scale, offset))
  }

  def from(
      scales: List[Double],
      offsets: List[Double],
      targetSize: Int
  ): List[ValueAdjust] =
    if scales.forall(_ == 1) && offsets.forall(_ == 0)
    then List.fill(targetSize)(None)
    else
      val ls = LazyList.continually(scales).flatten
      val lo = LazyList.continually(offsets).flatten
      ls.zip(lo).take(targetSize).map(Adjust.apply).toList

  def apply(scale: Double, offset: Double): ValueAdjust =
    if (scale == 1 && offset == 0) None
    else Adjust(scale, offset)

  def applyAll(
      ft: FitBaseType,
      adjust: Iterable[ValueAdjust],
      data: Vector[FitBaseValue]
  ): Vector[FitBaseValue] =
    val adjusts = LazyList.from(adjust).concat(LazyList.continually(None))
    data.zip(adjusts).map { case (v, ad) => ad(ft)(v) }

  def reverseAll(
      ft: FitBaseType,
      adjust: Iterable[ValueAdjust],
      data: Vector[FitBaseValue]
  ): Vector[FitBaseValue] =
    val adjusts = LazyList.from(adjust).concat(LazyList.continually(None))
    data.zip(adjusts).map { case (v, ad) => ad.reverse(ft)(v) }
