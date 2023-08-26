package fit4s.webview.server.util

import java.time.{Instant, ZoneId}

import cats.data.ValidatedNel
import cats.syntax.all.*

import fit4s.activities.data.*

import org.http4s.ParseFailure

object ActivitySummaryQueryVar {

  def unapply(
      params: Map[String, collection.Seq[String]]
  ): Option[(ZoneId, Instant) => ValidatedNel[ParseFailure, ActivityQuery]] = {
    val page = params
      .get("limit")
      .flatMap(_.headOption)
      .map(n => parseInt(n).map(Page.unlimited.withLimit))
      .getOrElse(Page.unlimited.validNel)

    val cond = QueryConditionVar.unapply(params)

    Option { (zone, now) =>
      (cond.traverse(_.apply(zone, now)), page).mapN(ActivityQuery.apply)
    }
  }

  private def parseInt(str: String): ValidatedNel[ParseFailure, Int] =
    str.toIntOption.toValidNel(ParseFailure(s"Invalid integer value: $str", ""))
}
