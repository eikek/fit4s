package fit4s.bench

import java.util.concurrent.TimeUnit

import fit4s.core.LargeSample
import fit4s.core.data.LatLng
import fit4s.core.data.Polyline

import org.openjdk.jmh.annotations.*

@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
class PolylineBench:

  var coordinates600: Vector[LatLng] = Vector.empty
  var encoded: String = ""

  @Setup
  def setup(): Unit =
    val c300 = LargeSample.coordinates ++ LargeSample.coordinates.reverse
    coordinates600 = c300 ++ c300
    encoded = Polyline.encode(coordinates600)

  @Benchmark
  def encodePolyline =
    Polyline.encode(coordinates600)

  @Benchmark
  def decodePolyline =
    Polyline.decode(encoded)

  @Benchmark
  def decodeLatLngs =
    Polyline.toLatLng(encoded)
