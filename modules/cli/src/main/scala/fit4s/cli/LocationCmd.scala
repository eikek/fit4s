package fit4s.cli

import cats.effect._

import fit4s.cli.location._

import com.monovore.decline.Opts

object LocationCmd:
  enum SubOpts:
    case List(cfg: ListCmd.Options)
    case Move(cfg: MoveCmd.Options)
    case Delete(cfg: DeleteCmd.Options)

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
      .map(SubOpts.List.apply)
      .orElse(moveOpts.map(SubOpts.Move.apply))
      .orElse(deleteOpts.map(SubOpts.Delete.apply))

  def apply(cliConfig: CliConfig, opts: SubOpts): IO[ExitCode] =
    opts match
      case SubOpts.List(cfg)   => ListCmd(cliConfig, cfg)
      case SubOpts.Move(cfg)   => MoveCmd(cliConfig, cfg)
      case SubOpts.Delete(cfg) => DeleteCmd(cliConfig, cfg)
