package fit4s.core

import java.time.Instant

import fit4s.codec.TestData
import fit4s.profile.*

import munit.FunSuite

class FileIdTest extends FunSuite with TestSyntax:

  test("create custom id"):
    val file = TestData.Activities.edge146
    val fit = Fit.read(file.contents).require.head
    val fid = fit.getMessages(FileIdMsg).head.as[FileId].value

    val fileId = FileId(
      fileType = ProfileEnum.unsafe(4, FileType),
      manufacturer = ProfileEnum.unsafe(1, ManufacturerType),
      product = Some(ProfileEnum.unsafe(3121, GarminProductType)),
      serialNumber = Some(3419639228L),
      createdAt = Some(Instant.parse("2022-11-26T12:36:02Z")),
      number = None,
      productName = None
    )
    val expected = "2UNiVbZvNyPMxuAqigB"
    assertEquals(fid, fileId)
    assertEquals(fid.asStringLegacy, expected)
    assertEquals(FileId.fromStringLegacy(expected).fold(sys.error, identity), fileId)

  test("string codec"):
    val file = TestData.Activities.edge146
    val fit = Fit.read(file.contents).require.head
    val fid = fit.getMessages(FileIdMsg).head.as[FileId].value

    val fileId = FileId(
      fileType = ProfileEnum.unsafe(4, FileType),
      manufacturer = ProfileEnum.unsafe(1, ManufacturerType),
      product = Some(ProfileEnum.unsafe(3121, GarminProductType)),
      serialNumber = Some(3419639228L),
      createdAt = Some(Instant.parse("2022-11-26T12:36:02Z")),
      number = None,
      productName = None
    )
    assertEquals(fid, fileId)
    val expected = "2UNj4h4B4ftWHRLj6xw"
    assertEquals(fid.asString, expected)
    assertEquals(FileId.fromString(expected).fold(sys.error, identity), fileId)
