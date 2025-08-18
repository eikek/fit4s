package fit4s.codec.internal

import fit4s.codec.FitFileStructure
import fit4s.codec.TestData

import munit.FunSuite
import scodec.bits.ByteVector
import scodec.bits.hex

class CrcTest extends FunSuite:

  test("crc 1"):
    assertEquals(Crc(ByteVector.empty), 0)
    assertEquals(Crc(hex"bc8dd3cb515c753effffffff0100310cffff04"), 9137)

  test("crc 2"):
    val file = TestData.Activities.swim25mLane.contents
    val structure = FitFileStructure.decode(file).require
    assert(structure.checkCrc.isEmpty, structure.checkCrc.toString())
