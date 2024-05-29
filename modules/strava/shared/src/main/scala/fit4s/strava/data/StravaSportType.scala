package fit4s.strava.data

import cats.data.NonEmptyList

import fit4s.profile.types.Sport

sealed trait StravaSportType extends Product:
  final def name: String =
    productPrefix

  def fitSport: Option[Sport] = None

object StravaSportType:

  case object AlpineSki extends StravaSportType:
    override def fitSport = Some(Sport.AlpineSkiing)
  case object BackcountrySki extends StravaSportType:
    override def fitSport = Some(Sport.CrossCountrySkiing)
  case object Badminton extends StravaSportType
  case object Canoeing extends StravaSportType
  case object Crossfit extends StravaSportType
  case object EBikeRide extends StravaSportType
  case object Elliptical extends StravaSportType
  case object EMountainBikeRide extends StravaSportType
  case object Golf extends StravaSportType:
    override def fitSport = Some(Sport.Golf)
  case object GravelRide extends StravaSportType:
    override def fitSport = Some(Sport.Cycling)
  case object Handcycle extends StravaSportType
  case object HighIntensityIntervalTraining extends StravaSportType:
    override def fitSport = Some(Sport.Hiit)
  case object Hike extends StravaSportType:
    override def fitSport = Some(Sport.Hiking)
  case object IceSkate extends StravaSportType:
    override def fitSport = Some(Sport.IceSkating)
  case object InlineSkate extends StravaSportType:
    override def fitSport = Some(Sport.InlineSkating)
  case object Kayaking extends StravaSportType:
    override def fitSport = Some(Sport.Kayaking)
  case object Kitesurf extends StravaSportType:
    override def fitSport = Some(Sport.Kitesurfing)
  case object MountainBikeRide extends StravaSportType:
    override def fitSport = Some(Sport.Cycling)
  case object NordicSki extends StravaSportType:
    override def fitSport = Some(Sport.CrossCountrySkiing)
  case object Pickleball extends StravaSportType
  case object Pilates extends StravaSportType
  case object Racquetball extends StravaSportType
  case object Ride extends StravaSportType:
    override def fitSport = Some(Sport.Cycling)
  case object RockClimbing extends StravaSportType:
    override def fitSport = Some(Sport.RockClimbing)
  case object RollerSki extends StravaSportType
  case object Rowing extends StravaSportType:
    override def fitSport = Some(Sport.Rowing)
  case object Run extends StravaSportType:
    override def fitSport = Some(Sport.Running)
  case object Sail extends StravaSportType:
    override def fitSport = Some(Sport.Sailing)
  case object Skateboard extends StravaSportType
  case object Snowboard extends StravaSportType:
    override def fitSport = Some(Sport.Snowboarding)
  case object Snowshoe extends StravaSportType:
    override def fitSport = Some(Sport.Snowshoeing)
  case object Soccer extends StravaSportType:
    override def fitSport = Some(Sport.Soccer)
  case object Squash extends StravaSportType
  case object StairStepper extends StravaSportType
  case object StandUpPaddling extends StravaSportType:
    override def fitSport = Some(Sport.StandUpPaddleboarding)
  case object Surfing extends StravaSportType:
    override def fitSport = Some(Sport.Surfing)
  case object Swim extends StravaSportType:
    override def fitSport = Some(Sport.Swimming)
  case object TableTennis extends StravaSportType
  case object Tennis extends StravaSportType:
    override def fitSport = Some(Sport.Tennis)
  case object TrailRun extends StravaSportType:
    override def fitSport = Some(Sport.Running)
  case object Velomobile extends StravaSportType
  case object VirtualRide extends StravaSportType:
    override def fitSport = Some(Sport.Cycling)
  case object VirtualRow extends StravaSportType:
    override def fitSport = Some(Sport.Rowing)
  case object VirtualRun extends StravaSportType:
    override def fitSport = Some(Sport.Running)
  case object Walk extends StravaSportType:
    override def fitSport = Some(Sport.Walking)
  case object WeightTraining extends StravaSportType:
    override def fitSport = Some(Sport.Training)
  case object Wheelchair extends StravaSportType
  case object Windsurf extends StravaSportType:
    override def fitSport = Some(Sport.Windsurfing)
  case object Workout extends StravaSportType:
    override def fitSport = Some(Sport.Training)
  case object Yoga extends StravaSportType

  val all: NonEmptyList[StravaSportType] =
    NonEmptyList.of(
      AlpineSki,
      BackcountrySki,
      Badminton,
      Canoeing,
      Crossfit,
      EBikeRide,
      Elliptical,
      EMountainBikeRide,
      Golf,
      GravelRide,
      Handcycle,
      HighIntensityIntervalTraining,
      Hike,
      IceSkate,
      InlineSkate,
      Kayaking,
      Kitesurf,
      MountainBikeRide,
      NordicSki,
      Pickleball,
      Pilates,
      Racquetball,
      Ride,
      RockClimbing,
      RollerSki,
      Rowing,
      Run,
      Sail,
      Skateboard,
      Snowboard,
      Snowshoe,
      Soccer,
      Squash,
      StairStepper,
      StandUpPaddling,
      Surfing,
      Swim,
      TableTennis,
      Tennis,
      TrailRun,
      Velomobile,
      VirtualRide,
      VirtualRow,
      VirtualRun,
      Walk,
      WeightTraining,
      Wheelchair,
      Windsurf,
      Workout,
      Yoga
    )

  def fromString(str: String): Either[String, StravaSportType] =
    all.find(_.name.equalsIgnoreCase(str)).toRight(s"Invalid strava sport: $str")
