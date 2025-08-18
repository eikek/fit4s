package fit4s.codec

import com.garmin.fit.MesgNum
import munit.FunSuite

class DevFieldTest extends FunSuite:

  test("dev fields"):
    val fit = FitFile.read(TestData.Activities.edge1536.contents).require.head
    val msg = fit.findMessages(MesgNum.RECORD).filter(_.fields.exists(!_.isTyped))
    println(msg)
