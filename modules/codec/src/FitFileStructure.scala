package fit4s.codec

import fit4s.codec.internal.Codecs
import fit4s.codec.internal.Crc
import fit4s.codec.internal.FitFileCodec

import scodec.*
import scodec.bits.ByteVector

final case class FitFileStructure(
    header: FileHeader,
    headerBytes: ByteVector,
    records: ByteVector,
    crc: Int,
    crcBytes: ByteVector
):
  def encoded: ByteVector =
    FitFileStructure.codec.encode(this).map(_.bytes).require

  lazy val computedCrc: Int =
    Crc(headerBytes ++ records)

  def isCrcValid: Boolean =
    header.isCrcValid && computedCrc == crc

  def checkCrc: Option[Err] =
    header.checkCrc match
      case None =>
        Option.unless(isCrcValid)(DecodeErr.InvalidContentCrc(crc, computedCrc))
      case r @ Some(_) => r

  def updateCrc: FitFileStructure =
    copy(
      crcBytes = Codecs.crc.encode(computedCrc).require.bytes,
      crc = computedCrc,
      header = header.updateCrc
    )

  def updateHeader: FitFileStructure =
    copy(headerBytes = header.encoded)

object FitFileStructure:

  val codec: Codec[FitFileStructure] =
    FitFileCodec.fitStructureCodec

  /** Decode bytes into a single fit fie structure. */
  def decode(fit: ByteVector): Attempt[FitFileStructure] =
    codec.complete.decodeValue(fit.bits)

  /** Check the given bytes if it is a fit file by decoding it repeatedly (for chained fit
    * files).
    */
  def checkFitFile(bv: ByteVector): Option[Err] =
    val (err, _) = codec.decodeAll(_ => ())((), (_, _) => ())(bv.bits)
    err

  /** Check whether the given bytes is a valid fit file. */
  def isFitFile(bv: ByteVector): Boolean = checkFitFile(bv).isEmpty

  /** Decodes the bytes into a fit structure and validates the crc. */
  def checkIntegrity(fit: ByteVector): Option[Err] =
    val (err1, err2) = codec.decodeAll(_.checkCrc)(None, (a, b) => a.orElse(b))(fit.bits)
    err1.orElse(err2)
