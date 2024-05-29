package fit4s.webview.server.util

import fit4s.activities.data.ActivityId

object ActivityIdVar:

  def unapply(str: String): Option[ActivityId] =
    str.toLongOption.map(ActivityId.apply)
