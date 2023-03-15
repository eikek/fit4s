package src.main.scala.fit4s.build

import fit4s.build.model._

object MessagesGenerator {

  def generate(
      pkgname: String,
      msgDefs: List[MessageDef],
      typeDefs: List[TypeDesc]
  ): List[SourceFile] = {
    val fields = msgDefs.groupBy(_.messageName)
    makeAllMsgsFile(pkgname, fields.keys.toList) ::
      makeFiles(typeDefs)(pkgname, fields)
  }

  def makeAllMsgsFile(pkg: String, names: List[String]): SourceFile = {
    val name = "FitMessages.scala"
    val defs = names.sorted
      .map(n => s"val ${snakeCamelIdent(n)} = add(msg.${snakeCamelType(n)}Msg)")
      .mkString("  ", "\n  ", "  ")
    val contents =
      s"""package ${pkg}
         |
         |object FitMessages extends Msgs {
         |$defs
         |}
         |""".stripMargin

    SourceFile(name, contents)
  }

  def makeFiles(
      typeDefs: List[TypeDesc]
  )(pkg: String, groups: Map[String, List[MessageDef]]): List[SourceFile] =
    groups.view.map { case (name, defs) =>
      makeFile(typeDefs)(pkg, name, defs)
    }.toList

  def makeFile(
      typeDefs: List[TypeDesc]
  )(pkg: String, name: String, dfs: List[MessageDef]): SourceFile = {
    val objName = snakeCamelType(name)

    val fileName = s"${objName}Msg.scala"
    val fieldDecl =
      dfs.map(makeFieldDecl(typeDefs)).mkString("  ", "\n  ", "  ")

    val contents =
      s"""package ${pkg}.msg
         |
         |import ${pkg}.basetypes._
         |import fit4s.profile._
         |
         |object ${objName}Msg extends Msg {
         |  val globalMessageNumber: MesgNum = MesgNum.${objName}
         |
         |  $fieldDecl
         |
         |  override def toString(): String = "${objName}Msg"
         |}
         |
         |""".stripMargin

    SourceFile(fileName, contents)
  }

  def makeFieldDecl(typeDefs: List[TypeDesc])(fd: MessageDef): String = {
    val baseTypeNames = typeDefs.filter(_.name == "fit_base_type").map(_.valueName).toSet
    val typeName = scalaTypeName(fd.fieldType, baseTypeNames)
    val baseType =
      if (baseTypeNames.contains(fd.fieldType))
        s"FitBaseType.${snakeCamelType(fd.fieldType)}"
      else if (fd.fieldType == "bool") "FitBaseType.Enum"
      else s"${typeName}.baseType"

    s"""
       |/** ${fd.comment.getOrElse("")} */
       |val ${snakeCamelIdent(fd.fieldName)}: Msg.Field[$typeName] =
       |  add(
       |    Msg.Field(
       |      fieldDefinitionNumber = ${fd.fieldDefNumber.getOrElse(-1)},
       |      fieldName = "${fd.fieldName}",
       |      fieldTypeName = "${fd.fieldType}",
       |      fieldBaseType = $baseType,
       |      fieldCodec = ${scalaTypeCodec(fd.fieldType, baseTypeNames)},
       |      isArray = Msg.ArrayDef.${fd.isArray},
       |      components = ${fd.components.map(_.inQuotes)},
       |      scale = ${fd.scale},
       |      offset = ${fd.offset},
       |      units = ${fd.units.map(_.inQuotes)},
       |      bits = ${fd.bits}
       |    )
       |  )
       |""".stripMargin
  }

  def scalaTypeName(name: String, baseTypes: Set[String]): String =
    if (baseTypes.contains(name) || name == "bool") s"FitBaseType"
    else s"${snakeCamelType(name)}"

  def scalaTypeCodec(name: String, baseTypes: Set[String]): String = {
    val tn = snakeCamelType(name)
    if (baseTypes.contains(name) || name == "bool") s"FitBaseType.codec"
    else s"$tn.codec"
  }

  implicit class StringOps(self: String) {
    def inQuotes = s""""$self""""
  }
}
