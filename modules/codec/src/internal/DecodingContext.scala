package fit4s.codec
package internal

import fit4s.codec.TypedDevField.FieldDescription

trait DecodingContext extends LMTLookup with DevFieldLookup with TimestampLookup

object DecodingContext:
  def apply(): Ctx = Ctx()

  val empty: DecodingContext = new DecodingContext {
    def getDefinition(n: Int) = None
    def getDevFieldDescription(fd: DevFieldDef) = None
    val lastTimestamp: Option[Long] = None
  }

  final case class Ctx(
      defs: Map[Int, DefinitionRecord] = Map.empty,
      fieldDescr: Map[DevFieldId, FieldDescription] = Map.empty,
      lastTimestamp: Option[Long] = None
  ) extends DecodingContext:
    def getDefinition(n: Int) =
      defs.get(n).map(_.message)

    def getDevFieldDescription(fd: DevFieldDef) =
      fieldDescr.get(fd.key)

    def updateDefinition(r: DefinitionRecord): Ctx =
      copy(defs = defs.updated(r.localMessageType, r))

    def updateData(r: DataRecord): Ctx =
      val nextTs = r.timestamp.orElse(lastTimestamp)
      DeveloperProfile.fieldDescription(r) match
        case None     => copy(lastTimestamp = nextTs)
        case Some(fd) =>
          copy(fieldDescr = fieldDescr.updated(fd.key, fd), lastTimestamp = nextTs)

    def update(r: Record): Ctx =
      r.fold(updateDefinition, updateData)

    override def toString(): String =
      s"DecodeContext(${defs.keySet}, ${fieldDescr.keySet}, $lastTimestamp)"
