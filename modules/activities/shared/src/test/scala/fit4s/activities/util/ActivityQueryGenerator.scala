package fit4s.activities.util

import java.time.{Duration, Instant}

import cats.data.NonEmptyList
import fs2.io.file.Path

import fit4s.activities.data.QueryCondition._
import fit4s.activities.data._
import fit4s.data.{DeviceProduct, Distance, FileId}
import fit4s.profile.types._

import org.scalacheck.Gen

object ActivityQueryGenerator:

  def generator: Gen[ActivityQuery] =
    for {
      c <- Gen.option(conditionGenerator)
      p <- pageGen
    } yield ActivityQuery(c, p)

  def pageGen: Gen[Page] =
    for {
      offset <- Gen.choose(0, 3000)
      limit <- Gen.choose(1, 500)
    } yield Page(limit, offset)

  def conditionGenerator: Gen[QueryCondition] =
    Gen.frequency(
      8 -> basicConditionGen,
      3 -> nelGen(innerConditionGen).map(QueryCondition.And.apply),
      3 -> nelGen(innerConditionGen).map(QueryCondition.Or.apply),
      3 -> innerConditionGen.map(QueryCondition.Not.apply)
    )

  private def innerConditionGen: Gen[QueryCondition] =
    Gen.frequency(9 -> basicConditionGen, 2 -> Gen.lzy(conditionGenerator))

  def basicConditionGen: Gen[QueryCondition] =
    Gen.oneOf(
      nelGen(tagNameGen).map(TagAllStarts.apply),
      nelGen(tagNameGen).map(TagAnyStarts.apply),
      nelGen(tagNameGen).map(TagAllMatch.apply),
      nelGen(tagNameGen).map(TagAnyMatch.apply),
      nelGen(pathGen).map(LocationAllMatch.apply),
      nelGen(pathGen).map(LocationAnyMatch.apply),
      nelGen(pathGen).map(LocationAllStarts.apply),
      nelGen(pathGen).map(LocationAnyStarts.apply),
      nelGen(activityIdGen).map(ActivityIdMatch.apply),
      fileIdGen.map(_.asString).map(FileIdMatch.apply),
      deviceMatchGen,
      Gen.oneOf(true, false).map(StravaLink.apply),
      Gen.oneOf(Sport.all).map(SportMatch.apply),
      Gen.oneOf(SubSport.all).map(SubSportMatch.apply),
      instantGen.map(StartedAfter.apply),
      instantGen.map(StartedBefore.apply),
      distanceGen.map(DistanceGE.apply),
      distanceGen.map(DistanceLE.apply),
      durationGen.map(ElapsedGE.apply),
      durationGen.map(ElapsedLE.apply),
      durationGen.map(MovedLE.apply),
      durationGen.map(MovedGE.apply),
      textGen.map(NotesContains.apply),
      textGen.map(NameContains.apply)
    )

  def activityIdGen: Gen[ActivityId] =
    Gen.choose[Long](0, 99999).map(ActivityId.apply)

  def deviceMatchGen: Gen[DeviceMatch] =
    deviceProductGen.map(DeviceMatch.apply)

  def tagNameGen: Gen[TagName] =
    Gen
      .oneOf("bike/mine", "some-tag", "other-tag", "tag with whitespace")
      .map(TagName.unsafeFromString)

  def pathGen: Gen[Path] =
    Gen.oneOf("/my/location", "/other/location", "/yet/another/path").map(Path.apply)

  def nelGen[A](g: Gen[A]): Gen[NonEmptyList[A]] =
    Gen
      .choose(1, 3)
      .flatMap(size =>
        Gen.resize(size, Gen.nonEmptyListOf(g).map(NonEmptyList.fromListUnsafe))
      )

  def fileIdGen: Gen[FileId] = for {
    ft <- Gen.oneOf(File.all)
    man <- Gen.oneOf(Manufacturer.all)
    prod <- deviceProductGen
    ser <- Gen.option(serialNumberGen)
    created <- Gen.option(dateTimeGen)
    num <- Gen.option(serialNumberGen)
    name <- Gen.option(Gen.oneOf("device name 1", "devicename2"))
  } yield FileId(ft, man, prod, ser, created, num, name)

  def deviceProductGen: Gen[DeviceProduct] = Gen.frequency(
    8 -> Gen.oneOf(GarminProduct.all).map(DeviceProduct.Garmin.apply),
    7 -> Gen.oneOf(FaveroProduct.all).map(DeviceProduct.Favero.apply),
    1 -> Gen.const(DeviceProduct.Unknown)
  )

  def instantGen: Gen[Instant] =
    Gen.choose(646747200000L, 1909051200000L).map(Instant.ofEpochMilli)

  def dateTimeGen: Gen[DateTime] =
    val now = Instant.now()
    val offset = DateTime.offset
    val max = Duration.between(offset, now).toSeconds
    Gen
      .choose(DateTime.minTimeForOffset, max)
      .map(DateTime.apply)

  def distanceGen: Gen[Distance] =
    Gen.choose(0d, 1500 * 1000d).map(Distance.meter)

  def durationGen: Gen[Duration] =
    Gen.choose(Duration.ofSeconds(1), Duration.ofHours(9))

  def textGen: Gen[String] =
    Gen.oneOf("some notes ", "doesn't matter what really?!")

  def serialNumberGen: Gen[Long] =
    Gen.choose(0, Int.MaxValue).map(_.toLong)
