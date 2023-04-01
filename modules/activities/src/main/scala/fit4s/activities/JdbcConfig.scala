package fit4s.activities

import cats.effect._
import cats.syntax.all._
import fs2.io.file.{Files, Path}

final case class JdbcConfig(url: String, user: String, password: String) {

  override def toString: String =
    s"JdbcConfig($url, $user, ***)"
}

object JdbcConfig {

  /** Creates a database connection configuration using the given existing directory. */
  def fromDirectory(dir: Path): JdbcConfig = {
    val file = dir / "activities.db"
    JdbcConfig(
      s"jdbc:h2:file://${file.absolute.toString};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
      "sa",
      "sa"
    )
  }

  /** Creates a database connection configuration by using either an environment variable
    * `FIT4S_ACTIVITIES_STORAGE_DIR` pointing to an existing directory, or uses a default
    * location.
    */
  def defaultFilesystem[F[_]: Sync: Files]: F[JdbcConfig] = {
    val S = Sync[F]
    val fs = Files[F]

    val fromEnv =
      S.delay(
        Option(System.getenv("FIT4S_ACTIVITIES_STORAGE_DIR"))
          .map(_.trim)
          .filter(_.nonEmpty)
          .map(Path.apply)
      )

    val defaultLocation =
      for {
        home <- fs.userHome
        dir = home / ".config" / "fit4s-activities"
        _ <- fs.createDirectories(dir)
      } yield dir

    val dbFile = fromEnv.flatMap(_.map(S.pure).getOrElse(defaultLocation))
    dbFile.map(fromDirectory)
  }
}
