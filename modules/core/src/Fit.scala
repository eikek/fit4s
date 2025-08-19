package fit4s.core

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

import fit4s.codec.FitFile
import fit4s.core.data.LatLng
import fit4s.core.data.Polyline
import fit4s.core.data.Position
import fit4s.profile.FileIdMsg
import fit4s.profile.GlobalMessages
import fit4s.profile.RecordMsg

import scodec.Attempt
import scodec.bits.ByteVector

final class Fit(val file: FitFile, val cfg: Config):

  /** Gets the file-id message that is recommended to be in every fit file. */
  def fileId: Option[FileId] =
    getMessages(FileIdMsg).headOption.flatMap(_.as[FileId].toOption.flatten)

  /** Constructs a polyline from the position data in all record messages. */
  def track(using Polyline.Config): Either[String, Polyline] =
    getLatLngs.map(Polyline.apply(_*))

  def getLatLngs =
    val reader = Position.reader(RecordMsg.positionLat, RecordMsg.positionLong)
    val init: Either[String, Vector[LatLng]] = Right(Vector.empty)
    getMessages(RecordMsg)
      .foldLeft(init) { (res, m) =>
        res.flatMap(vs =>
          reader.read(m).map(_.map(v => vs.appended(v.toLatLng)).getOrElse(vs))
        )
      }

  def getMessages[N](n: N)(using g: GetMesgNum[N]): LazyList[FitMessage] =
    val num = g.get(n)
    GlobalMessages.values.get(num) match
      case None         => LazyList.empty
      case Some(schema) =>
        LazyList
          .from(file.groupByMsg.getOrElse(num, Vector.empty))
          .map(dr =>
            FitMessage(
              dr,
              schema,
              cfg.expandSubFields,
              cfg.expandComponents,
              cfg.expandDeveloperFields
            )
          )

object Fit:

  def read(bv: ByteVector, cfg: Config = Config()): Attempt[Vector[Fit]] =
    FitFile.read(bv, cfg.checkCrc).map(_.map(Fit(_, cfg)))

  def fromInputStream(is: InputStream, cfg: Config = Config()): Attempt[Vector[Fit]] =
    read(ByteVector.view(is.readAllBytes()), cfg)

  def fromNIO(file: Path, cfg: Config = Config()): Attempt[Vector[Fit]] =
    read(ByteVector.view(Files.readAllBytes(file)), cfg)

  def fromFile(file: java.io.File, cfg: Config = Config()): Attempt[Vector[Fit]] =
    fromNIO(file.toPath(), cfg)
