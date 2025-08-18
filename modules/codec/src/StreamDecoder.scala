package fit4s.codec

import fit4s.codec.StreamDecoder.*
import fit4s.codec.internal.Codecs
import fit4s.codec.internal.DecodingContext
import fit4s.codec.internal.FileHeaderCodec
import fit4s.codec.internal.RecordCodec

import scodec.*
import scodec.bits.BitVector
import scodec.bits.ByteVector

/** Allows to decode fit files in a streaming way.
  *
  * The `FitPartConsumer` receives each decoded part of the fit file. The decoding keeps
  * going until the consumer signals to stop by returning `false`.
  */
object StreamDecoder:
  sealed trait FitPart
  final case class FitHeader(header: FileHeader) extends FitPart
  final case class FitRecord(record: Record) extends FitPart
  final case class FitCrc(crc: Int) extends FitPart

  trait FitPartConsumer:
    def onPart(part: FitPart): Boolean
    def onError(err: Err): Unit
    def onDone(): Unit

  object FitPartConsumer {
    def apply(
        onHeader: FileHeader => Boolean = _ => true,
        onRecord: Record => Boolean = _ => true,
        onErr: Err => Unit = _ => (),
        done: => Unit = ()
    ): FitPartConsumer =
      new FitPartConsumer {
        def onDone(): Unit = done
        def onError(err: Err): Unit = onErr(err)
        def onPart(part: FitPart): Boolean = part match
          case FitHeader(h) => onHeader(h)
          case FitRecord(r) => onRecord(r)
          case FitCrc(c)    => true
      }
  }

  def decode(source: ByteSource, cont: FitPartConsumer): Unit = {
    val decoder = FitPartDecoder()
    decode0(source, cont, decoder)
  }

  @annotation.tailrec
  private def decode0(
      source: ByteSource,
      cont: FitPartConsumer,
      decoder: FitPartDecoder
  ): Unit =
    val next = source.next
    if (next.isEmpty) cont.onDone()
    else
      Codecs.collectUntilErr[Vector, FitPart](decoder, next.bits) match
        case Attempt.Successful(DecodeResult(values, remain)) =>
          val keep = values.forall(cont.onPart)
          if (keep) {
            decode0(source.prepend(remain), cont, decoder)
          }

        case Attempt.Failure(_: Err.InsufficientBits) =>
          decode0(source.prepend(next), cont, decoder)

        case Attempt.Failure(err) =>
          cont.onError(err)
          cont.onDone()

  private class FitPartDecoder extends Decoder[FitPart] {
    var state: State = State()

    def decode(bits: BitVector): Attempt[DecodeResult[FitPart]] =
      state.nextToken match
        case NextToken.Header =>
          FileHeaderCodec.decoder(false).decode(bits).map { result =>
            val n = result.map(part)
            state = state.update(n, bits)
            n
          }

        case NextToken.Record =>
          RecordCodec.recordDecoder(state).decode(bits).map { result =>
            val n = result.map(part)
            state = state.update(n, bits)
            n
          }

        case NextToken.Crc =>
          Codecs.crc.decode(bits).map { result =>
            val n = result.map(part)
            state = state.update(n, bits)
            n
          }

    def part(header: FileHeader): FitPart = FitHeader(header)
    def part(record: Record): FitPart = FitRecord(record)
    def part(crc: Int): FitPart = FitCrc(crc)
  }

  private enum NextToken:
    case Header
    case Record
    case Crc

  private case class State(
      nextToken: NextToken = NextToken.Header,
      currentFile: Option[FileHeader] = None,
      bytesRead: ByteSize = ByteSize.zero,
      ctx: DecodingContext.Ctx = DecodingContext()
  ) extends DecodingContext {
    override def toString(): String =
      s"State(ctx=${ctx}, current=$currentFile, read=$bytesRead, next=$nextToken)"

    def getDefinition(n: Int): Option[DefinitionMessage] = ctx.getDefinition(n)
    def getDevFieldDescription(fd: DevFieldDef) = ctx.getDevFieldDescription(fd)
    def lastTimestamp: Option[Long] = ctx.lastTimestamp

    def update(res: DecodeResult[FitPart], previous: BitVector): State =
      res.value match
        case FitHeader(header) =>
          State(NextToken.Record, Some(header), ByteSize.zero)
        case FitRecord(record) =>
          val read =
            bytesRead + (ByteSize.bits(previous.length) - ByteSize.bits(
              res.remainder.length
            ))
          val done = currentFile.exists(_.dataSize == read)
          State(
            if done then NextToken.Crc else NextToken.Record,
            currentFile,
            read,
            ctx.update(record)
          )
        case FitCrc(_) =>
          State()
  }

  extension (self: ByteSource)
    def prepend(bv: BitVector): ByteSource =
      if (bv.isEmpty) self
      else self.prepend(bv.bytes)
    def prepend(bv: ByteVector): ByteSource =
      if (bv.isEmpty) self
      else
        self match
          case c: ConcatByteSource => c.prepend(bv)
          case s                   => ConcatByteSource(bv, s)

  private class ConcatByteSource(var prefix: ByteVector, s: ByteSource)
      extends ByteSource {
    def next: ByteVector =
      if prefix.isEmpty then s.next
      else {
        val n = prefix ++ s.next
        prefix = ByteVector.empty
        n
      }

    def close(): Unit = s.close()

    def prepend(bv: ByteVector): ConcatByteSource =
      ConcatByteSource(bv ++ prefix, s)
    def prepend(bv: BitVector): ConcatByteSource =
      ConcatByteSource(bv.bytes ++ prefix, s)
  }
