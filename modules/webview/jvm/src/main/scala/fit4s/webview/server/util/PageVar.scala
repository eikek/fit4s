package fit4s.webview.server.util

import cats.data.ValidatedNel
import cats.syntax.all.*

import fit4s.activities.data.Page

import org.http4s.ParseFailure

object PageVar:
  val defaultLimit = 50
  val defaultOffset = 0

  val first = Page(defaultLimit, defaultOffset)

  def unapply(
      params: Map[String, collection.Seq[String]]
  ): Option[ValidatedNel[ParseFailure, Page]] =
    val limit = params
      .get("limit")
      .flatMap(_.headOption)
      .map(parseInt)
      .getOrElse(defaultLimit.validNel)
    val offset = params
      .get("offset")
      .flatMap(_.headOption)
      .map(parseInt)
      .getOrElse(defaultOffset.validNel)

    (limit, offset).mapN(Page.apply).some

  private def parseInt(str: String): ValidatedNel[ParseFailure, Int] =
    str.toIntOption.toValidNel(ParseFailure(s"Invalid integer value: $str", ""))
