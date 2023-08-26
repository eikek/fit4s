package fit4s

import scodec._
import scodec.codecs._

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
case class FileHeader(
    protocolVersion: Short,
    profileVersion: Int,
    dataSize: Long,
    dataType: String,
    crc: Int
)

object FileHeader {

  val codec: Codec[FileHeader] = {
    val fields = ushort8 :: uint16L :: uint32L :: fixedSizeBits(32, ascii)
    val zeroCrc = provide(0)
    val crc = uint16L.withContext("FIT file crc")

    ushort8
      .consume[FileHeader] {
        case 12 =>
          fields.flatAppend(_ => zeroCrc).as[FileHeader]
        case 14 =>
          fields.flatAppend(_ => crc).as[FileHeader]
        case n =>
          fail(Err(s"FIT file headers of size $n not supported."))
      }(_ => 14)
      .withContext("FIT file header")
  }

}
