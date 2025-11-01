package fit4s.bench

import java.util.concurrent.TimeUnit

import scala.collection.mutable.ListBuffer

import fit4s.core.LargeSample
import fit4s.core.data.LatLng
import fit4s.core.data.Polyline

import org.openjdk.jmh.annotations.*

@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
class PolylineDecodeN:

  val cfg = Polyline.Config()
  var polyline: Polyline = Polyline.empty(cfg)

  @Param(Array("1", "10", "100", "1000"))
  var chunkSize: Int = -1

  @Setup
  def setup(): Unit =
    val c300 = LargeSample.coordinates ++ LargeSample.coordinates.reverse
    val coordinates600 = c300 ++ c300
    polyline = Polyline(cfg)(coordinates600*)

  @Benchmark
  def decodeLatLngInChunks: Vector[LatLng] =
    val buf = new ListBuffer[LatLng]()
    var remain = polyline
    while (remain.nonEmpty)
      remain.decodeN(chunkSize) match
        case Some((v, n)) =>
          buf.addAll(v)
          remain = n
        case None =>
          remain = Polyline.empty(cfg)
    buf.toVector
