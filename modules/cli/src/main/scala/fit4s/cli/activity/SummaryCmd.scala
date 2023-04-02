package fit4s.cli.activity

import cats.effect.{Clock, ExitCode, IO}
import cats.syntax.all._
import fit4s.activities.ActivityQuery.OrderBy
import fit4s.activities.impl.ConditionParser
import fit4s.activities.{ActivityLog, ActivityQuery}
import fit4s.cli.CliError

import java.time.ZoneId

object SummaryCmd {

  final case class Config(
      query: Option[String]
  )

  def apply(cfg: Config): IO[ExitCode] =
    ActivityLog.default[IO]().use { log =>
      for {
        currentTime <- Clock[IO].realTimeInstant

        query <- cfg.query match {
          case Some(q) =>
            new ConditionParser(ZoneId.systemDefault(), currentTime)
              .parseCondition(q)
              .fold(
                err => IO.raiseError(new CliError(s"Query parsing failed: $err")),
                IO.pure
              )
              .map(_.some)

          case None => IO.pure(None)
        }

        sessions <- log
          .activityList(ActivityQuery(query, OrderBy.StartTime))
          .compile
          .toVector

        _ <- IO.println(ConsoleUtil.printHeader("Summary"))
        _ <- IO.println(Summary.summarize(sessions).summaryTable(2))

      } yield ExitCode.Success
    }
}
