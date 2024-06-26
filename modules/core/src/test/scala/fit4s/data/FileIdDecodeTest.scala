package fit4s.data

import scala.util.Random

import fit4s.FitMessage
import fit4s.FitMessage.DataMessage
import fit4s.json.JsonCodec
import fit4s.profile.messages.FileIdMsg
import fit4s.profile.types.*

import io.bullet.borer.*
import munit.FunSuite
import scodec.bits.ByteVector

class FileIdDecodeTest extends FunSuite with JsonCodec:

  test("decode FileId data (2)"):
    val data = ByteVector.fromValidHex("220a24eb63681234ffffffff0100890affff04")
    val definition = Json
      .decode(
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":0,"fieldCount":7,"fields":[{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}},{"fieldDefNum":4,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":7,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":1,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":5,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}],"devFieldCount":0,"devFields":[],"profileMsg":0}""".getBytes
      )
      .to[FitMessage.DefinitionMessage]
      .value

    assertEquals(definition.profileMsg, Some(FileIdMsg))
    val dataMessage = DataMessage(definition, data)

    val fileId = FileId.from(dataMessage)
    assertEquals(
      fileId,
      Right(
        FileId(
          fileType = File.Activity,
          manufacturer = Manufacturer.Garmin,
          product = DeviceProduct.Garmin(GarminProduct.Fenix5),
          serialNumber = Some(3945007650L),
          createdAt = Some(DateTime(873621603L)),
          number = None,
          productName = None
        )
      )
    )

  test("decode FileId data"):
    val data = ByteVector.fromValidHex("000103dc0001e24002")
    val definition = Json
      .decode(
        """{"reserved":0,"archType":"BIG_ENDIAN","globalMessageNumber":0,"fieldCount":4,"fields":[{"fieldDefNum":1,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}],"devFieldCount":0,"devFields":[],"profileMsg":0}""".getBytes
      )
      .to[FitMessage.DefinitionMessage]
      .value

    assertEquals(definition.profileMsg, Some(FileIdMsg))
    val dataMessage = DataMessage(definition, data)

    val fileId = FileId.from(dataMessage)
    assertEquals(
      fileId,
      Right(
        FileId(
          fileType = File.Settings,
          manufacturer = Manufacturer.Garmin,
          product = DeviceProduct.Garmin(GarminProduct.Fr60),
          serialNumber = Some(123456L),
          createdAt = None,
          number = None,
          productName = None
        )
      )
    )

  test("fileId as string"):
    val samples =
      for {
        file <- File.all
        manufacturer <- Manufacturer.all
        device <- DeviceProduct.all
      } yield FileId(
        file,
        manufacturer,
        device,
        Option.when(Random.nextBoolean())(9845466L),
        Option.when(Random.nextBoolean())(DateTime(873621603L)),
        Option.when(Random.nextBoolean())(5542L),
        Option.when(Random.nextBoolean())("xyzabc")
      )

    Random.shuffle(samples).take(50).foreach { fileId =>
      val id = fileId.asString
      val decoded = FileId.fromString(id)
      assertEquals(decoded, Right(fileId))
    }
