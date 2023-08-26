package fit4s.webview.server.util

import java.time.{Instant, ZoneId}

import cats.data.ValidatedNel
import cats.syntax.all.*

import fit4s.activities.data.*

import org.http4s.ParseFailure

object ActivityQueryVar {

  def unapply(
      params: Map[String, collection.Seq[String]]
  ): Option[(ZoneId, Instant) => ValidatedNel[ParseFailure, ActivityQuery]] = {
    val page = PageVar
      .unapply(params)
      .getOrElse(PageVar.first.validNel)

    val cond = QueryConditionVar.unapply(params)

    Option { (zone, now) =>
      (cond.traverse(_.apply(zone, now)), page).mapN(ActivityQuery.apply)
    }
  }
}
