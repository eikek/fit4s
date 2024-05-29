package fit4s.tcx

import java.time.Instant

import fit4s.data.*
import fit4s.profile.types.Sport

import munit.*

class TcxReaderTest extends FunSuite:

  test("read sample tcx"):
    val content = scala.xml.XML.load(getClass.getResource("/sample.tcx"))
    val parsed = TcxReader.activities(content)
    assertEquals(parsed.size, 1)
    val a = parsed.head
    assertEquals(a.laps.size, 3)
    assertEquals(a.id, Instant.parse("2012-12-26T21:29:53Z"))
    assertEquals(a.sport, Sport.Running)
    assertEquals(a.laps.map(_.track.size).sum, 383)

    val sampleTrackpoint = Instant.parse("2012-12-26T21:29:59Z")
    val tp = a.laps.head.track.find(_.time == sampleTrackpoint).get

    assertEquals(tp.heartRate, Some(HeartRate.bpm(62)))
    assertEquals(tp.altitude, Some(Distance.meter(178.942626953)))
    assertEquals(tp.distance, Some(Distance.meter(1.10694694519)))
    assertEquals(tp.cadence, None)
    assertEquals(tp.position, Some(Position.degree(35.9518683795, -79.0931715444)))
