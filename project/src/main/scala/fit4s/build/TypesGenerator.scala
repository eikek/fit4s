package fit4s.build

import model._
import src.main.scala.fit4s.build.SourceFile
object TypesGenerator {

  def generate(pkgname: String, typeDefs: List[TypeDesc]): List[SourceFile] = {
    val groups = typeDefs.groupBy(_.name)
    makeFiles(pkgname, groups)
  }

  def makeFiles(pkg: String, groups: Map[String, List[TypeDesc]]): List[SourceFile] =
    groups.view.map { case (name, defs) =>
      makeTypes(pkg, name, defs)
    }.toList

  def makeTypes(pkg: String, name: String, dfs: List[TypeDesc]): SourceFile = {
    val objName = snakeCamel(name)

    def caseObject(td: TypeDesc) = {
      val line =
        s"case object ${snakeCamel(td.valueName)} extends $objName { val rawValue = ${td.value}L }"
      td.comment match {
        case Some(comment) =>
          s"/** $comment */\n  $line"
        case None => line
      }
    }

    def values = dfs.map(caseObject).mkString("  ", "\n  ", "  ")

    val contents =
      s"""package $pkg.basetypes
         |/* This file has been generated. */
         |
         |sealed trait $objName extends fit4s.profile.GenBaseType
         |object $objName extends fit4s.profile.GenBaseTypeCompanion[$objName] {
         |
         |$values
         |
         |  val all: List[$objName] =
         |    List(${dfs.map(_.valueName).map(snakeCamel).mkString(", ")})
         |
         |  protected val allMap: Map[$objName, Long] =
         |    all.map(e => e -> e.rawValue).toMap
         |
         |  protected val baseCodecName: String = "${dfs.head.baseType}"
         |}
       """.stripMargin
    SourceFile(s"${snakeCamel(name)}.scala", contents)
  }

  private def snakeCamel(str: String): String = {
    val digits = str.takeWhile(_.isDigit)
    if (digits.isEmpty) str.split('_').map(_.capitalize).mkString
    else snakeCamel(str.drop(digits.length) + digits)
  }
}
