package fit4s.cli

import cats.Show

import fit4s.activities.data.{ActivityId, Tag}

trait FormatDefinition {
  implicit val activityIdShow: Show[ActivityId] =
    Show.show(aid => f"${aid.id}% 4d")

  implicit val tagVectorShow: Show[Vector[Tag]] =
    Show.show { records =>
      records.map(_.name.name).sorted.mkString("[", ",", "]")
    }

  implicit class StringOps(self: String) {
    def in(s: Styles): String =
      if (self.isBlank) ""
      else s"${s.style}$self${Console.RESET}"
  }
}

object FormatDefinition extends FormatDefinition
