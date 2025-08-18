package fit4s.codec

import munit.FunSuite

class FitFileStructureTest extends FunSuite:

  test("bad crc file"):
    val data = TestData.Corrupted.badCrc.contents
    val fit = FitFileStructure.decode(data).require
    assert(FitFileStructure.isFitFile(data))
    assert(fit.checkCrc.isDefined)
    assert(FitFileStructure.checkIntegrity(data).isDefined)

    val fixed = fit.updateCrc
    assert(fixed.checkCrc.isEmpty)

  test("bad file, but good structure and valid crc"):
    val data = TestData.Corrupted.noDefMsg.contents
    val fit = FitFileStructure.decode(data).require
    assert(fit.checkCrc.isEmpty)
