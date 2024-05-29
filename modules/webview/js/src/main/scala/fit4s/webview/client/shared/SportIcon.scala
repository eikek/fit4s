package fit4s.webview.client.shared

import cats.data.NonEmptyList
import cats.effect.*
import fs2.dom.*

import fit4s.common.instances.all.given
import fit4s.profile.types.Sport

import calico.*
import calico.html.Modifier
import calico.html.io.{*, given}

object SportIcon:
  private val fa = "fa"

  def apply[M](s: Sport, moreCls: List[String] = Nil, modifier: M = ())(using
      M: Modifier[IO, HtmlElement[IO], M]
  ): Resource[IO, HtmlElement[IO]] =
    i(cls := fa :: iconClass(s) :: moreCls, modifier)

  def of[M](s: NonEmptyList[Sport], moreCls: List[String] = Nil, modifier: M = ())(using
      M: Modifier[IO, HtmlElement[IO], M]
  ): Resource[IO, HtmlElement[IO]] =
    s.distinct match
      case NonEmptyList(sport, Nil) =>
        apply(sport, moreCls, modifier)
      case _ =>
        i(cls := "fa fa-stopwatch-20" :: moreCls, modifier)

  def iconClass(s: Sport): String = s match
    case Sport.Cycling            => "fa-person-biking"
    case Sport.Running            => "fa-person-running"
    case Sport.Swimming           => "fa-person-swimming"
    case Sport.Soccer             => "fa-futbol"
    case Sport.Snowboarding       => "fa-person-snowboarding"
    case Sport.CrossCountrySkiing => "fa-person-skiing-nordic"
    case Sport.Walking            => "fa-person-walking"
    case Sport.Hiking             => "fa-person-hiking"
    case Sport.AlpineSkiing       => "fa-person-skiing"
    case Sport.Basketball         => "fa-basketball"
    case _                        => "fa-shoe-prints"
    // was too lazy to add more…
