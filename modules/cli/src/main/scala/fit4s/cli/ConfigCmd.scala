package fit4s.cli

import cats.effect._
import com.monovore.decline.Opts
import fit4s.cli.config._

object ConfigCmd {

  sealed trait SubOpts extends Product
  object SubOpts {
    case class ListDefault(cfg: ListDefaultCmd.Options) extends SubOpts
  }

  private val listOpts: Opts[ListDefaultCmd.Options] =
    Opts.subcommand("list-defaults", "List locations")(ListDefaultCmd.opts)

  val opts: Opts[SubOpts] =
    listOpts
      .map(SubOpts.ListDefault)

  @annotation.nowarn
  def apply(cliConfig: CliConfig, opts: SubOpts): IO[ExitCode] =
    opts match {
      case SubOpts.ListDefault(cfg) => ListDefaultCmd(cfg)
    }
}
