package fit4s.activities.dump

import cats.syntax.all.*

import fit4s.activities.JdbcConfig.Dbms
import fit4s.activities.records.*

import doobie.*
import doobie.syntax.all.*

object RestartSequences:

  private val tables = List(
    RActivity.table,
    RActivityGeoPlace.table,
    RActivityLap.table,
    RActivityLocation.table,
    RActivitySession.table,
    RActivitySessionData.table,
    RActivityStrava.table,
    RActivityTag.table,
    RGeoPlace.table,
    RStravaToken.table,
    RTag.table
  )

  def apply(dbms: Dbms): ConnectionIO[Unit] =
    dbms match
      case Dbms.H2       => forH2
      case Dbms.Postgres => forPostgres

  def forH2: ConnectionIO[Unit] =
    tables.traverse_ { tn =>
      val seqName = Fragment.const(s"${tn.internals.sql.trim}_id_seq")
      sql"""ALTER SEQUENCE $seqName RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM $tn)""".update.run
    }

  def forPostgres: ConnectionIO[Unit] =
    tables.traverse_ { tn =>
      val seqName = s"${tn.internals.sql.trim}_id_seq"
      sql"""SELECT setval($seqName, (SELECT COALESCE(MAX(id), 1) FROM $tn))"""
        .query[Long]
        .unique
    }
