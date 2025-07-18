package fit4s.build

import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}

import scala.jdk.CollectionConverters._
import model._
import org.apache.poi.ss.usermodel.{Cell, CellType, Row}
import sbt.File
import sbt.util.Logger

import scala.util.{Try, Using}

object ProfileReader {

  def readFile(xlsx: File)(implicit logger: Logger) = this.synchronized {
    val wb = new XSSFWorkbook(xlsx)
    val sheetCount = wb.getNumberOfSheets
    logger.info(s"Found $sheetCount sheets in the xlsx file")
    if (sheetCount != 2) sys.error(s"Unexpected sheet count $sheetCount")
    val types = wb.getSheetAt(0)
    val messages = wb.getSheetAt(1)

    (readTypes(types), readMessages(messages))
  }

  def readMessages(sheet: XSSFSheet): List[MessageDef] = {
    // skip header
    val rows = sheet.asScala.drop(1).toList.map(_.messagesFormat)
    val first = rows.head

    val initialMessageName =
      first.messageName.getOrElse(sys.error("Expected message name in first row!"))

    def go(
        mn: String,
        rest: List[MessagesFormat],
        result: List[MessageFieldLine]
    ): List[MessageFieldLine] =
      rest match {
        case Nil    => result
        case a :: m =>
          a.messageName match {
            case Some(mn_) => go(mn_, m, result)
            case None      =>
              a.fieldDefNum match {
                case None =>
                  // sub field belonging to the previous field
                  val previous = result.head
                  val subfieldDef =
                    MessageSubFieldLine(
                      fieldName = a.fieldName.getOrElse(
                        sys.error(s"Expected field name in row ${a.self.getRowNum}")
                      ),
                      fieldType = a.fieldType.getOrElse(
                        sys.error(s"Expected field type in row ${a.self.getRowNum}")
                      ),
                      isArray = a.arrayDef,
                      components = a.components,
                      scale = a.scale,
                      offset = a.offset,
                      units = a.units,
                      bits = a.bits,
                      accumulate = a.accumulate,
                      refFieldName = a.refFieldName
                        .map(_.split(',').map(_.trim).filter(_.nonEmpty).toList)
                        .getOrElse(Nil),
                      refFieldValue = a.refFieldValue
                        .map(_.split(',').map(_.trim).filter(_.nonEmpty).toList)
                        .getOrElse(Nil),
                      comment = a.comment
                    )
                  go(mn, m, previous.addSubfield(subfieldDef) :: result.tail)

                case Some(fieldDefNum) =>
                  val messageDef =
                    MessageFieldLine(
                      messageName = mn,
                      fieldDefNumber = fieldDefNum,
                      fieldName = a.fieldName.getOrElse(
                        sys.error(s"Expected field name in row ${a.self.getRowNum}")
                      ),
                      fieldType = a.fieldType.getOrElse(
                        sys.error(s"Expected field type in row ${a.self.getRowNum}")
                      ),
                      isArray = a.arrayDef,
                      components = a.components,
                      scale = a.scale,
                      offset = a.offset,
                      units = a.units,
                      bits = a.bits,
                      accumulate = a.accumulate,
                      comment = a.comment,
                      products = a.products,
                      example = a.example,
                      subFields = Nil
                    )
                  go(mn, m, messageDef :: result)
              }

          }
      }

    go(initialMessageName, rows.tail.filterNot(_.isMessagesTitle), Nil).reverse
      .groupBy(_.messageName)
      .map(MessageDef.tupled)
      .toList
  }

  def readTypes(sheet: XSSFSheet): List[TypeDesc] = {
    // skip header
    val rows = sheet.asScala.drop(1).toList.map(_.typesFormat)
    val first = rows.head

    val initialTypeName =
      first.getTypeName.getOrElse(sys.error("Expected type name in first row!"))
    val initialBaseType =
      first.getBaseType.getOrElse(sys.error("Expected base type in first row!"))

    @annotation.tailrec
    def go(
        tn: String,
        bt: String,
        rest: List[TypesFormat],
        result: List[TypeDesc]
    ): List[TypeDesc] =
      rest match {
        case Nil    => result
        case a :: m =>
          (a.getTypeName, a.getBaseType) match {
            case (Some(tn_), Some(bt_)) =>
              if (m.headOption.exists(_.getTypeName.isDefined))
                go(
                  tn_,
                  bt_,
                  m,
                  TypeDesc(
                    tn_,
                    bt_,
                    a.getValueName.getOrElse(""),
                    a.getValueNumber.getOrElse(-1),
                    a.getComment
                  ) :: result
                )
              else go(tn_, bt_, m, result)
            case _ =>
              val vn = a.getValueName.getOrElse(
                sys.error(s"No value name in row: ${a.self.getRowNum}")
              )
              val v = a.getValueNumber.getOrElse(
                sys.error(s"No value number in row: ${a.self.getRowNum}")
              )
              go(tn, bt, m, TypeDesc(tn, bt, vn, v, a.getComment) :: result)
          }
      }

    go(initialTypeName, initialBaseType, rows.tail, Nil).reverse
  }

  implicit private class RowOps(self: Row) {
    def getCells = self.asScala.toList

    def typesFormat = new TypesFormat(self)

    def messagesFormat = new MessagesFormat(self)

    def stringValue(index: Int): Option[String] =
      Option(self.getCell(index).getStringCellValue).map(_.trim).filter(_.nonEmpty)

    def anyValue(index: Int): Option[String] = {
      val c = self.getCell(index)
      c.getCellType match {
        case CellType.STRING =>
          Option(c.getStringCellValue).map(_.trim).filter(_.nonEmpty)

        case CellType.NUMERIC => Option(c.getNumericCellValue).map(_.toString)

        case CellType.BLANK   => None
        case CellType._NONE   => None
        case CellType.BOOLEAN => Option(c.getBooleanCellValue).map(_.toString)
        case CellType.ERROR   => Option(c.getErrorCellValue).map(_.toString)
        case CellType.FORMULA => Option(c.getCellFormula)
        case _                => None
      }
    }

    def doubleValue(index: Int): Option[Double] = {
      val c = self.getCell(index)
      c.getCellType match {
        case CellType.NUMERIC =>
          Option(c.getNumericCellValue)

        case CellType.STRING =>
          Option(c.getStringCellValue)
            .map { s =>
              if (s.contains('.') && s.contains(',')) s.replaceAll(",", "")
              else s.replace(',', '.')
            }
            .filter(_.trim.nonEmpty)
            .map(s =>
              Try(s.toDouble).toEither.left
                .map(ex => s"Invalid number in ${self.getRowNum}/$index: $s")
                .fold(sys.error, identity)
            )

        case ct =>
          sys.error(
            s"Unexpected cell type '$ct' in row ${self.getRowNum}. Expected NUMERIC."
          )
      }
    }

    def intValue(index: Int): Option[Int] = {
      val c = self.getCell(index)
      c.getCellType match {
        case CellType.NUMERIC =>
          Option(c.getNumericCellValue).map(_.toInt)

        case CellType.STRING =>
          Option(c.getStringCellValue)
            .flatMap {
              case s if s.startsWith("0x") =>
                Try(BigInt(s.drop(2), 16)).toOption.map(_.toInt)

              case s if s.startsWith("x") =>
                Try(BigInt(s.drop(1), 16)).toOption.map(_.toInt)

              case s =>
                Try(BigInt(s.drop(1))).toOption.map(_.toInt)
            }

        case ct =>
          sys.error(
            s"Unexpected cell type '$ct' in row ${self.getRowNum}. Expected NUMERIC/STRING."
          )
      }
    }

    def intListValue(index: Int): List[Int] =
      anyValue(index)
        .map(
          _.split(',')
            .map(_.toDouble.toInt)
            .toList
        )
        .getOrElse(Nil)

    def doubleListValue(index: Int): List[Double] =
      anyValue(index).map(_.split(',').map(_.toDouble).toList).getOrElse(Nil)

    def stringListValue(index: Int): List[String] =
      anyValue(index).map(_.split(',').toList).getOrElse(Nil)
  }

  final class TypesFormat(val self: Row) {
    def getTypeName: Option[String] = self.stringValue(0)

    def getBaseType: Option[String] = self.stringValue(1)

    def getValueName: Option[String] = self.stringValue(2)

    def getValueNumber: Option[Int] = self.intValue(3)

    def getComment: Option[String] = self.stringValue(4)
  }

  final class MessagesFormat(val self: Row) {
    def isMessagesTitle =
      self.stringValue(3).exists(_.endsWith("MESSAGES"))

    def messageName: Option[String] = self.stringValue(0)
    def fieldDefNum: Option[Int] = self.intValue(1)
    def fieldName: Option[String] = self.stringValue(2)
    def fieldType: Option[String] = self.stringValue(3)
    def arrayDef: ArrayDef =
      self
        .stringValue(4)
        .map(ArrayDef.fromString)
        .map(_.fold(sys.error, identity))
        .getOrElse(ArrayDef.NoArray)

    def components = self.stringListValue(5)
    def scale = self.doubleListValue(6)
    def offset = self.doubleValue(7)
    def units = self.stringValue(8)
    def bits = self.intListValue(9)
    def accumulate = self.intListValue(10)
    def refFieldName = self.stringValue(11)
    def refFieldValue = self.stringValue(12)
    def comment = self.stringValue(13)
    def products = self.stringValue(14)
    def example = self.anyValue(15)
  }
}
