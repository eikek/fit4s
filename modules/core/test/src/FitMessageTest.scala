package fit4s.core

import java.util.UUID

import fit4s.codec.TestData
import fit4s.core.data.*
import fit4s.profile.DeveloperDataIdMsg
import fit4s.profile.FileIdMsg
import fit4s.profile.GarminProductType
import fit4s.profile.RecordMsg

import munit.FunSuite

class FitMessageTest extends FunSuite with TestSyntax:
  test("dev application id"):
    val data = TestData.Activities.edge1536
    val fit = Fit.read(data.contents).require.head
    val devIdMsg = fit.getMessages(DeveloperDataIdMsg).head
    val appId = devIdMsg.field(DeveloperDataIdMsg.applicationId).as[UUID]
    assertEquals(
      appId,
      Some(Right(UUID.fromString("c5d949c3-9acb-4e00-bb2d-c3b871e9e733")))
    )

  test("component expansion creating new fields"):
    // this file has speed & altitude fields that have components enhanced_altitude and enhanced_speed
    val cfg = Config(expandComponents = true)
    val file = TestData.Activities.edge146
    val fit = Fit.read(file.contents, cfg).require.head
    val record = fit.getMessages(RecordMsg).head
    val speed = record.field(RecordMsg.speed).as[Speed].value
    val enhancedSpeed = record.field(RecordMsg.enhancedSpeed).as[Speed].value
    assertEquals(speed, Speed.meterPerSecond(1.941))
    assertEquals(enhancedSpeed, Speed.meterPerSecond(1.941))

    val alt = record.field(RecordMsg.altitude).as[Distance].value
    val enhancedAlt = record.field(RecordMsg.enhancedAltitude).as[Distance].value
    assertEqualsDouble(alt.toMeter, 473.8, 0.1)
    assertEqualsDouble(enhancedAlt.toMeter, 473.8, 0.1)

  test("no component expansion"):
    val cfg = Config(expandComponents = false)
    val file = TestData.Activities.edge146
    val fit = Fit.read(file.contents, cfg).require.head
    val record = fit.getMessages(RecordMsg).head
    val speed = record.field(RecordMsg.speed).as[Speed].value
    val enhancedSpeed = record.field(RecordMsg.enhancedSpeed).as[Speed]
    assertEquals(speed, Speed.meterPerSecond(1.941))
    assert(enhancedSpeed.isEmpty)

    val alt = record.field(RecordMsg.altitude).as[Distance].value
    val enhancedAlt = record.field(RecordMsg.enhancedAltitude).as[Distance]
    assertEqualsDouble(alt.toMeter, 473.8, 0.1)
    assert(enhancedAlt.isEmpty)

  test("replace subfields"):
    val cfg = Config(expandSubFields = true)
    val file = TestData.Activities.edge146
    val fit = Fit.read(file.contents, cfg).require.head
    val fid = fit.getMessages(FileIdMsg).head
    val product = fid.field(FileIdMsg.product).asEnum.get
    assertEquals(product.ordinal, GarminProductType.edge530)

  test("not replacing subfields"):
    val cfg = Config(expandSubFields = false)
    val file = TestData.Activities.edge146
    val fit = Fit.read(file.contents, cfg).require.head
    val fid = fit.getMessages(FileIdMsg).head
    // the product field has subfield that indicate in which enum to look for the value
    val product = fid.field(FileIdMsg.product).asEnum
    assert(product.isEmpty)
    val numValue = fid.field(FileIdMsg.product).as[Int].value
    assertEquals(numValue, GarminProductType.edge530)

  test("timestamp from compressed header"):
    val file = TestData.Activities.fr70Intervals
    val fit = Fit.read(file.contents).require.head
    val rec = fit.getMessages(RecordMsg).head
    assertEquals(rec.timestamp, Some(DateTime(738571955L)))
