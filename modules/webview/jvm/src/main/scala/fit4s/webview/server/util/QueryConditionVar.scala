package fit4s.webview.server.util

import java.time.{Instant, ZoneId}

import cats.data.ValidatedNel
import cats.syntax.all.*

import fit4s.activities.data.QueryCondition

import org.http4s.ParseFailure

object QueryConditionVar:

  def validate(zoneId: ZoneId, currentTime: Instant)(
      cond: String
  ): ValidatedNel[ParseFailure, QueryCondition] =
    QueryCondition
      .parser(zoneId, currentTime)(cond)
      .leftMap(err => ParseFailure(err, ""))
      .toValidatedNel

  def unapply(
      params: Map[String, collection.Seq[String]]
  ): Option[(ZoneId, Instant) => ValidatedNel[ParseFailure, QueryCondition]] =
    params
      .get("q")
      .flatMap(_.headOption)
      .map(str => validate(_, _)(str))
