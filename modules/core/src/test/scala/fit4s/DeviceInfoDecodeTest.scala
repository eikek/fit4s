package fit4s

import cats.syntax.all._

import fit4s.FitMessage.DataMessage
import fit4s.data.DeviceProduct
import fit4s.json.JsonCodec
import fit4s.profile.messages.DeviceInfoMsg
import fit4s.profile.types.{DeviceIndex, GarminProduct}

import io.bullet.borer.*
import munit.FunSuite
import scodec.bits.ByteVector

class DeviceInfoDecodeTest extends FunSuite with JsonCodec:
  test("decode DeviceInfo data"):
    val data = ByteVector.fromValidHex(
      "249c0934220a24ebffffffffffffffffffffffffffffffff000000000000000000000000000000000000000000000000000000000000000000000000ffffffff0100890a1202ffffffff000000ffffffffff00ffff05ffffffffffffff"
    )
    val definition = Json
      .decode(
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":23,"fieldCount":27,"fields":[{"fieldDefNum":253,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}},{"fieldDefNum":7,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":8,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":15,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":16,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":17,"sizeBytes":32,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":7},"fitBaseType":7}},{"fieldDefNum":24,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}},{"fieldDefNum":31,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":4,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":5,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":10,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":13,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":21,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":11},"fitBaseType":139}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":1,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":6,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":9,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":11,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":18,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":20,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":10},"fitBaseType":10}},{"fieldDefNum":22,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":23,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":25,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":29,"sizeBytes":6,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":30,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}}],"devFieldCount":0,"devFields":[],"profileMsg":23}""".getBytes
      )
      .to[FitMessage.DefinitionMessage]
      .value

    val dataMessage = DataMessage(definition, data)

    assertEquals(
      dataMessage.getField(DeviceInfoMsg.deviceIndex).map(_.map(_.value)),
      DeviceIndex.Creator.some.asRight
    )
    assertEquals(
      DeviceProduct.from(dataMessage),
      DeviceProduct.Garmin(GarminProduct.Fenix5).asRight
    )
    assertEquals(
      dataMessage.getField(DeviceInfoMsg.softwareVersion).map(_.map(_.value.rawValue)),
      530L.some.asRight
    )
