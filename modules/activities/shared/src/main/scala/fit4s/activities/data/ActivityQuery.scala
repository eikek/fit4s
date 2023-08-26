package fit4s.activities.data

final case class ActivityQuery(condition: Option[QueryCondition], page: Page)

object ActivityQuery {}
