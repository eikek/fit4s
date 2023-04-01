package fit4s.activities.impl

import cats.data.NonEmptyList
import fit4s.activities.ActivityQuery
import fit4s.activities.ActivityQuery.Condition._
import fit4s.activities.ActivityQuery.{Condition, OrderBy}
import fit4s.activities.data.TagName
import fit4s.data.{DeviceProduct, Distance, FileId}
import fit4s.profile.types.{
  DateTime,
  FaveroProduct,
  File,
  GarminProduct,
  Manufacturer,
  Sport,
  SubSport
}
import fs2.io.file.Path
import org.scalacheck.Gen

import java.time.{Duration, Instant}

object ActivityQueryGenerator {

  def generator: Gen[ActivityQuery] =
    for {
      c <- Gen.option(conditionGenerator)
      o <- orderByGenerator
    } yield ActivityQuery(c, o)

  def orderByGenerator: Gen[OrderBy] =
    Gen.oneOf(OrderBy.Distance, OrderBy.StartTime)

  def conditionGenerator: Gen[Condition] =
    Gen.frequency(
      8 -> basicConditionGen,
      3 -> nelGen(innerConditionGen).map(Condition.And),
      3 -> nelGen(innerConditionGen).map(Condition.Or),
      3 -> innerConditionGen.map(Condition.Not)
    )

  private def innerConditionGen: Gen[Condition] =
    Gen.frequency(9 -> basicConditionGen, 2 -> Gen.lzy(conditionGenerator))

  def basicConditionGen: Gen[Condition] =
    Gen.oneOf(
      tagNameGen.map(TagStarts),
      nelGen(tagNameGen).map(TagMatch),
      nelGen(pathGen).map(LocationMatch),
      pathGen.map(LocationStarts),
      fileIdGen.map(_.asString).map(FileIdMatch),
      Gen.oneOf(Sport.all).map(SportMatch),
      Gen.oneOf(SubSport.all).map(SubSportMatch),
      instantGen.map(StartedAfter),
      instantGen.map(StartedBefore),
      distanceGen.map(DistanceGE),
      distanceGen.map(DistanceLE),
      durationGen.map(ElapsedGE),
      durationGen.map(ElapsedLE),
      durationGen.map(MovedLE),
      durationGen.map(MovedGE),
      textGen.map(NotesMatch)
    )

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
    8 -> Gen.oneOf(GarminProduct.all).map(DeviceProduct.Garmin),
    7 -> Gen.oneOf(FaveroProduct.all).map(DeviceProduct.Favero),
    1 -> Gen.const(DeviceProduct.Unknown)
  )

  def instantGen: Gen[Instant] =
    Gen.choose(646747200000L, 1909051200000L).map(Instant.ofEpochMilli)

  def dateTimeGen: Gen[DateTime] = {
    val now = Instant.now()
    val offset = DateTime.offset
    val max = Duration.between(offset, now).toSeconds
    Gen
      .choose(DateTime.minTimeForOffset, max)
      .map(DateTime.apply)
  }

  def distanceGen: Gen[Distance] =
    Gen.choose(0d, 1500 * 1000d).map(Distance.meter)

  def durationGen: Gen[Duration] =
    Gen.choose(Duration.ofSeconds(1), Duration.ofHours(9))

  def textGen: Gen[String] =
    Gen.oneOf("some notes ", "doesn't matter what really?!")

  def serialNumberGen: Gen[Long] =
    Gen.choose(0, Int.MaxValue).map(_.toLong)
}
