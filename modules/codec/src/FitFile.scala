package fit4s.codec

import fit4s.codec.internal.FitFileCodec

import scodec.Attempt
import scodec.bits.ByteVector

final case class FitFile(
    header: FileHeader,
    records: Vector[Record],
    crc: Int
):

  /** Encode this fit file into its binary representation.
    *
    * @param rawEncoding
    *   if true, the data is encoded as given. otherwise (the default) encoding will
    *   calculate the correct crcs and header size from the data
    * @return
    */
  def encoded(rawEncoding: Boolean = false): Attempt[ByteVector] =
    val enc =
      if rawEncoding then FitFileCodec.fitFileEncoderRaw
      else FitFileCodec.fitFileEncoder
    enc.encode(this).map(_.bytes)

  /** Return all data records groupd by its global message type and sorted by their
    * timestamp.
    */
  lazy val groupByMsg: Map[Int, Vector[DataRecord]] =
    records
      .flatMap(_.toEither.toOption)
      .sortBy(_.timestamp.getOrElse(Long.MinValue))
      .groupBy(_.globalMessage)

  def findMessages(global: Int): Vector[DataRecord] =
    groupByMsg.getOrElse(global, Vector.empty)

object FitFile:

  /** Read fit files from a given binary representation.
    *
    * @param bv
    * @param checkCrc
    *   if true (the default) decoding fails if the crc is invalid
    * @return
    */
  def read(bv: ByteVector, checkCrc: Boolean = true): Attempt[Vector[FitFile]] =
    FitFileCodec.fitFilesDecoder(checkCrc).complete.decodeValue(bv.bits)
