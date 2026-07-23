package fit4s.codec

import fit4s.codec.internal.RadarExtension

import com.garmin.fit.MesgNum
import munit.FunSuite

class DevFieldTest extends FunSuite:

  test("dev fields"):
    val fit = FitFile.read(TestData.Activities.edge1536.contents).require.head
    val ext = RadarExtension(fit)
    val lapValues = fit
      .findMessages(MesgNum.LAP)
      .flatMap(r => ext.getValue(r, RadarExtension.Field.RadarLap))
    assertEquals(lapValues, Vector(4, 1, 0, 3, 6, 3, 1, 1, 3, 0, 0, 0, 2, 1))

    val recordValues = fit
      .findMessages(MesgNum.RECORD)
      .flatMap(r => ext.getValue(r, RadarExtension.Field.RadarCurrent))
    assertEquals(recordValues.last, 25)

    val sessionValues = fit
      .findMessages(MesgNum.SESSION)
      .flatMap(r => ext.getValue(r, RadarExtension.Field.RadarTotal))
    assertEquals(sessionValues, Vector(25))
