package fit4s.webview.server.util

import cats.data.ValidatedNel
import cats.syntax.all._

import fit4s.activities.data.TagName

import org.http4s.ParseFailure

object TagNamesVar {
  def validate(name: String): ValidatedNel[ParseFailure, TagName] =
    TagName.fromString(name).leftMap(ParseFailure(_, "")).toValidatedNel

  def unapply(
      params: Map[String, collection.Seq[String]]
  ): Option[ValidatedNel[ParseFailure, List[TagName]]] =
    params
      .get("tag")
      .map(_.toList)
      .getOrElse(Nil)
      .traverse(validate)
      .some

  def unapply(name: String): Option[TagName] =
    TagName.fromString(name).toOption
}
