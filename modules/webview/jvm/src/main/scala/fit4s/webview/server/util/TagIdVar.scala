package fit4s.webview.server.util

import fit4s.activities.data.TagId

object TagIdVar:

  def unapply(str: String): Option[TagId] =
    str.toLongOption.map(TagId.apply)
