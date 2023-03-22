package fit4s.cli

import com.monovore.decline.Opts
import fs2.io.file.Path

trait BasicOpts {

  def fileArg: Opts[Path] =
    Opts
      .argument[java.nio.file.Path](metavar = "file")
      .map(Path.fromNioPath)

}
