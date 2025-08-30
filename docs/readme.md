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

```scala mdoc
import fit4s.codec.*

val testFile = TestData.Activities.moxyFr735
val fr735 = FitFile.read(testFile.contents).require.head
```

A fit file may contain many concatenated fit files. The data is in the
`records` vector. It contains definition and data messages. Each data
message is described by a corresponding definition message.

```scala mdoc
// 0 is the FileId message
val msg = fr735.findMessages(0).head
```

This example gets the field 1, which defines the manufacturer. The
value 1 means "Garmin".

```scala mdoc
msg.fieldData(1)
```

While `FitFile.read` decodes into data structures, if you only want to
verify the integrity, `FitFileStructure` can be used.

```scala mdoc
FitFileStructure.checkIntegrity(TestData.Activities.fenix539.contents)
FitFileStructure.checkIntegrity(TestData.Corrupted.badCrc.contents)
```

While `FitFile.read` reads the entire file into memory, there is also
a `StreamDecoder` which can be used to incrementally read fit files.

## profile

The profile module contains data structures that describe the "global
profile". This data is necessary to make sense of a decoded fit file.
For example, it provides constants for message types and its expected
fields with their field names and other data that allows to interpret
the values in correctly.

A fit message may contain all or a subset of the described fields in
the corresponding profile.

For example, the `FileIdMsg` has these fields in its profile:

```scala mdoc
import fit4s.profile.*

FileIdMsg.allFields
```
Here you can see that field number `1` denotes the manufacturer. The type `enum` means that the number in the fit file corresponds to some value in the profile type "manufacturer". The `ManufacturerType` can be used to lookup the value:

```scala mdoc
ManufacturerType.values(1)

```
## core

The core module combines the previous modules to create a more
convenient api for reading fit files. It wraps a `FitFile` into a
`Fit` class and a data record into a `FitMessage` class. Then you can
get information about a field by providing the appropriate profile
information.

```scala mdoc
import fit4s.core.*

val fenix539 = Fit.read(TestData.Activities.fenix539.contents).require.head
val fileIdMsg = fenix539.getMessages(FileIdMsg).toVector.head
val manu = fileIdMsg.field(FileIdMsg.manufacturer)
val value = manu.asEnum
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

```scala mdoc
fileIdMsg.field(FileIdMsg.serialNumber).as[Long]
```

In the same way if there is a `MessageReader` instance in scope, the
complete message can be converted into some value. There is a `FileId`
case class defined that provides such a reader.

```scala mdoc
fileIdMsg.as[FileId]
```

## cli

Finally a small cli application exists for demoing. It can read fit
files and convert it into JSON or create a HTML file to view the data.

Try with `--help` to find out more.

You can use [coursier](https://get-coursier.io/docs/cli-launch) to run
the cli:

```
cs launch com.github.eikek::fit4s-cli:0.11.0 -- --help
```

The [release page](https://github.com/eikek/fit4s/releases/latest)
provides a zip file containing the cli tool.

Using [nix](https://nixos.org) you can run it from this repo:

```
nix run github:eikek/fit4s -- --help
```
