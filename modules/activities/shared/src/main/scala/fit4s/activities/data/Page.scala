package fit4s.activities.data

final case class Page(limit: Int, offset: Int):
  def withLimit(n: Int): Page = copy(limit = n)
  def withOffset(n: Int): Page = copy(offset = n)

  def next: Page =
    Page(limit, offset + limit)

  def previous: Page =
    if (offset <= 0) this
    else Page(limit, math.max(0, offset - limit))

object Page:
  val unlimited: Page = Page(Int.MaxValue, 0)

  def one(limit: Int): Page = Page(limit, 0)
