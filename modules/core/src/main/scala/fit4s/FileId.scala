package fit4s

import java.time.Instant

/** The file_id message identifies the format/content of the FIT file (type field) and the
  * combination of fields provides a globally unique identifier for the file. Each FIT
  * file should contain one and only one file_id message. If the combination of type,
  * manufacturer, product and serial_number is insufficient, for example on a device
  * supporting multiple device files, the time_created or number fields must be populated
  * to differentiate the files.
  *
  * If the file is created offline (for example using a PC application) the file_id fields
  * could be set as per the creating application or to the values of the destination
  * device, if known. If the file is in an intermediate state, only type need be set, so
  * long as the other fields are later updated by the target device.
  */
final case class FileId(
    file: Int,
    manufacturer: Int,
    serialNumber: Long,
    created: Instant,
    number: Int
)
