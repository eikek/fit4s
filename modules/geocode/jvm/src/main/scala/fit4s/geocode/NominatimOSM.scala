package fit4s.geocode

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{Duration, FiniteDuration}

import cats.effect.*
import cats.syntax.all.*
import fs2.io.net.Network

import fit4s.common.util.Cache
import fit4s.data.Position
import fit4s.geocode.NominatimOSM.State
import fit4s.geocode.data.Place
import fit4s.http.borer.BorerEntityCodec.Implicits.*

import org.http4s.Method.GET
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.client.EmberClientBuilder

final class NominatimOSM[F[_]: Async](
    client: Client[F],
    cfg: NominatimConfig,
    state: Ref[F, State],
    cache: Cache[F, Position, Place]
) extends ReverseLookup[F]
    with Http4sClientDsl[F]
    with NominatimDecoder {

  private[this] val logger = scribe.cats.effect[F]

  private val reqEvery =
    FiniteDuration(((1 * 1000) / cfg.maxReqPerSecond).toLong, TimeUnit.MILLISECONDS)

  override def lookup(position: Position): F[Option[Place]] =
    cache.cached(lookupRaw)(position)

  def lookupRaw(position: Position): F[Option[Place]] = {
    // ?format=json&lat=47.4573699&lon=8.4247654
    val url = cfg.baseUrl
      .withQueryParam("format", "json")
      .withQueryParam("lat", position.latitude.toDegree)
      .withQueryParam("lon", position.longitude.toDegree)

    for {
      begin <- Clock[F].monotonic
      nextTime <- state.tryModify(_.nextLookup(begin, reqEvery))
      resp <- nextTime match {
        case Some(d) if d == Duration.Zero =>
          client.expectOption[Place](GET(url)).attempt.flatMap {
            case Right(v) => v.pure[F]
            case Left(ex) =>
              logger.warn(s"Nominatim lookup failed for position $position!", ex).as(None)
          }
        case Some(d) =>
          Async[F].sleep(d).flatMap(_ => lookup(position))
        case None =>
          lookup(position)
      }
    } yield resp
  }
}

object NominatimOSM {

  case class State(lastLookup: FiniteDuration) {
    def nextLookup(now: FiniteDuration, step: FiniteDuration): (State, FiniteDuration) = {
      val next = lastLookup + step
      if (next < now) (copy(lastLookup = now), Duration.Zero)
      else (this, next - now)
    }
  }

  object State {
    val empty: State = State(Duration.Zero)
  }

  def apply[F[_]: Async](client: Client[F], cfg: NominatimConfig): F[ReverseLookup[F]] =
    for {
      state <- Ref.of[F, State](State.empty)
      cache <- PlaceCache.memory(cfg.cacheSize)
    } yield new NominatimOSM[F](client, cfg, state, cache)

  def resource[F[_]: Async: Network](
      cfg: NominatimConfig,
      httpTimeout: Duration
  ): Resource[F, ReverseLookup[F]] =
    EmberClientBuilder.default[F].withTimeout(httpTimeout).build.evalMap(apply(_, cfg))
}
