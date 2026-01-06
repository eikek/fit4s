package fit4s.codec

import fit4s.codec.StreamDecoder.FitPart
import fit4s.codec.internal.FitFileCodec

import com.garmin.fit.MesgNum
import munit.FunSuite
import scodec.Err

class FitFileTest extends FunSuite:

  test("dev fields"):
    val data = TestData.Activities.edge1536
    val fit = FitFile.read(data.contents).require.head
    val rr = fit
      .findMessages(TestProfile.Msg.record)
      .drop(29)
      .head

    val unknownDevFields = rr.devFields.filter(!_.isTyped)
    assertEquals(unknownDevFields, Vector.empty)
    assertEquals(rr.typedDevFields.size, 5)

    val f1 = rr.findDevField(DevFieldId(0, 0)).get
    assertEquals(f1.fieldDescription.fieldName, Some("radar_ranges"))
    assertEquals(f1.data, Vector(6, 0, 0, 0, 0, 0, 0, 0))

    val f2 = rr.findDevField(DevFieldId(0, 1)).get
    assertEquals(f2.fieldDescription.fieldName, Some("radar_speeds"))
    assertEquals(f2.data, Vector(3, 0, 0, 0, 0, 0, 0, 0))

    val f3 = rr.findDevField(DevFieldId(0, 2)).get
    assertEquals(f3.fieldDescription.fieldName, Some("radar_current"))
    assertEquals(f3.data, Vector(0))

    val f4 = rr.findDevField(DevFieldId(0, 5)).get
    assertEquals(f4.fieldDescription.fieldName, Some("passing_speed"))
    assertEquals(f4.data, Vector(11))

    val f5 = rr.findDevField(DevFieldId(0, 6)).get
    assertEquals(f5.fieldDescription.fieldName, Some("passing_speedabs"))
    assertEquals(f5.data, Vector(32))

    val sess = fit.findMessages(MesgNum.SESSION).head
    assertEquals(sess.typedDevFields.size, 1)
    val f6 = sess.findDevField(DevFieldId(0, 3)).get
    assertEquals(f6.fieldDescription.fieldName, Some("radar_total"))
    assertEquals(f6.data, Vector(25))

  test("basic decode 1"):
    val data = TestData.Activities.edge146
    val fit = FitFile.read(data.contents).require.head

    val fileId = fit.singleMsg(TestProfile.Msg.fileId)
    val typeField = fileId.fieldData(TestProfile.FileId.fileType).get
    val manufacturer = fileId.fieldData(TestProfile.FileId.manufacturer).get
    val serial = fileId.fieldData(TestProfile.FileId.serialnum).get
    val timeCreated = fileId.fieldData(TestProfile.FileId.timeCreated).get
    assertEquals(typeField, Vector(4))
    assertEquals(manufacturer, Vector(1))
    assertEquals(serial, Vector(3419639228L))
    assertEquals(timeCreated, Vector(1038400562L))

    val activity = fit.singleMsg(TestProfile.Msg.activity)
    assertEquals(
      activity.fieldData(TestProfile.Activity.numSessions).get,
      Vector(1)
    )
    assertEquals(
      activity.fieldData(TestProfile.Activity.timestamp).get,
      Vector(1038404362L)
    )
    assertEquals(
      activity.fieldData(TestProfile.Activity.totalTimer).get,
      Vector(3431099L)
    )
    assertEquals(
      activity.fieldData(TestProfile.Activity.localTime).get,
      Vector(1038407962L)
    )

    val sport = fit.singleMsg(TestProfile.Msg.sport)
    assertEquals(
      sport.fieldData(10).get,
      Vector(160, 0, 160)
    )
    assertEquals(
      sport.fieldData(TestProfile.Sport.name),
      Some(Vector("Commute"))
    )

  test("fail with bad file"):
    val data = TestData.Corrupted.noDefMsg.contents
    val fits = FitFile.read(data)
    assert(fits.isFailure)

  test("fail with invalid crc"):
    val data = TestData.Corrupted.badCrc.contents
    val fits = FitFile.read(data)
    assert(fits.isFailure)

  test("don't fail with invalid crc, when requested"):
    val data = TestData.Corrupted.badCrc.contents
    val fits = FitFile.read(data, checkCrc = false)
    assert(fits.isSuccessful)

  test(s"reading ${TestData.allCount} test fit files without error"):
    val dec = FitFileCodec.fitFilesDecoder(checkCrc = true).complete
    var counter = 0
    var failed = 0

    TestData.allFiles.foreach { file =>
      counter += 1
      val res = dec.decode(file.contents.bits)
      if (res.isFailure) {
        failed += 1
        println(s"* ${file.name}: $res")
      }
    }
    assertEquals(
      failed,
      0,
      s"Not all files succeeded decoding. Faile: $failed out of $counter"
    )

  test("decode encoded file"):
    val codc = FitFileCodec.fitFilesDecoder(checkCrc = true).complete

    TestData.allFiles.foreach { file =>
      val decoded = codc.decode(file.contents.bits).require.value
      if decoded.nonEmpty then
        val encoded = decoded.head.encoded().require
        val decoded2 = codc.decode(encoded.bits)
        if (decoded2.isFailure) {
          fail(s"Decoding encoded file '${file.name}' failed: $decoded2")
        } else {
          val fm2 = decoded2.require.value.head
          val fm1 = decoded.head
          assertEquals(
            fm2.records.size,
            fm1.records.size,
            s"decoded not equal for: ${file.name}"
          )
        }
    }

  test("diagnose file".ignore):
    val file = TestData.Activities.fr70Intervals
    DiagnoseDecode.decode(file.contents).require

  test("stream-decode one by one"):
    // 26 = Workout, 27 = WorkoutStep, 12 = SportMesg, 20 = Record
    val files = TestData.Activities.all ++ TestData.Workouts.all
    val consumer = new FitFileTest.CountConsumer()
    files.foreach { file =>
      consumer.reset()
      StreamDecoder.decode(file.source, consumer)
      assert(consumer.success, s"Failed decoding ${file.name}: ${consumer.error.get}")
    }

  test("stream-decode all at once"):
    val allFiles = ByteSource.concat(
      TestData.Activities.all.map(_.source) ++ TestData.Workouts.all.map(_.source)
    )
    val consumer = new FitFileTest.CountConsumer()
    StreamDecoder.decode(allFiles, consumer)
    assert(
      consumer.success,
      s"Decoding failed at file ${consumer.count} (${allFiles.name.getOrElse("")}): ${consumer.error.get}"
    )
    val expectedFiles = 78
    assertEquals(
      consumer.count,
      expectedFiles,
      s"Expected $expectedFiles fit files, got: ${consumer.count}"
    )

  extension (self: FitFile)
    def singleMsg(num: Int) =
      self.findMessages(num) match
        case v if v.isEmpty  => fail(s"No message for: $num")
        case v if v.size > 1 => fail(s"More than one message for: $num")
        case v               => v.head

object FitFileTest:
  final class CountConsumer extends StreamDecoder.FitPartConsumer {
    private var _count = 0
    private var _error: Option[Err] = None

    def count = _count
    def success: Boolean = _error.isEmpty
    def failed: Boolean = _error.isDefined
    def error = _error

    def onPart(part: FitPart): Boolean =
      part match
        case StreamDecoder.FitHeader(_) =>
          _count += 1
        case _ =>
          ()
      true

    def onError(err: Err): Unit =
      _error = Some(err)

    def onDone(): Unit = ()
    def reset(): Unit =
      _count = 0
      _error = None
  }
