package fit4s.core

import java.time.Instant

import fit4s.codec.TestData
import fit4s.profile.*

import munit.FunSuite

class FileIdTest extends FunSuite with TestSyntax:

  test("string codec 1"):
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

  test("segment fileId"):
    val file = TestData.Segment.segment2
    val fit = Fit.read(file.contents).require.head
    val fid = fit.fileId.get
    val expect = FileId(
      fileType = ProfileEnum(FileType, FileType.segment),
      manufacturer = ProfileEnum(ManufacturerType, 8888),
      product = None,
      serialNumber = Some(1743444),
      createdAt = Some(Instant.parse("2025-12-05T14:22:54Z")),
      number = None,
      productName = Some("mkfit.sc")
    )
    val expectStr = "38pwqnCW1oHReh2KDu7ZAH1jfoHz7"
    assertEquals(fid, expect)
    assertEquals(fid.asString, expectStr)
    assertEquals(FileId.fromString(expectStr), Right(fid))
