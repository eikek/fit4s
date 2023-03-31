package fit4s.activities.data

final case class Page(limit: Int, offset: Int)

object Page {
  val unlimited: Page = Page(Int.MaxValue, 0)

  def one(limit: Int): Page = Page(limit, 0)
}
