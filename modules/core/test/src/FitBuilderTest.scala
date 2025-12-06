package fit4s.core

import java.time.Instant
import java.time.temporal.ChronoUnit

import cats.syntax.all.*

import fit4s.core.FieldValue.*
import fit4s.core.data.*
import fit4s.profile.*

import munit.FunSuite

class FitBuilderTest extends FunSuite:
  val position = Position(Semicircle.semicircle(5), Semicircle.semicircle(6))

  val startPosEncoder: MessageEncoder[Position] =
    Position.messageEncode(SegmentLapMsg)(_.startPositionLat, _.startPositionLong)
  val endPosEncoder: MessageEncoder[Position] =
    Position.messageEncode(SegmentLapMsg)(_.endPositionLat, _.endPositionLong)

  val posEncoder: MessageEncoder[(Position, Int)] =
    import MessageEncoder.syntax.*
    MessageEncoder.forMsg(SegmentPointMsg).fields { case (msg, (pos, idx)) =>
      (msg.messageIndex -> idx) ::
        Position.messageEncode(msg)(_.positionLat, _.positionLong).encode(pos).fields
    }

  test("create segment"):
    val positions = List.iterate(position, 8)(p => p + p)
    val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
    val file =
      FitBuilder
        .newBuilder(
          _.segment,
          createdAt = now,
          manufacturer = ProfileEnum(ManufacturerType, ManufacturerType.garmin)
        )
        .record(SegmentIdMsg)(
          _.field(_.name, "My Segment 1")
            .field(_.sport, SportType.cycling)
            .field(_.enabled, 1)
        )
        .record(SegmentLapMsg) {
          _.field(_.timestamp, Instant.now)
            .set(position)(using startPosEncoder)
            .set(position)(using endPosEncoder)
            .field(_.totalTimerTime, Duration.minutes(15.4))
            .field(_.totalCalories, Calories.kcal(254))
            .field(_.totalDistance, Distance.km(1.104))
            .field(_.eventType, Option.empty[Byte])
        }
        .records(positions.zipWithIndex)(using posEncoder)
        .build

    val data = file.encoded().require
    val path = java.nio.file.Path.of("test.fit").toAbsolutePath()
    java.nio.file.Files.write(path, data.toArray)
    println(path)

    val decoded = Fit.read(data).require.head
    assertEquals(
      decoded.fileId.get,
      FileId(
        ProfileEnum(FileType, FileType.segment),
        ProfileEnum(ManufacturerType, ManufacturerType.garmin).some,
        None,
        None,
        now.some,
        None,
        None
      )
    )
    val segId = decoded.getMessages(SegmentIdMsg).head
    val segName = segId.field(SegmentIdMsg.name).as[String]
    assertEquals(segName, Some(Right("My Segment 1")))
    val segSport = segId.field(SegmentIdMsg.sport).asEnum
    assertEquals(segSport, Some(ProfileEnum(SportType, SportType.cycling)))
    val points = decoded.getMessages(SegmentPointMsg).toList
    assertEquals(points.size, 8)
    val plat0 = points.head.field(SegmentPointMsg.positionLat).as[Semicircle]
    val plng0 = points.head.field(SegmentPointMsg.positionLong).as[Semicircle]
    assertEquals(plat0, Some(Right(Semicircle.semicircle(5))))
    assertEquals(plng0, Some(Right(Semicircle.semicircle(6))))
