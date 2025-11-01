package fit4s.core

import java.time.Instant

import fit4s.codec.*
import fit4s.core.data.*
import fit4s.core.data.Polyline.Precision
import fit4s.profile.*

import munit.FunSuite

class ExperimentTest extends FunSuite with TestSyntax:
  val cfg = Polyline.Config()

  test("find stuff".ignore):
    for
      file <- List(TestData.Activities.fr70Intervals)
      fit <- Fit.read(file.contents).require
      rec <- fit.getMessages(RecordMsg)
      _ = println(s"${rec.timestamp.map(_.asInstant)}")
    yield ()

  test("print polyline".ignore):
    val data = TestData.Activities.edge1536
    val fit = Fit.read(data.contents).require.head
    println(fit.getMessages(FileIdMsg).head.as[FileId])
    given cfg: Polyline.Config = Polyline.Config(precision = Precision.high)

    val (track, positions) =
      fit.getMessages(RecordMsg).foldLeft((Polyline.empty(cfg), Vector.empty[LatLng])) {
        case ((line, vec), rec) =>
          val p = Position
            .reader(
              RecordMsg.positionLat,
              RecordMsg.positionLong
            )
            .read(rec)
            .value
          (line.add(p.toLatLng), vec.appended(p.toLatLng))
      }

    val toRender = positions
    FileUtil.writeJsArray("track.coord", toRender)(p => s"[ ${p.lat}, ${p.lng} ]")
    FileUtil.writeAll("track-poly", track.asBytes)

  test("find messages with expansion fields".ignore):
    val msgsWithExpansion = GlobalMessages.values.values.filter(
      _.allFields.values.exists(_.components.nonEmpty)
    )
    TestData.Activities.all.map { file =>
      val lines =
        for {
          (fit, idx) <- Fit.read(file.contents).require.zipWithIndex
          msgSchema <- msgsWithExpansion
          fields = msgSchema.allFields.values.filter(_.components.nonEmpty).toList
          field <- fields
          msg <- fit.getMessages(msgSchema)
          df <- msg.field(field).toList
        } yield s"${file.name} ($idx): ${msgSchema} / ${df.fieldName}"
      lines.toSet.foreach(println)
    }

  test("get file ids".ignore):
    TestData.allFiles.foreach { file =>
      for {
        fit <- Fit.read(file.contents).require
        msg <- fit.getMessages(FileIdMsg)
        id = msg.as[FileId]
      } println(id)
    }

  test("fit api".ignore):
    val data = TestData.Activities.edge1536
    val fit = Fit.read(data.contents).require.head
    val fileId = fit.getMessages(FileIdMsg).head

    println(fileId.as[FileId])

    val sess = fit.getMessages(SessionMsg).head
    val start = Position.reader(SessionMsg.startPositionLat, SessionMsg.startPositionLong)
    println(start.read(sess))
    println(s"read fileid from session message: ${sess.as[FileId]}")

  test("fit message api".ignore):
    val data = TestData.Activities.edge1536
    val fit = Fit.read(data.contents).require.head
    val fm = fit.getMessages(SessionMsg).head

    println(fm.field(SessionMsg.enhancedAvgSpeed).map(_.as[Speed]))
    println(fm.field(SessionMsg.totalDistance).map(_.as[Distance]))

    val fileId = fit.getMessages(FileIdMsg).head
    val manu = fileId.field(FileIdMsg.manufacturer).map(_.asEnum)
    val ft = fileId.field(FileIdMsg.`type`).map(_.asEnum)
    val serial = fileId.field(FileIdMsg.serialNumber).map(_.as[Long])
    val created = fileId.field(FileIdMsg.timeCreated).map(_.as[Instant])
    println(manu)
    println(ft)
    println(serial)
    println(created)

  test("api exploration".ignore) {
    val data = TestData.Activities.fr935run
    val fit = FitFile.read(data.contents).require.head

    val fileId = fit.findMessages(MesgNumType.fileId).head
    // val manu = fileId.fieldData(FileIdMsg.manufacturer.fieldDefNum).get.flatMap {
    //   case n: Int => ManufacturerType(n)
    //   case _      => None
    // }
    val manu = fieldValue(fileId, FileIdMsg.manufacturer)
    val fileType = fieldValue(fileId, FileIdMsg.`type`)
    val num = fieldValue(fileId, FileIdMsg.number)
    // val prod = fileId.fieldData(FileIdMsg.product.fieldDefNum).get.flatMap { v =>
    //   FileIdMsg.product.subFields match
    //     case Nil =>
    // }

    val record = fit.findMessages(RecordMsg.globalNum).head
    val pos = fieldValue(record, RecordMsg.positionLat)
    val time = fieldValue(record, CommonMsg.timestamp)
    val prod = fieldValue(fileId, FileIdMsg.product)
    println(manu)
    println(fileType)
    println(num)
    println(pos)
    println(time)
    println(prod)
  }

  def fieldValue(dm: DataRecord, field: MsgField): Vector[FitBaseValue] =
    dm.findFieldByNum(field.fieldDefNum) match
      case None     => Vector.empty
      case Some(df) =>
        field.subFields match
          case Nil =>
            field.profileType match
              case Some(profileType) =>
                df.data.headOption.flatMap {
                  case n: Int =>
                    profileType(n).orElse(df.data.headOption)
                  case n: Byte =>
                    profileType(n.toInt).orElse(df.data.headOption)
                  case v =>
                    Some(v)
                }.toVector
              case None =>
                df.data

          case subs =>
            subs
              .find(subFieldApplies(dm))
              .map(field.merge)
              .map(fieldValue(dm, _))
              .getOrElse(df.data)

  def subFieldApplies(dm: DataRecord)(sf: SubField): Boolean =
    sf.references.exists(ref =>
      dm.fieldData(ref.refField.fieldDefNum).exists(_ == Vector(ref.refFieldValue))
    )
