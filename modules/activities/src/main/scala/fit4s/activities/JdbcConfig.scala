package fit4s.activities

import fs2.io.file.Path

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
}
