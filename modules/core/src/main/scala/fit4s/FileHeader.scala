package fit4s

import scodec._
import scodec.codecs._

/** The file header provides information about the FIT File. The
  * minimum size of the file header is 12 bytes including protocol and
  * profile version numbers, the amount of data contained in the file
  * and data type signature.  The 12 byte header is considered legacy,
  * using the 14 byte header is preferred.  The header size should
  * always be decoded before attempting to interpret a FIT file,
  * Dynastream may extend the header as necessary.  Computing the CRC
  * is optional when using a 14 byte file header, it is permissible to
  * set it to 0x0000.  Including the CRC in the file header allows the
  * CRC of the file to be computed as the file is being written when
  * the amount of data to be contained in the file is not known. Table
  * 3-1 outlines the FIT file header format.
  */
case class FileHeader(
  headerSize: Short
    , protocolVersion: Short
    , profileVersion: Int
    , dataSize: Long
    , dataType: String
    , crc: Int)

object FileHeader {

  val codec = (ushort8 ::  // headerSize
    ushort8 :: // protocol version
    uint16L :: // profile version
    uint32L :: // data size
    fixedSizeBits(32, ascii) :: // ascii file type
    uint16L // crc
  ).as[FileHeader]

}
