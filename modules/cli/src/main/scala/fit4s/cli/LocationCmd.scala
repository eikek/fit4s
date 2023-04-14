package fit4s.cli

import cats.effect._
import com.monovore.decline.Opts
import fit4s.cli.location._

object LocationCmd {

  sealed trait SubOpts extends Product
  object SubOpts {
    case class List(cfg: ListCmd.Options) extends SubOpts
    case class Move(cfg: MoveCmd.Options) extends SubOpts
    case class Delete(cfg: DeleteCmd.Options) extends SubOpts
  }

  private val listOpts: Opts[ListCmd.Options] =
    Opts.subcommand("list", "List locations")(ListCmd.opts)

  private val moveOpts: Opts[MoveCmd.Options] =
    Opts.subcommand("move", "Move a location")(MoveCmd.opts)

  private val deleteOpts: Opts[DeleteCmd.Options] =
    Opts.subcommand("delete", "Delete a location and all related activities")(
      DeleteCmd.opts
    )

  val opts: Opts[SubOpts] =
    listOpts
      .map(SubOpts.List)
      .orElse(moveOpts.map(SubOpts.Move))
      .orElse(deleteOpts.map(SubOpts.Delete))

  def apply(cliConfig: CliConfig, opts: SubOpts): IO[ExitCode] =
    opts match {
      case SubOpts.List(cfg)   => ListCmd(cliConfig, cfg)
      case SubOpts.Move(cfg)   => MoveCmd(cliConfig, cfg)
      case SubOpts.Delete(cfg) => DeleteCmd(cliConfig, cfg)
    }
}
