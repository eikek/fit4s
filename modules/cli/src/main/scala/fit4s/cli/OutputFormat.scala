package fit4s.cli

sealed trait OutputFormat {}

object OutputFormat {
  final case object Json extends OutputFormat
  final case object Text extends OutputFormat
}
