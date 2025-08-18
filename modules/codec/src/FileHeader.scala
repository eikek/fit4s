package fit4s.codec

import fit4s.codec.internal.{Crc, FileHeaderCodec}

import scodec.*
import scodec.bits.ByteVector

/** The file header provides information about the FIT File. The minimum size of the file
  * header is 12 bytes including protocol and profile version numbers, the amount of data
  * contained in the file and data type signature. The 12 byte header is considered
  * legacy, using the 14 byte header is preferred. The header size should always be
  * decoded before attempting to interpret a FIT file, Garmin International, Inc. may
  * extend the header as necessary. Computing the CRC is optional when using a 14 byte
  * file header, it is permissible to set it to 0x0000. Including the CRC in the file
  * header allows the CRC of the file to be computed as the file is being written when the
  * amount of data to be contained in the file is not known. Table 1 outlines the FIT file
  * header format.
  *
  * @param profileVersion
  *   Protocol version number as provided in SDK
  * @param protocolVersion
  *   Profile version number as provided in SDK
  * @param dataSize
  *   Length of the Data Records section in bytes. Does not include Header or CRC
  * @param dataType
  *   ASCII values for “.FIT”. A FIT binary file opened with a text editor will contain a
  *   readable “.FIT” in the first line.
  * @param crc
  *   Contains the value of the CRC (see CRC ) of Bytes 0 through 11, or may be set to
  *   0x0000. This field is optional.
  */
final case class FileHeader(
    protocolVersion: Short,
    profileVersion: Int,
    dataSize: ByteSize,
    crc: Int
):

  lazy val computedCrc = Crc(encoded.dropRight(2))

  def updateCrc: FileHeader = copy(crc = computedCrc)

  def isCrcValid: Boolean =
    crc == 0 || computedCrc == crc

  def encoded: ByteVector =
    FileHeaderCodec.encoder.encode(this).map(_.bytes).require

  def checkCrc: Option[Err] =
    Option.unless(isCrcValid)(DecodeErr.InvalidHeaderCrc(crc, computedCrc))

object FileHeader:
  /** The header codec. If `checkCrc` is true decoding fails if an invalid crc is
    * encountered.
    */
  def codec(checkCrc: Boolean): Codec[FileHeader] =
    FileHeaderCodec.codec(checkCrc)

  /** Decode from a byte vector. No bytes must be remain after decoding, it will result in
    * an error otherwise.
    *
    * @param bv
    *   the input byte vector
    * @param checkCrc
    *   Only applies to decoding. If true, decoding fails when an invalid crc is found.
    */
  def decode(bv: ByteVector, checkCrc: Boolean = true): Attempt[FileHeader] =
    FileHeaderCodec.decoder(checkCrc).complete.decodeValue(bv.bits)
