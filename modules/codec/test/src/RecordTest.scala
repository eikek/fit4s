package fit4s.codec

import fit4s.codec.TypedDevField.FieldDescription
import fit4s.codec.internal.{DecodingContext, RecordCodec}

import munit.FunSuite
import scodec.bits.BitVector
import scodec.bits.ByteOrdering
import scodec.bits.ByteVector
import scodec.bits.hex

class RecordTest extends FunSuite:
  def constantMsg(dm: DefinitionMessage, lastTs: Option[Long] = None): DecodingContext =
    new DecodingContext {
      def getDefinition(lmt: Int): Option[DefinitionMessage] = Some(dm)
      def getDevFieldDescription(fd: DevFieldDef): Option[FieldDescription] = None
      def lastTimestamp: Option[Long] = lastTs
    }

  def recordCodec(dm: DefinitionMessage) =
    RecordCodec.record(constantMsg(dm))
  def dataRecordCodec(dm: DefinitionMessage, lastTs: Option[Long] = None) =
    RecordCodec.dataRecord(constantMsg(dm, lastTs))

  test("read definition record bytes"):
    val hexString =
      "40" + // record header
        "00" + // reserved byte
        "00" + // architecture
        "0000" + // global message number
        "07" + // field count (7)
        "03048c 040486 070486 010284 020284 050284 000100" // 7 3-byte fields

    val data = BitVector.fromValidHex(hexString)
    val result = RecordCodec.defRecord.decode(data).require
    val dfm = result.value
    assert(result.remainder.isEmpty)
    assertEquals(dfm.header, NormalRecordHeader(false, 0))
    assertEquals(dfm.message.meta.byteOrder, ByteOrdering.LittleEndian)
    assertEquals(dfm.message.devFields, Nil)
    assertEquals(dfm.message.meta.globalMessageNum, 0)
    assertEquals(dfm.message.dataMessageSize, ByteSize.bytes(19))
    assertEquals(dfm.message.fields.size, 7)

  test("read definintion record 2"):
    val data = hex"40 00 00 0000 06 010284 020284 03048c 040486 050284 000100"
    val result = RecordCodec.defRecord.complete.decodeValue(data.bits).require
    assertEquals(result.header, NormalRecordHeader(false, 0))
    assertEquals(result.message.meta.byteOrder, ByteOrdering.LittleEndian)
    assertEquals(result.message.meta.globalMessageNum, 0)
    assertEquals(result.message.fields.size, 6)
    assertEquals(result.message.devFields.size, 0)
    assertEquals(result.message.dataMessageSize, ByteSize.bytes(15))

  test("read data record"):
    val dmmBytes =
      hex"45000016000afd0486000102010102020102040102050100060102070102080102090102"
    val dataBytes = hex"05b3b6052c0101010206ffffffff"
    val dmmRec = RecordCodec.defRecord.complete
      .decodeValue(dmmBytes.bits)
      .require

    assertEquals(dmmRec.header, NormalRecordHeader(false, 5))
    assertEquals(dmmRec.message.meta.byteOrder, ByteOrdering.LittleEndian)
    assertEquals(dmmRec.message.meta.globalMessageNum, 22)
    assertEquals(dmmRec.message.fields.size, 10)
    assertEquals(dmmRec.message.devFields.size, 0)
    assertEquals(dmmRec.message.dataMessageSize, ByteSize.bytes(13))

    val ddmRec = dataRecordCodec(dmmRec.message).complete
      .decodeValue(dataBytes.bits)
      .require

    assertEquals(ddmRec.header, NormalRecordHeader(false, 5))
//    assertEquals(ddmRec.bytes, dataBytes.drop(1))
    assertEquals(ddmRec.definition, dmmRec.message)

    val cc = recordCodec(dmmRec.message).complete
    cc.decodeValue(dmmBytes.bits)
      .require
      .fold(_ => (), r => fail(s"Expected definition record, got: $r"))
    cc.decodeValue(dataBytes.bits)
      .require
      .fold(r => fail(s"Expected data record, got: $r"), _ => ())

  test("read compressed ts data record"):
    val dmmBytes = hex"43000014000308030d030102040102"
    val ddmBytes = hex"f848d0025120"

    val dmmRec = RecordCodec.defRecord.complete
      .decodeValue(dmmBytes.bits)
      .require
    assertEquals(dmmRec.header, NormalRecordHeader(false, 3))
    assertEquals(dmmRec.message.meta.byteOrder, ByteOrdering.LittleEndian)
    assertEquals(dmmRec.message.meta.globalMessageNum, 20)
    assertEquals(dmmRec.message.fields.size, 3)
    assertEquals(dmmRec.message.devFields.size, 0)
    assertEquals(dmmRec.message.dataMessageSize, ByteSize.bytes(5))

    val ddmRec = dataRecordCodec(dmmRec.message).complete
      .decodeValue(ddmBytes.bits)
      .require
    assertEquals(ddmRec.header, CompressedTimestampHeader(3, 24))
    assertEquals(ddmRec.definition, dmmRec.message)

    val cc = recordCodec(dmmRec.message).complete
    cc.decodeValue(dmmBytes.bits)
      .require
      .fold(_ => (), r => fail(s"Expected definition record, got: $r"))
    cc.decodeValue(ddmBytes.bits)
      .require
      .fold(r => fail(s"Expected data record, got: $r"), _ => ())
