package fit4s.codec

import fit4s.codec.internal.LMTLookup

import scodec.Err

trait DecodeErr extends Err

object DecodeErr:

  final case class NoDefinitionMessage(
      header: RecordHeader,
      defs: LMTLookup,
      context: List[String] = Nil
  ) extends DecodeErr:
    def pushContext(ctx: String): NoDefinitionMessage =
      NoDefinitionMessage(header, defs, ctx :: context)
    def message: String =
      s"No definition message found for data record: $header (defs=$defs)"

  final case class InvalidFitHeaderSize(size: Int, context: List[String] = Nil)
      extends DecodeErr:
    def pushContext(ctx: String): InvalidFitHeaderSize =
      InvalidFitHeaderSize(size, ctx :: context)
    def message: String = s"FIT file headers of size $size not supported."

  final case class InvalidHeaderCrc(
      provided: Int,
      computed: Int,
      context: List[String] = Nil
  ) extends DecodeErr:
    def pushContext(ctx: String): InvalidHeaderCrc =
      InvalidHeaderCrc(provided, computed, ctx :: context)
    def message: String =
      s"Header crc invalid: provided ${provided} vs computed ${computed}"

  final case class InvalidContentCrc(
      provided: Int,
      computed: Int,
      context: List[String] = Nil
  ) extends DecodeErr:
    def pushContext(ctx: String): InvalidContentCrc =
      InvalidContentCrc(provided, computed, ctx :: context)
    def message: String =
      s"Content crc invalid: provided ${provided} vs computed ${computed}"
