package fit4s.activities.records

import cats.Foldable

import fit4s.activities.data.Page

import doobie._
import doobie.implicits._

trait DoobieSyntax {

  implicit final class PageOps(self: Page) {
    def asFragment: Fragment =
      if (self == Page.unlimited) Fragment.empty
      else fr"LIMIT ${self.limit} OFFSET ${self.offset}"
  }

  implicit final class MoreFragmentFoldableOps[F[_]: Foldable](self: F[Fragment]) {
    def commas: Fragment =
      self.foldSmash1(Fragment.empty, sql",", Fragment.empty)
  }
}
