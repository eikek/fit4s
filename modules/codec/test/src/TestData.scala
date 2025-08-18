package fit4s.codec

import java.net.URL
import java.util.concurrent.atomic.AtomicReference

import scodec.bits.ByteVector

object TestData:
  final class TestFile(val name: String, val url: URL) {
    lazy val contents: ByteVector =
      ByteVector.view(url.openStream.readAllBytes())

    def source: ByteSource = ByteSource.fromURL(url).withName(name)
  }

  abstract class TestFiles {
    private val registry: AtomicReference[Map[String, TestFile]] =
      new AtomicReference(Map.empty)
    def all: Iterable[TestFile] =
      registry.get().values

    protected def r(resource: String): TestFile =
      val testFile = readFile(resource)
      registry.updateAndGet(data => data.updated(resource, testFile))
      testFile
  }

  object Corrupted extends TestFiles {
    val badCrc = r("/files/610464_bad_crc.fit")
    val noDefMsg = r("/files/610464_no_defmsg.fit")
  }

  object Workouts extends TestFiles {
    val workout = r("/files/workout.fit")
  }

  object Activities extends TestFiles {

    val fenix539 = r("/files/fenix5_indoor_539.fit")
    val edge16 = r("/files/edge530_indoor_16.fit")
    val edge1536 = r("/files/edge1040_1536.fit")
    val swim626 = r("/files/swim_626.fit")
    val edge146 = r("/files/edge530_146.fit")
    val fenix521 = r("/files/fenix5_openwater_521.fit")
    val fenix254 = r("/files/fenix5_lapswim_254.fit")

    // https://github.com/karaul/fitplotter/tree/main/examples
    val fr935run = r("/files/fr935_karaul-fitplotter.fit")

    // downloaded from zwift
    val zwift = r("/files/zwift_activity.fit")

    // https://github.com/adriangibbons/php-fit-file-analysis/issues/47#issue-307898230
    val run2018 = r("/files/run-20180322T212609.fit")

    // https://github.com/adriangibbons/php-fit-file-analysis/issues/47#issuecomment-383787693
    val fit610464 = r("/files/610464.fit")

    // https://github.com/adriangibbons/php-fit-file-analysis/issues/47#issuecomment-402627765
    val fit27753 = r("/files/2775379975.fit")
    val fit27804 = r("/files/2780451464.fit")
    val fit28253 = r("/files/2825367441.fit")

    // https://github.com/adriangibbons/php-fit-file-analysis/issues/54#issuecomment-513231277
    val fit54 = r("/files/file54.fit")

    // https://github.com/adriangibbons/php-fit-file-analysis/issues/64#issue-708477095
    val fit682708 = r("/files/68270818303.fit")

    // https://github.com/Runalyze/Runalyze/tree/support/4.3.x/tests/testfiles/fit
    val fenix2 = r("/runalyze/Fenix-2.fit")
    val fenix2NegativeTimes = r("/runalyze/Fenix-2-negative-times.fit")
    val fenix2Pauses = r("/runalyze/Fenix-2-pauses.fit")
    val fenix3EmptyRunscribe = r("/runalyze/Fenix-3-with-empty-runscribe.fit")
    val fenix3InactiveMoxy = r("/runalyze/Fenix-3-with-inactive-Moxy.fit")
    val fenix3Runscribe = r("/runalyze/Fenix-3-with-runscribe-v1-38.fit")
    val fr630Lth = r("/runalyze/FR630-with-lth.fit")
    val fr70Intervals = r("/runalyze/FR70-intervals.fit")
    val fr735xtPerfCond = r("/runalyze/FR735XT-with-performance-condition.fit")
    val fr920StartEvents = r("/runalyze/FR920-additional-start-events.fit")
    val fr920Runscribe = r("/runalyze/FR920xt-with-runscribe-plus.fit")
    val fr920Stryd = r("/runalyze/FR920xt-with-Stryd.fit")
    val runPower2 = r("/runalyze/garmin-runPower-2.fit")
    val runPower = r("/runalyze/garmin-runPower.fit")
    val hrOnlyZeros = r("/runalyze/hr-only-zeros.fit")
    val hrvExample = r("/runalyze/HRV-example.fit")
    val invalidAltitudeEmtpyRecordAtEnd = r(
      "/runalyze/invalid-altitude-and-empty-record-at-end.fit"
    )
    val bikeCadence = r("/runalyze/IPBike-cadence.fit")
    val moxy2Sensors = r("/runalyze/moxy-2sensors.fit")
    val moxyFloat = r("/runalyze/moxy-float.fit")
    val moxyFr735 = r("/runalyze/moxy-fr735.fit")
    val multisession = r("/runalyze/Multisession.fit")
    val multisessionStopAfterTransition = r(
      "/runalyze/Multisession-stop-after-transition.fit"
    )
    val multisportTriathlon = r("/runalyze/multisport-triathlon-fenix3.fit")
    val oneSecondJumpToPast = r("/runalyze/One-second-jump-to-past.fit")
    val osynceStopBug = r("/runalyze/osynce-stop-bug.fit")
    val standard = r("/runalyze/Standard.fit")
    val supportPronation = r("/runalyze/support-001885-rs-pronation.fit")
    val suuntoAmbit3NoFinalLap = r("/runalyze/Suunto-Ambit-3-Peak-without-final-lap.fit")
    val swim25mLane = r("/runalyze/swim-25m-lane.fit")
    val swimFenix50mLane = r("/runalyze/swim-fenix-50m.fit")
    val swimOutdoor = r("/runalyze/swim-outdoor.fit")
    val swimPoolIq = r("/runalyze/swim-pool-via-iq.fit")
    val swimIq = r("/runalyze/swim-via-iq.fit")
    val timeJump = r("/runalyze/time-jump.fit")
    val zwiftBadTraining = r("/runalyze/Zwift-bad-training-effect.fit")
    val withPower = r("/runalyze/with-power.fit")
    val withNewDynamics = r("/runalyze/with-new-dynamics.fit")

  }

  def allFiles = Activities.all ++ Workouts.all

  def allCount = allFiles.size

  def readFile(name: String): TestFile =
    Option(getClass.getResource(name)) match
      case None     => sys.error(s"Resource not found: $name")
      case Some(in) => TestFile(name, in)
