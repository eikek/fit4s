package fit4s.codec
package internal

trait LMTLookup:
  /** Lookup a definition message given a local message type. */
  def getDefinition(lmt: Int): Option[DefinitionMessage]

trait DevFieldLookup:
  /** Lookup a field description given a developer field definition. */
  def getDevFieldDescription(fd: DevFieldDef): Option[TypedDevField.FieldDescription]

trait TimestampLookup:
  /** Lookup the most recent decoded timestamp value. */
  def lastTimestamp: Option[Long]

enum MsgType:
  case Data
  case Definition
  def toBool = if (this == Data) false else true

object MsgType:
  def fromBool(flag: Boolean): MsgType = if (flag) MsgType.Definition else MsgType.Data

enum HeaderType:
  case Normal
  case Compressed
  def toBool = fold(false, true)
  def fold[A](fn: => A, fc: => A): A = if this == Normal then fn else fc

object HeaderType:
  def fromBool(flag: Boolean) = if (flag) HeaderType.Compressed else HeaderType.Normal
  def fromHeader(rh: RecordHeader) = rh.fold(_ => Normal, _ => Compressed)
