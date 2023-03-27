package fit4s.cli

import com.monovore.decline.Opts
import fs2.io.file.Path

trait BasicOpts {

  def fileArg: Opts[Path] =
    Opts
      .argument[java.nio.file.Path](metavar = "file")
      .map(Path.fromNioPath)
      .validate(s"file must be a file")(p =>
        !java.nio.file.Files.isDirectory(p.toNioPath)
      )

  def dirArg: Opts[Path] =
    Opts
      .argument[java.nio.file.Path](metavar = "dir")
      .map(Path.fromNioPath)
      .validate(s"dir must be a directory")(p =>
        java.nio.file.Files.isDirectory(p.toNioPath)
      )
}
