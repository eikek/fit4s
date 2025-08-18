# fit4s

This is a library for Scala 3 for decoding and encoding FIT files.

The codecs are written using
[scodec](https://github.com/scodec/scodec).

FIT files are defined by garmin,
https://developer.garmin.com/fit/protocol/. The fit file specification
is only one part to fully obtain the data, the second part is a
"profile" which defines the meaning for the data in a fit file.

The library is comprised of the following modules:

## codec

The codec module defines the codecs for reading and writing fit files.
It has a single dependency on the scodec library. This module has no
profile information and therefore allows to decode fit files but can
only give limited knowledge on the decoded data.

The `FitFile` represents a decoded fit file. With `FitFile.read` a fit
file can be decoded:

```scala
import fit4s.codec.*

val testFile = TestData.Activities.moxyFr735
// testFile: TestFile = fit4s.codec.TestData$TestFile@613b2454
val fr735 = FitFile.read(testFile.contents).require.head
// fr735: FitFile = FitFile(
//   header = FileHeader(
//     protocolVersion = 16,
//     profileVersion = 2006,
//     dataSize = 4106L,
//     crc = 48432
//   ),
//   records = Vector(
//     DataRecord(
//       header = NormalRecordHeader(
//         messageTypeSpecific = false,
//         localMessageType = 4
//       ),
//       definition = DefinitionMessage(
//         meta = Meta(byteOrder = LittleEndian, globalMessageNum = 34),
//         fields = List(
//           FieldDef(
//             fieldDefNumber = 253,
//             sizeBytes = 4,
//             baseType = FieldBaseType(endianAbility = true, baseTypeNum = 6)
//           ),
//           FieldDef(
//             fieldDefNumber = 0,
//             sizeBytes = 4,
//             baseType = FieldBaseType(endianAbility = true, baseTypeNum = 6)
//           ),
//           FieldDef(
//             fieldDefNumber = 5,
//             sizeBytes = 4,
//             baseType = FieldBaseType(endianAbility = true, baseTypeNum = 6)
//           ),
//           FieldDef(
//             fieldDefNumber = 1,
//             sizeBytes = 2,
//             baseType = FieldBaseType(endianAbility = true, baseTypeNum = 4)
//           ),
//           FieldDef(
//             fieldDefNumber = 2,
//             sizeBytes = 1,
//             baseType = FieldBaseType(endianAbility = false, baseTypeNum = 0)
//           ),
//           FieldDef(
//             fieldDefNumber = 3,
//             sizeBytes = 1,
//             baseType = FieldBaseType(endianAbility = false, baseTypeNum = 0)
//           ),
//           FieldDef(
//             fieldDefNumber = 4,
//             sizeBytes = 1,
// ...
```

A fit file may contain many concatenated fit files. The data is in the
`records` vector. It contains definition and data messages. Each data
message is described by a corresponding definition message.

```scala
// 0 is the FileId message
val msg = fr735.findMessages(0).head
// msg: DataRecord = DataRecord(
//   header = NormalRecordHeader(messageTypeSpecific = false, localMessageType = 0),
//   definition = DefinitionMessage(
//     meta = Meta(byteOrder = LittleEndian, globalMessageNum = 0),
//     fields = List(
//       FieldDef(
//         fieldDefNumber = 3,
//         sizeBytes = 4,
//         baseType = FieldBaseType(endianAbility = true, baseTypeNum = 12)
//       ),
//       FieldDef(
//         fieldDefNumber = 4,
//         sizeBytes = 4,
//         baseType = FieldBaseType(endianAbility = true, baseTypeNum = 6)
//       ),
//       FieldDef(
//         fieldDefNumber = 7,
//         sizeBytes = 4,
//         baseType = FieldBaseType(endianAbility = true, baseTypeNum = 6)
//       ),
//       FieldDef(
//         fieldDefNumber = 1,
//         sizeBytes = 2,
//         baseType = FieldBaseType(endianAbility = true, baseTypeNum = 4)
//       ),
//       FieldDef(
//         fieldDefNumber = 2,
//         sizeBytes = 2,
//         baseType = FieldBaseType(endianAbility = true, baseTypeNum = 4)
//       ),
//       FieldDef(
//         fieldDefNumber = 5,
//         sizeBytes = 2,
//         baseType = FieldBaseType(endianAbility = true, baseTypeNum = 4)
//       ),
//       FieldDef(
//         fieldDefNumber = 0,
//         sizeBytes = 1,
//         baseType = FieldBaseType(endianAbility = false, baseTypeNum = 0)
//       )
//     ),
//     devFields = List()
//   ),
//   lastTimestamp = None,
//   fields = Vector(
//     TypedDataField(
//       meta = Meta(byteOrder = LittleEndian, globalMessageNum = 0),
//       fieldDef = FieldDef(
// ...
```

This example gets the field 1, which defines the manufacturer. The
value 1 means "Garmin".

```scala
msg.fieldData(1)
// res0: Option[Vector[FitBaseValue]] = Some(value = Vector(1))
```

While `FitFile.read` decodes into data structures, if you only want to
verify the integrity, `FitFileStructure` can be used.

```scala
FitFileStructure.checkIntegrity(TestData.Activities.fenix539.contents)
// res1: Option[Err] = None
FitFileStructure.checkIntegrity(TestData.Corrupted.badCrc.contents)
// res2: Option[Err] = Some(
//   value = InvalidHeaderCrc(provided = 13793, computed = 4833, context = List())
// )
```


## profile

The profile module contains data structures that describe the "global
profile". This data is necessary to make sense of a decoded fit file.
For example, it provides constants for message types and its expected
fields with their field names and other data that allows to interpret
the values in correctly.

A fit message may contain all or a subset of the described fields in
the corresponding profile.

For example, the `FileIdMsg` has these fields in its profile:

```scala
import fit4s.profile.*

FileIdMsg.allFields
// res3: Map[Int, MsgField] = HashMap(
//   0 -> MsgField(
//     fieldDefNum = 0,
//     fieldName = "type",
//     profileType = Some(value = <function1>),
//     baseTypeName = "enum",
//     components = List(),
//     scale = List(),
//     offset = 0.0,
//     units = List(),
//     bits = List(),
//     subFields = List()
//   ),
//   5 -> MsgField(
//     fieldDefNum = 5,
//     fieldName = "number",
//     profileType = None,
//     baseTypeName = "uint16",
//     components = List(),
//     scale = List(),
//     offset = 0.0,
//     units = List(),
//     bits = List(),
//     subFields = List()
//   ),
//   1 -> MsgField(
//     fieldDefNum = 1,
//     fieldName = "manufacturer",
//     profileType = Some(value = <function1>),
//     baseTypeName = "uint16",
//     components = List(),
//     scale = List(),
//     offset = 0.0,
//     units = List(),
//     bits = List(),
//     subFields = List()
//   ),
//   2 -> MsgField(
//     fieldDefNum = 2,
//     fieldName = "product",
//     profileType = None,
//     baseTypeName = "uint16",
//     components = List(),
//     scale = List(),
//     offset = 0.0,
//     units = List(),
//     bits = List(),
//     subFields = List(
//       SubField(
// ...
```
Here you can see that field number `1` denotes the manufacturer. The type `enum` means that the number in the fit file corresponds to some value in the profile type "manufacturer". The `ManufacturerType` can be used to lookup the value:

```scala
ManufacturerType.values(1)
// res4: String = "garmin"
```
## core

The core module combines the previous modules to create a more
convenient api for reading fit files. It wraps a `FitFile` into a
`Fit` class and a data record into a `FitMessage` class. Then you can
get information about a field by providing the appropriate profile
information.

```scala
import fit4s.core.*

val fenix539 = Fit.read(TestData.Activities.fenix539.contents).require.head
// fenix539: Fit = fit4s.core.Fit@4f8b6189
val fileIdMsg = fenix539.getMessages(FileIdMsg).toVector.head
// fileIdMsg: FitMessage = FitMessage(
//   mesgNum = 0,
//   schema = HashMap(
//     0 -> MsgField(
//       fieldDefNum = 0,
//       fieldName = "type",
//       profileType = Some(value = <function1>),
//       baseTypeName = "enum",
//       components = List(),
//       scale = List(),
//       offset = 0.0,
//       units = List(),
//       bits = List(),
//       subFields = List()
//     ),
//     5 -> MsgField(
//       fieldDefNum = 5,
//       fieldName = "number",
//       profileType = None,
//       baseTypeName = "uint16",
//       components = List(),
//       scale = List(),
//       offset = 0.0,
//       units = List(),
//       bits = List(),
//       subFields = List()
//     ),
//     1 -> MsgField(
//       fieldDefNum = 1,
//       fieldName = "manufacturer",
//       profileType = Some(value = <function1>),
//       baseTypeName = "uint16",
//       components = List(),
//       scale = List(),
//       offset = 0.0,
//       units = List(),
//       bits = List(),
//       subFields = List()
//     ),
//     2 -> MsgField(
//       fieldDefNum = 2,
//       fieldName = "garmin_product",
//       profileType = Some(value = <function1>),
//       baseTypeName = "uint16",
//       components = List(),
//       scale = List(),
//       offset = 0.0,
//       units = List(),
//       bits = List(),
// ...
val manu = fileIdMsg.field(FileIdMsg.manufacturer)
// manu: Option[FieldValue] = Some(
//   value = FieldValue(
//     fieldName = "manufacturer",
//     fieldNumber = 1,
//     baseType = Uint16(4, byte=0x84, size=2),
//     profileType = Some(value = <function1>),
//     unit = None,
//     data = Vector(1)
//   )
// )
val value = manu.asEnum
// value: Option[ProfileEnum] = Some(
//   value = ProfileEnum(profile = <function1>, value = "garmin")
// )
```

The `field` method gets a `FieldValue` from the message, that combines
the profile information and the data from the fit file. Each field my
be interpreted differently. The `manufacturer` is a value from a
predefined enumeration. The enumeration is already known by the
profile. The call `asEnum` looks up the value from the appropriate
enumaration and returns a `ProfileEnum` value.

Other values can be obtained via `as[<type>]` calls. This requires a
`FieldReader` instance to be in scope. For example, the file-id
message also contains a serial number which can be seen as a long:

```scala
fileIdMsg.field(FileIdMsg.serialNumber).as[Long]
// res5: Option[Either[String, Long]] = Some(
//   value = Right(value = 3945007650L)
// )
```

In the same way if there is a `MessageReader` instance in scope, the
complete message can be converted into some value. There is a `FileId`
case class defined that provides such a reader.

```scala
fileIdMsg.as[FileId]
// res6: Either[String, Option[FileId]] = Right(
//   value = Some(
//     value = FileId(
//       fileType = ProfileEnum(profile = <function1>, value = "activity"),
//       manufacturer = ProfileEnum(profile = <function1>, value = "garmin"),
//       product = Some(
//         value = ProfileEnum(profile = <function1>, value = "fenix5")
//       ),
//       serialNumber = Some(value = 3945007650L),
//       createdAt = Some(value = 2019-01-06T17:49:49Z),
//       number = None,
//       productName = None
//     )
//   )
// )
```

## cli

Finally a small cli application exists for demoing. It can read fit
files and convert it into JSON or create a HTML file to view the data.

Try with `--help` to find out more.
