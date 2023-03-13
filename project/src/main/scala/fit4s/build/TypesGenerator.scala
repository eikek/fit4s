package fit4s.build

import model._
object TypesGenerator {

  def generate(pkgname: String, typeDefs: List[TypeDesc]): String = {
    val groups = typeDefs.groupBy(_.name)

    s"""package $pkgname
       |
       |object ProfileTypes {
       |${makeGroup(groups)}
       |}""".stripMargin
  }

  def makeGroup(groups: Map[String, List[TypeDesc]]): String =
    groups
      .map { case (name, defs) =>
        s"""object `$name` {
           |${makeTypes(defs)}
           |}""".stripMargin
      }
      .mkString("\n\n")

  def makeTypes(dfs: List[TypeDesc]): String =
    dfs
      .map(tt => s"""/** ${tt.comment.getOrElse("")} */
                    |object `${tt.valueName}` {
                    |  val baseType: String = "${tt.baseType}"
                    |  val value: Int = ${tt.value}
                    |  val valueName: String = "${tt.valueName}"
                    |}""".stripMargin)
      .mkString("\n\n")
}
