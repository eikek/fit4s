package fit4s.cli

import cats.effect.{ExitCode, IO}

import fit4s.cli.tag._

import com.monovore.decline.Opts

object TagCmd extends SharedOpts {

  sealed trait SubOpts
  object SubOpts {
    case class Add(add: AddCmd.Options) extends SubOpts
    case class List(list: ListCmd.Options) extends SubOpts
    case class Rename(rename: RenameCmd.Options) extends SubOpts
    case class Remove(remove: RemoveCmd.Options) extends SubOpts
    case class Unlink(unlink: UnlinkCmd.Options) extends SubOpts
  }

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
      .map(SubOpts.Add)
      .orElse(listCmd.map(SubOpts.List))
      .orElse(renameCmd.map(SubOpts.Rename))
      .orElse(removeCmd.map(SubOpts.Remove))
      .orElse(unlinkCmd.map(SubOpts.Unlink))

  def apply(cliConfig: CliConfig, opts: SubOpts): IO[ExitCode] =
    opts match {
      case SubOpts.Add(c)    => AddCmd(cliConfig, c)
      case SubOpts.List(c)   => ListCmd(cliConfig, c)
      case SubOpts.Rename(c) => RenameCmd(cliConfig, c)
      case SubOpts.Remove(c) => RemoveCmd(cliConfig, c)
      case SubOpts.Unlink(c) => UnlinkCmd(cliConfig, c)
    }
}
