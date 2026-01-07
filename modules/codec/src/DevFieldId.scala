package fit4s.codec

opaque type DevFieldId = Int

object DevFieldId:
  private val marker = 1 << 16

  def apply(devDataIdx: Short, fieldDefNum: Int): DevFieldId =
    val n = marker | (devDataIdx << 8)
    n | fieldDefNum

  def apply(f: TypedDevField.FieldDescription): DevFieldId =
    apply(f.devIndex, f.fieldDefNum)

  def apply(f: TypedDevField): DevFieldId =
    apply(f.fieldDescription)

  def apply(fd: DevFieldDef): DevFieldId =
    apply(fd.devIndex, fd.fieldDefNumber)

  def isDevFieldId(n: Int): Boolean =
    (n & marker) != 0

  def fromInt(n: Int): Option[DevFieldId] =
    if (n & marker) == 0 then None else Some(n)

  extension (key: DevFieldId)
    def extracted: (Short, Short) =
      val devIndex = (key & (marker - 1)) >> 8
      val fieldDefNum = key & 0x00ff
      (devIndex.toShort, fieldDefNum.toShort)

    def devIndex: Short =
      val (devIdx, _) = extracted
      devIdx

    def fieldDefNum: Short =
      val (_, fd) = extracted
      fd

    def toInt: Int = key
