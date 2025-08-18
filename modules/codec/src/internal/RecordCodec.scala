package fit4s.codec
package internal

import scodec.bits.ByteOrdering
import scodec.{Codec, Decoder, Encoder}

private[codec] object RecordCodec:
  private val cc = Codecs
  val normalHeader: Codec[NormalRecordHeader] =
    ((cc.bool <~ cc.reservedBit) :: cc.localMessageType).as[NormalRecordHeader]

  val compressedTsHeader: Codec[CompressedTimestampHeader] =
    (cc.uint2 :: cc.ushort5).as[CompressedTimestampHeader]

  private def fieldBaseType(bo: ByteOrdering) =
    ((cc.bool <~ cc.reservedBits(2)) :: cc.baseTypeNum(bo)).as[FieldBaseType]

  private def fieldDef(bo: ByteOrdering): Codec[FieldDef] =
    (cc.fieldDefNum(bo) :: cc.fieldSize(bo) :: fieldBaseType(bo)).as[FieldDef]

  private def devFieldDef(bo: ByteOrdering): Codec[DevFieldDef] =
    (cc.fieldDefNum(bo) :: cc.fieldSize(bo) :: cc.devDataIdx(bo)).as[DevFieldDef]

  private def meta: Codec[DefinitionMessage.Meta] =
    cc.reservedByte ~> cc.byteOrder
      .flatPrepend(cc.globalMsgNum(_).tuple)
      .as[DefinitionMessage.Meta]

  def definitionMsg(withDevData: Boolean): Codec[DefinitionMessage] =
    meta
      .flatPrepend { meta =>
        val bo = meta.byteOrder
        val fields = cc.listOfN(cc.fieldCount(bo), fieldDef(bo))
        if (withDevData) fields :: cc.listOfN(cc.fieldCount(bo), devFieldDef(bo))
        else fields :: cc.provide(List.empty[DevFieldDef])
      }
      .as[DefinitionMessage]

  val defMsgHeader: Codec[NormalRecordHeader] =
    cc.headerType.unit(HeaderType.Normal) ~> cc.msgType.unit(
      MsgType.Definition
    ) ~> normalHeader

  val defRecord =
    defMsgHeader
      .flatPrepend(h => definitionMsg(h.developerDataFlag).tuple)
      .as[DefinitionRecord]

  val dataRecordHeader: Codec[RecordHeader] =
    cc.headerType.consume(
      _.fold(
        cc.msgType.unit(MsgType.Data) ~> normalHeader.upcast[RecordHeader],
        compressedTsHeader.upcast[RecordHeader]
      )
    )(HeaderType.fromHeader)

  val dataRecordEncoder: Encoder[DataRecord] =
    (dataRecordHeader :: DataFieldCodec.fieldsEncoder.encodeOnly).contramap[DataRecord](
      r => (r.header, r.fields)
    )

  def dataRecordDecoder(ctx: DecodingContext): Decoder[DataRecord] =
    dataRecordHeader.flatPrepend(h => dataRecordParts(h, ctx)).as[DataRecord]

  def dataRecord(ctx: DecodingContext): Codec[DataRecord] =
    Codec(dataRecordEncoder, dataRecordDecoder(ctx))

  val recordEncoder: Encoder[Record] =
    Encoder[Record] {
      case dmr: DefinitionRecord => defRecord.encode(dmr)
      case ddr: DataRecord       => dataRecordEncoder.encode(ddr)
    }

  def recordDecoder(ctx: DecodingContext): Decoder[Record] =
    cc.peekMsgType.flatMap {
      case MsgType.Data       => dataRecordDecoder(ctx)
      case MsgType.Definition => defRecord.asDecoder
    }

  def record(ctx: DecodingContext): Codec[Record] =
    Codec(recordEncoder, recordDecoder(ctx))

  private def dataRecordParts(
      header: RecordHeader,
      ctx: DecodingContext
  ): Codec[(DefinitionMessage, Option[Long], Vector[DataField], Vector[DevField])] =
    ctx.getDefinition(header.localMessageType) match
      case None =>
        cc.fail(DecodeErr.NoDefinitionMessage(header, ctx))
      case Some(dm) =>
        val fieldBytes = dm.fieldsSize.toBytes.toInt
        val devBytes = dm.devFieldsSize.toBytes.toInt
        cc.provide(dm) :: cc.provide(ctx.lastTimestamp) ::
          cc.fixedSizeBytes(fieldBytes, DataFieldCodec.codec(dm)) ::
          cc.fixedSizeBytes(devBytes, DevFieldCodec.codec(dm, ctx))
