package fit4s.cli

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.cli.tag.{AddCmd, ListCmd}

object TagCmd extends BasicOpts {

  sealed trait SubOpts
  object SubOpts {
    case class Add(add: AddCmd.Options) extends SubOpts
    case class List(list: ListCmd.Options) extends SubOpts
  }

  val addCmd: Opts[AddCmd.Options] =
    Opts.subcommand("add", "Add tags to all selected activities") {
      (activitySelectionOps, addTags).mapN(AddCmd.Options)
    }

  val listCmd: Opts[ListCmd.Options] =
    Opts.subcommand("list", "List all tags") {
      tagFilter.map(ListCmd.Options)
    }

  val options: Opts[SubOpts] =
    addCmd
      .map(SubOpts.Add)
      .orElse(listCmd.map(SubOpts.List))

  def apply(cliConfig: CliConfig, opts: SubOpts): IO[ExitCode] =
    opts match {
      case SubOpts.Add(c)  => AddCmd(cliConfig, c)
      case SubOpts.List(c) => ListCmd(cliConfig, c)
    }
}
