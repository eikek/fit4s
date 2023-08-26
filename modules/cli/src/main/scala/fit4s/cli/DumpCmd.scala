package fit4s.cli

import cats.effect._

import fit4s.cli.dump.*

import com.monovore.decline.Opts

object DumpCmd {

  enum SubOpts:
    case Create(cfg: CreateCmd.Options)
    case Read(cfg: ReadCmd.Options)

  private val createOpts: Opts[CreateCmd.Options] =
    Opts.subcommand(
      "create",
      "Create a dump from the current database to a gzipped file."
    )(CreateCmd.opts)

  private val readOpts: Opts[ReadCmd.Options] =
    Opts.subcommand(
      "read",
      "Import a dump into the database. The database must be empty!"
    )(ReadCmd.opts)

  val opts: Opts[SubOpts] =
    createOpts
      .map(SubOpts.Create.apply)
      .orElse(readOpts.map(SubOpts.Read.apply))

  def apply(cliConfig: CliConfig, opts: SubOpts): IO[ExitCode] =
    opts match {
      case SubOpts.Create(cfg) => CreateCmd(cliConfig, cfg)
      case SubOpts.Read(cfg)   => ReadCmd(cliConfig, cfg)
    }
}
