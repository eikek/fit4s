package fit4s.cli

import cats.effect.*
import cats.syntax.all.*

import fit4s.activities.ImportResult
import fit4s.activities.data.{ActivityId, ActivityRereadData}

import com.monovore.decline.Opts
import io.bullet.borer.Encoder
import io.bullet.borer.Json
import io.bullet.borer.derivation.MapBasedCodecs

object RereadCmd extends SharedOpts {

  final case class Options(query: ActivitySelection, format: OutputFormat)

  val opts: Opts[Options] =
    (activitySelectionOps, outputFormatOpts).mapN(Options.apply)

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        query <- resolveQuery(opts.query, cliCfg.timezone)
        q <- query match
          case None    => IO.raiseError(CliError("A query is mandatory to specify"))
          case Some(v) => IO.pure(v)
        last <- log
          .rereadActivities(cliCfg.timezone, q)
          .map(Result.apply.tupled)
          .zipWithIndex
          .evalMap { case (res, idx) => IO.println(res.format(opts.format)).as(idx + 1) }
          .compile
          .last
        count = last.getOrElse(0)
        _ <- IO.println(s"Re-read $count activities.")
      } yield ExitCode.Success
    }

  def resultLine(data: ActivityRereadData, result: ImportResult[ActivityId]): String =
    result match {
      case ImportResult.Success(id) =>
        s"OK: ${id.id} at ${data.file}"

      case ImportResult.Failure(reason) =>
        s"Err: ${data.file} - ${reason.show}"
    }

  final case class Result(
      data: ActivityRereadData,
      result: ImportResult[ActivityId]
  ):
    def format(out: OutputFormat): String =
      out.fold(
        Json.encode(this).toUtf8String,
        resultLine(data, result)
      )

  object Result:
    given Encoder[ImportResult[ActivityId]] =
      Encoder.forString.contramap(_.show)
    given Encoder[Result] =
      MapBasedCodecs.deriveEncoder
}
