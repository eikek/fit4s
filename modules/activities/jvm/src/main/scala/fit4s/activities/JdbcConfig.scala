package fit4s.activities

import fs2.io.file.Path

import fit4s.activities.JdbcConfig.Dbms

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class JdbcConfig(url: String, user: String, password: String):

  override def toString: String =
    s"JdbcConfig($url, $user, ***)"

  val getDbms: Either[String, Dbms] =
    if (url.toLowerCase.startsWith("jdbc:h2:")) Right(Dbms.H2)
    else if (url.toLowerCase.startsWith("jdbc:postgresql:")) Right(Dbms.Postgres)
    else Left(s"DBMS not supported (Postgres and H2 are): $url")

  lazy val dbms: Dbms = getDbms.fold(sys.error, identity)

object JdbcConfig:
  enum Dbms:
    case H2
    case Postgres

  /** Creates a database connection configuration using the given existing directory. */
  def fromDirectory(dir: Path): JdbcConfig =
    val file = dir / "activities.db"
    JdbcConfig(
      s"jdbc:h2:file://${file.absolute.toString};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
      "sa",
      "sa"
    )

  given Encoder[JdbcConfig] = deriveEncoder
