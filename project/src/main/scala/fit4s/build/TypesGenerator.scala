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
      if (isSingleton(defs)) makeSingleton(pkg, defs.head)
      else if (name == "date_time") makeDateTime(pkg, defs.head)
      else if (name == "local_date_time") makeDateTime(pkg, defs.head)
      else makeTypes(pkg, name, defs)
    }.toList

  def isSingleton(dfs: List[TypeDesc]) =
    dfs.tail.isEmpty && dfs.headOption.exists(_.valueName.isEmpty)

  def makeDateTime(pkg: String, td: TypeDesc): SourceFile = {
    val objName = snakeCamelType(td.name)
    val contents =
      s"""package $pkg.types
         |/* This file has been generated. */
         |
         |/** ${td.comment.getOrElse("")} */
         |final case class $objName(rawValue: Long) extends GenFieldType {
         |  val typeName: String = "${td.name}"
         |  def asInstant: java.time.Instant =
         |    $objName.offset.plusSeconds(rawValue)
         |
         |  def asLocalDateTime: java.time.LocalDateTime =
         |    $objName.offset.plusSeconds(rawValue).atOffset(java.time.ZoneOffset.UTC).toLocalDateTime
         |
         |  def isSystemTime: Boolean =
         |    rawValue < $objName.minTimeForOffset
         |}
         |object $objName extends ${objName}Companion {
         |  val minTimeForOffset: Long = 0x10000000L
         |  val offset: java.time.Instant = java.time.Instant.parse("1989-12-31T00:00:00Z")
         |
         |  val baseType: FitBaseType = FitBaseType.${snakeCamelType(td.baseType)}
         |}
       """.stripMargin
    SourceFile(s"${snakeCamelType(td.name)}.scala", contents)
  }

  def makeSingleton(pkg: String, td: TypeDesc): SourceFile = {
    if (td.baseType != "uint32") sys.error(s"Unexpected base type: $td")
    val objName = snakeCamelType(td.name)
    val contents =
      s"""package $pkg.types
         |/* This file has been generated. */
         |
         |import scodec.Codec
         |import scodec.bits.ByteOrdering
         |
         |/** ${td.comment.getOrElse("")} */
         |final case class $objName(rawValue: Long) extends GenFieldType {
         |  val typeName: String = "${td.name}"
         |}
         |object $objName extends GenFieldTypeCompanion[$objName] {
         |  override def codec(bo: ByteOrdering): Codec[$objName] =
         |    fit4s.codecs.ulongx(32, bo).xmap($objName.apply(_), _.rawValue)
         |
         |  val baseType: FitBaseType = FitBaseType.${snakeCamelType(td.baseType)}
         |}
       """.stripMargin
    SourceFile(s"${snakeCamelType(td.name)}.scala", contents)
  }

  def makeTypes(pkg: String, name: String, dfs: List[TypeDesc]): SourceFile = {
    val objName = snakeCamelType(name)

    def caseObject(td: TypeDesc) = {
      val line =
        s"""
           |case object ${snakeCamelType(td.valueName)} extends $objName {
           |  val rawValue = ${td.value}L
           |  val typeName = "${td.valueName}"
           |}
           |""".stripMargin
      td.comment match {
        case Some(comment) =>
          s"/** $comment */\n  $line"
        case None => line
      }
    }

    def values = dfs.map(caseObject).mkString("  ", "\n  ", "  ")

    val contents =
      s"""package $pkg.types
         |/* This file has been generated. */
         |
         |sealed trait $objName extends GenFieldType
         |object $objName extends EnumFieldTypeCompanion[$objName] {
         |
         |$values
         |
         |  val all: List[$objName] =
         |    List(${dfs.map(_.valueName).map(snakeCamelType).mkString(", ")})
         |
         |  protected val allMap: Map[$objName, Long] =
         |    all.map(e => e -> e.rawValue).toMap
         |
         |  val baseType: FitBaseType = FitBaseType.${snakeCamelType(dfs.head.baseType)}
         |}
       """.stripMargin
    SourceFile(s"${snakeCamelType(name)}.scala", contents)
  }
}
