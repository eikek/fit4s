package fit4s.activities

import fs2.io.file.Path

trait ImportCallback[F[_]] {
  def onFile(file: Path): F[Unit]
}
