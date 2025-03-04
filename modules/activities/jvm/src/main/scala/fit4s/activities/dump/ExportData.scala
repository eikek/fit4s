package fit4s.activities.dump

import cats.effect.*
import cats.syntax.all.*
import cats.{Applicative, Apply}
import fs2.{Chunk, Pipe, Stream}

import fit4s.activities.JdbcConfig
import fit4s.activities.data.Location
import fit4s.activities.dump.ExportData.ProgressObserve
import fit4s.activities.records.*
import com.github.eikek.borer.compats.fs2.StreamDecode

import doobie.*
import doobie.syntax.all.*
import io.bullet.borer.*

trait ExportData[F[_]]:

  def dump(progress: ProgressObserve[F]): Stream[F, Byte]

  def read(dryRun: Boolean, progress: ProgressObserve[F]): Pipe[F, Byte, Nothing]

object ExportData:
  trait ProgressObserve[F[_]]:
    def onProcess(value: DumpFormat): F[Unit]
    def andThen(next: ProgressObserve[F])(implicit F: Apply[F]): ProgressObserve[F] =
      ProgressObserve(v => onProcess(v) *> next.onProcess(v))
    def >>(next: ProgressObserve[F])(implicit F: Apply[F]): ProgressObserve[F] = andThen(
      next
    )

  object ProgressObserve:
    def apply[F[_]](f: DumpFormat => F[Unit]): ProgressObserve[F] =
      new ProgressObserve[F]:
        def onProcess(value: DumpFormat) = f(value)
    def none[F[_]: Applicative]: ProgressObserve[F] = apply(_ => ().pure[F])
    def count[F[_]](counters: Ref[F, Map[Class[?], Int]]): ProgressObserve[F] =
      ProgressObserve(v =>
        counters.update(m => m.updatedWith(v.getClass)(_.map(_ + 1).orElse(Some(1))))
      )

  def apply[F[_]: Sync](xa: Transactor[F], dbms: JdbcConfig.Dbms): ExportData[F] =
    new Impl[F](xa, dbms)

  private class Impl[F[_]: Sync](xa: Transactor[F], dbms: JdbcConfig.Dbms)
      extends ExportData[F]:

    private def encode(in: Stream[F, DumpFormat]): Stream[F, Byte] =
      in.flatMap { a =>
        Json
          .encode(a)
          .toByteArrayEither
          .fold(Stream.raiseError, ba => Stream.chunk(Chunk.array(ba)))
      }

    private def decode(in: Stream[F, Byte]): Stream[F, DumpFormat] =
      StreamDecode.decodeJson[F, DumpFormat](in)

    override def dump(progress: ProgressObserve[F]): Stream[F, Byte] =
      (
        RTag.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          RActivityLocation.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          RActivity.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          RActivitySession.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          RActivityLap.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          RActivitySessionData.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          RActivityTag.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          RActivityStrava.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          RGeoPlace.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          RActivityGeoPlace.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply) ++
          Stream.eval(RStravaToken.deleteExpired.transact(xa)).drain ++
          RStravaToken.streamAll.transactNoPrefetch(xa).map(DumpFormat.apply)
      ).evalTap(progress.onProcess).through(encode)

    override def read(
        dryRun: Boolean,
        progress: ProgressObserve[F]
    ): Pipe[F, Byte, Nothing] =
      _.through(decode)
        .evalTap(progress.onProcess)
        .through(if (dryRun) _.drain else databaseSink) ++
        Stream.eval(RestartSequences(dbms).transact(xa)).drain

    private def databaseSink(in: Stream[F, DumpFormat]): Stream[F, Nothing] =
      in.chunkN(50, allowFewer = true).evalMap(storeData).drain

    private def storeData(ch: Chunk[DumpFormat]): F[Int] =
      val p = ch.foldLeft(DumpProduct.empty)(_.add(_))
      p.insertStatement.transact(xa)
