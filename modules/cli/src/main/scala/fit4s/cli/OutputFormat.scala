package fit4s.cli

sealed trait OutputFormat {
  def fold[A](json: => A, text: => A): A
}

object OutputFormat {
  final case object Json extends OutputFormat {
    def fold[A](json: => A, text: => A): A = json
  }
  final case object Text extends OutputFormat {
    def fold[A](json: => A, text: => A): A = text
  }
}
