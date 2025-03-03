package fit4s.activities.records

import cats.Foldable

import fit4s.activities.data.Page

import doobie.*
import doobie.syntax.all.*

trait DoobieSyntax:

  extension (self: Page)
    def asFragment: Fragment =
      if (self == Page.unlimited) Fragment.empty
      else fr"LIMIT ${self.limit} OFFSET ${self.offset}"

  extension [F[_]: Foldable](self: F[Fragment])
    def commas: Fragment =
      self.foldSmash1(Fragment.empty, sql",", Fragment.empty)
