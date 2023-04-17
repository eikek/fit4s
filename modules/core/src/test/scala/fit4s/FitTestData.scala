package fit4s

import cats.effect.IO

import java.net.URL
import fs2._
import scodec.bits.ByteVector

object FitTestData {

  val exampleActivity = fromURL(getClass.getResource("/fit/Activity.fit"))

  val examplePoolswimActivity = fromURL(
    getClass.getResource("/fit/activity_poolswim_with_hr.fit")
  )

  val edge530CyclingActivity = fromURL(
    getClass.getResource("/fit/2023-03-16-06-25-37.fit")
  )

  val indoorCyclingActivity = fromURL(
    getClass.getResource("/fit/2023-01-12-15-30-29.fit")
  )

  val fenix5Activity = fromURL(
    getClass.getResource("/fit/fenix5_1.fit")
  )

  val garminSwimActivity = fromURL(
    getClass.getResource("/fit/swim_12980500464.fit")
  )

  private def fromURL(url: URL): IO[ByteVector] =
    fs2.io
      .readInputStream(IO(url.openStream()), 8192)
      .chunks
      .compile
      .fold(Chunk.empty[Byte])(_ ++ _)
      .map(_.toByteVector)
}
