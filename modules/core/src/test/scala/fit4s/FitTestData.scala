package fit4s

import java.net.URL

import cats.effect.IO
import fs2._

import scodec.bits.ByteVector

object FitTestData {

  val exampleActivity = fromURL(getClass.getResource("/fit/Activity.fit"))

  val examplePoolswimActivity = fromURL(
    getClass.getResource("/fit/activity_poolswim_with_hr.fit")
  )

  // cb413f3475acbaa15e93725a1b2ae12db0e36cfbc9962913a3e4c87ec16ca8f5
  // val edge530CyclingActivity = fromURL(
  //   getClass.getResource("/fit/2023-03-16-06-25-37.fit")
  // )

  // 42f3baddd1d4fdd47ee1d3bdfd52cfb3b82cf8e31146569e850c87e0cd87205b
  // val indoorCyclingActivity = fromURL(
  //   getClass.getResource("/fit/2023-01-12-15-30-29.fit")
  // )

  // bc0cad145526787c38e7d89f3baf4c388874cb4aa4faa6f595d3171315635d86
  // val fenix5Activity = fromURL(
  //   getClass.getResource("/fit/fenix5_1.fit")
  // )

  // ee57c232b1ec7eba92ceb3c401c72599a6552235a01f337ddde14eeeed814909
  // val garminSwimActivity = fromURL(
  //   getClass.getResource("/fit/swim_12980500464.fit")
  // )

  private def fromURL(url: URL): IO[ByteVector] =
    fs2.io
      .readInputStream(IO(url.openStream()), 8192)
      .chunks
      .compile
      .fold(Chunk.empty[Byte])(_ ++ _)
      .map(_.toByteVector)
}
