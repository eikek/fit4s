package fit4s.geocode

import cats.effect.IO
import fit4s.data.{Position, Semicircle}
import munit.CatsEffectSuite

import java.time.Instant

class PlayTest extends CatsEffectSuite {

  test("test") {
    NominatimOSM.resource[IO](NominatimConfig()).use { geo =>
      val pos1 =
        Position(Semicircle.semicircle(565303417), Semicircle.semicircle(101948041))
      val pos2 =
        Position(
          Semicircle.degree(47.457635249999996),
          Semicircle.degree(8.42487748403805)
        )

      println(Instant.now())

      for (_ <- 1 to 10) {
        val p = geo.lookup(if (math.random() > 0.5) pos1 else pos2).unsafeRunSync()
        println(s"${Instant.now()}: $p")
      }

      IO.unit
    }
  }

}
