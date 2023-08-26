package fit4s.cli

import cats.effect.{ExitCode, IO}

import fit4s.cli.tag._

import com.monovore.decline.Opts

object TagCmd extends SharedOpts:
  enum SubOpts:
    case Add(add: AddCmd.Options)
    case List(list: ListCmd.Options)
    case Rename(rename: RenameCmd.Options)
    case Remove(remove: RemoveCmd.Options)
    case Unlink(unlink: UnlinkCmd.Options)

  val addCmd: Opts[AddCmd.Options] =
    Opts.subcommand("add", "Add tags to all selected activities")(AddCmd.opts)

  val listCmd: Opts[ListCmd.Options] =
    Opts.subcommand("list", "List all tags")(ListCmd.opts)

  val renameCmd: Opts[RenameCmd.Options] =
    Opts.subcommand("rename", "Rename a tag")(RenameCmd.opts)

  val removeCmd: Opts[RemoveCmd.Options] =
    Opts.subcommand("remove", "Completely remove a tag")(RemoveCmd.opts)

  val unlinkCmd: Opts[UnlinkCmd.Options] =
    Opts.subcommand("unlink", "Unlink tags from selected activities")(UnlinkCmd.opts)

  val options: Opts[SubOpts] =
    addCmd
      .map(SubOpts.Add.apply)
      .orElse(listCmd.map(SubOpts.List.apply))
      .orElse(renameCmd.map(SubOpts.Rename.apply))
      .orElse(removeCmd.map(SubOpts.Remove.apply))
      .orElse(unlinkCmd.map(SubOpts.Unlink.apply))

  def apply(cliConfig: CliConfig, opts: SubOpts): IO[ExitCode] =
    opts match
      case SubOpts.Add(c)    => AddCmd(cliConfig, c)
      case SubOpts.List(c)   => ListCmd(cliConfig, c)
      case SubOpts.Rename(c) => RenameCmd(cliConfig, c)
      case SubOpts.Remove(c) => RemoveCmd(cliConfig, c)
      case SubOpts.Unlink(c) => UnlinkCmd(cliConfig, c)
