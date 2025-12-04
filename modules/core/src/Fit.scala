package fit4s.core

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

import fit4s.codec.FitFile
import fit4s.core.data.LatLng
import fit4s.core.data.Polyline
import fit4s.core.data.Timespan
import fit4s.core.internal.GpsTrack
import fit4s.profile.FileIdMsg
import fit4s.profile.GlobalMessages

import scodec.Attempt
import scodec.bits.ByteVector

final class Fit(val file: FitFile, val cfg: Config):

  /** Gets the file-id message that is recommended to be in every fit file. */
  def fileId: Option[FileId] =
    getMessages(FileIdMsg).headOption.flatMap(_.as[FileId].toOption.flatten)

  def fileTypeIs(ft: Int | String): Boolean =
    fileId.exists(_.fileType.isValue(ft))

  /** Constructs a polyline from the position data in all record messages that occured
    * within the given timespan.
    */
  def track(cfg: Polyline.Config, within: Timespan): Either[String, Polyline] =
    getLatLngs(within).map(Polyline.apply(cfg)(_*))

  def getLatLngs(within: Timespan): Either[String, Vector[LatLng]] =
    GpsTrack.fromFitByType(this, within)

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
