package src.main.scala.fit4s.build

import fit4s.build.model._

object MessagesGenerator {
  private val longBaseValue = "BaseTypedValue.LongBaseValue"
  private val floatBaseValue = "BaseTypedValue.FloatBaseValue"
  private val stringBaseValue = "BaseTypedValue.StringBaseValue"

  def generate(
      pkgname: String,
      msgDefs: List[MessageDef],
      typeDefs: List[TypeDesc]
  ): List[SourceFile] =
    makeAllMsgsFile(pkgname, msgDefs.map(_.messageName)) ::
      makeFiles(typeDefs)(pkgname, msgDefs)

  def makeAllMsgsFile(pkg: String, names: List[String]): SourceFile = {
    val name = "FitMessages.scala"
    val defs = names.sorted
      .map(n => s"val ${snakeCamelIdent(n)} = add(${snakeCamelType(n)}Msg)")
      .mkString("  ", "\n  ", "  ")
    val contents =
      s"""package ${pkg}.messages
         |
         |object FitMessages extends Msgs {
         |$defs
         |}
         |""".stripMargin

    SourceFile(name, contents)
  }

  def makeFiles(
      typeDefs: List[TypeDesc]
  )(pkg: String, groups: List[MessageDef]): List[SourceFile] =
    groups.view.map {
      makeFile(typeDefs, pkg)
    }.toList

  def messageTypeName(messageDef: MessageDef): String =
    s"${snakeCamelType(messageDef.messageName)}Msg"

  def makeFile(
      typeDefs: List[TypeDesc],
      pkg: String
  )(msgDef: MessageDef): SourceFile = {
    val objName = snakeCamelType(msgDef.messageName)

    val fileName = s"${objName}Msg.scala"
    val fieldDecl =
      msgDef.fields.map(makeMessageField(typeDefs, msgDef)).mkString("  ", "\n  ", "  ")

    val contents =
      s"""package ${pkg}.messages
         |
         |import ${pkg}.types._
         |
         |object ${messageTypeName(msgDef)} extends Msg {
         |  val globalMessageNumber: MesgNum = MesgNum.${objName}
         |
         |$fieldDecl
         |
         |  override def toString(): String = "${messageTypeName(msgDef)}"
         |}
         |
         |""".stripMargin

    SourceFile(fileName, contents)
  }

  def makeMessageField(typeDefs: List[TypeDesc], message: MessageDef)(
      fd: MessageFieldLine
  ): String = {
    val refFields = fd.subFields.flatMap(makeReferencedFields(typeDefs, message, fd))
    val subFields = fd.subFields.map(makeSubfieldDecl(typeDefs, message, fd))
    val field = makeFieldDecl(typeDefs)(fd)
    (refFields ::: subFields ::: List(field)).mkString("\n")
  }

  def makeSubfieldValName(
      messageDef: MessageFieldLine,
      subfieldDef: MessageSubFieldLine
  ): String =
    snakeCamelIdent(s"${messageDef.fieldName}_${subfieldDef.fieldName}")

  def makeReferencedFieldVarName(
      sf: MessageSubFieldLine,
      refFieldName: String,
      refFieldValue: String
  ): String =
    snakeCamelIdent(sf.fieldName + "_when_" + refFieldName + "_" + refFieldValue)

  def makeReferencedFields(
      typeDefs: List[TypeDesc],
      messageDef: MessageDef,
      md: MessageFieldLine
  )(sf: MessageSubFieldLine): List[String] =
    // referenced fields is a comma separated list
    sf.refFieldName.zip(sf.refFieldValue) match {
      case Nil  => Nil
      case list =>
        list.map { case (name, value) =>
          val baseTypeNames =
            typeDefs.filter(_.name == "fit_base_type").map(_.valueName).toSet
          val referencedField =
            messageDef
              .findField(name)
              .getOrElse(
                sys.error(
                  s"Referenced field '$name' not found in msg '${messageDef.messageName}'"
                )
              )
          val typeName = scalaTypeName(referencedField.fieldType, baseTypeNames)
          val fieldRef = snakeCamelIdent(referencedField.fieldName)
          val varName = makeReferencedFieldVarName(sf, name, value)
          val typeValue = typeDefs
            .find(td => td.valueName == value)
            .map(td => snakeCamelType(td.valueName))
            .getOrElse(
              sys.error(
                s"No type '${referencedField.fieldType}' found for referenced field '${name}'"
              )
            )

          s"""
             |  lazy val $varName =
             |    Msg.ReferencedField[$typeName](
             |      refField = ${messageTypeName(messageDef)}.$fieldRef,
             |      refFieldValue = $typeName.$typeValue
             |  )
             |""".stripMargin
        }
    }

  def makeSubfieldDecl(
      typeDefs: List[TypeDesc],
      messageDef: MessageDef,
      md: MessageFieldLine
  )(fd: MessageSubFieldLine): String = {
    val baseTypeNames = typeDefs.filter(_.name == "fit_base_type").map(_.valueName).toSet
    val typeName = scalaTypeName(fd.fieldType, baseTypeNames)
    val typeNameOrLong =
      if (baseTypeNames.contains(fd.fieldType) || fd.fieldType == "bool") longBaseValue
      else typeName
    val baseType =
      if (baseTypeNames.contains(fd.fieldType))
        s"FitBaseType.${snakeCamelType(fd.fieldType)}"
      else if (fd.fieldType == "bool") "FitBaseType.Enum"
      else s"${typeName}.baseType"

    val refFields = fd.refFieldName
      .zip(fd.refFieldValue)
      .map { case (name, value) =>
        makeReferencedFieldVarName(fd, name, value)
      }
      .mkString(", ")

    s"""
       |/** ${fd.comment.getOrElse("")} */
       |  lazy val ${makeSubfieldValName(md, fd)}: Msg.SubField[$typeNameOrLong] =
       |    Msg.SubField(
       |      fieldName = "${fd.fieldName}",
       |      fieldTypeName = "${fd.fieldType}",
       |      fieldBaseType = $baseType,
       |      fieldCodec = ${scalaTypeCodec(fd.fieldType, baseType, baseTypeNames)},
       |      isArray = Msg.ArrayDef.${fd.isArray},
       |      components = ${fd.components.map(_.inQuotes)},
       |      scale = ${fd.scale},
       |      offset = ${fd.offset},
       |      units = ${fd.units.map(_.inQuotes)},
       |      bits = ${fd.bits},
       |      references = List($refFields)
       |    )
       |""".stripMargin
  }

  def makeFieldDecl(typeDefs: List[TypeDesc])(fd: MessageFieldLine): String = {
    val baseTypeNames = typeDefs.filter(_.name == "fit_base_type").map(_.valueName).toSet
    val typeName = scalaTypeName(fd.fieldType, baseTypeNames)
    val typeNameOrLong =
      if (fd.fieldType == "string") stringBaseValue
      else if (Set("float32", "float64").contains(fd.fieldType)) floatBaseValue
      else if (baseTypeNames.contains(fd.fieldType) || fd.fieldType == "bool")
        longBaseValue
      else typeName
    val baseType =
      if (baseTypeNames.contains(fd.fieldType))
        s"FitBaseType.${snakeCamelType(fd.fieldType)}"
      else if (fd.fieldType == "bool") "FitBaseType.Enum"
      else s"${typeName}.baseType"

    val subfields = fd.subFields.map(sd => makeSubfieldValName(fd, sd)).mkString(", ")

    s"""
       |/** ${fd.comment.getOrElse("")} */
       |  val ${snakeCamelIdent(fd.fieldName)}: Msg.Field[$typeNameOrLong] =
       |    add(
       |      Msg.Field(
       |        fieldDefinitionNumber = ${fd.fieldDefNumber},
       |        fieldName = "${fd.fieldName}",
       |        fieldTypeName = "${fd.fieldType}",
       |        fieldBaseType = $baseType,
       |        fieldCodec = ${scalaTypeCodec(fd.fieldType, baseType, baseTypeNames)},
       |        isArray = Msg.ArrayDef.${fd.isArray},
       |        components = ${fd.components.map(_.inQuotes)},
       |        scale = ${fd.scale},
       |        offset = ${fd.offset},
       |        units = ${fd.units.map(_.inQuotes)},
       |        bits = ${fd.bits},
       |        subFields = () => List($subfields)
       |      )
       |    )
       |""".stripMargin
  }

  def scalaTypeName(name: String, baseTypes: Set[String]): String =
    if (baseTypes.contains(name) || name == "bool") s"FitBaseType"
    else s"${snakeCamelType(name)}"

  def scalaTypeCodec(name: String, baseType: String, baseTypes: Set[String]): String = {
    val tn = snakeCamelType(name)
    if (name == "string")
      s"fieldDef => _ => $stringBaseValue.codec(fieldDef.sizeBytes)"
    else if (name == "float32" || name == "float64")
      s"_ => bo => $floatBaseValue.codec(bo, $baseType)"
    else if (baseTypes.contains(name) || name == "bool")
      s"_ => bo => $longBaseValue.codec(bo, $baseType)"
    else s"_ => bo => $tn.codec(bo)"
  }

  implicit class StringOps(self: String) {
    def inQuotes = s""""$self""""
  }
}
