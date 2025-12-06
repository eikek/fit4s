package fit4s.core

import java.time.Instant

import scala.Tuple.Concat
import scala.deriving.Mirror

import fit4s.core.data.Timespan
import fit4s.profile.LapMsg
import fit4s.profile.LengthMsg
import fit4s.profile.MesgNumType
import fit4s.profile.MsgField
import fit4s.profile.ProfileEnum
import fit4s.profile.SegmentLapMsg
import fit4s.profile.SessionMsg
import fit4s.profile.SplitMsg

trait MessageReader[A]:
  def read(fit: FitMessage): Either[String, Option[A]]

  def map[B](f: A => B): MessageReader[B] =
    new MessageReader.MapReader(this, f)

  def flatMap[B](f: A => MessageReader[B]): MessageReader[B] =
    new MessageReader.FlatmapReader(this, f)

  def subflatMap[B](f: A => Option[B]): MessageReader[B] =
    new MessageReader.SubflatmapReader(this, f)

  def emap[B](f: A => Either[String, B]): MessageReader[B] =
    new MessageReader.EmapReader(this, f)

  def tuple: MessageReader[A *: EmptyTuple] =
    map(a => a *: EmptyTuple)

  def transform[B](
      f: Either[String, Option[A]] => Either[String, Option[B]]
  ): MessageReader[B] =
    new MessageReader.TransformReader(this, f)

  def option: MessageReader[Option[A]] =
    transform(_.map(Some(_)))

  def withDefault(default: => A): MessageReader[A] =
    transform(_.map(_.orElse(Some(default))))

  def or(other: => MessageReader[A]): MessageReader[A] =
    MessageReader.instance { fm =>
      read(fm) match
        case Right(None) => other.read(fm)
        case v           => v
    }

  def recover(f: String => A): MessageReader[A] =
    transform {
      case v @ Right(_) => v
      case Left(err)    => Right(Some(f(err)))
    }

  def withDescription(message: String): MessageReader[A] =
    transform(_.left.map(m => s"$message: $m"))

object MessageReader:

  inline def apply[A](using r: MessageReader[A]): MessageReader[A] = r

  def instance[A](f: FitMessage => Either[String, Option[A]]): MessageReader[A] =
    new MessageReader[A] {
      def read(fit: FitMessage): Either[String, Option[A]] = f(fit)
    }

  def withDescription[A](message: String)(
      f: FitMessage => Either[String, Option[A]]
  ): MessageReader[A] =
    instance(f).withDescription(message)

  def option[A](f: FitMessage => Option[A]): MessageReader[A] =
    instance(fit => Right(f(fit)))

  def select[A](f: FitMessage => A): MessageReader[A] =
    instance(fit => Right(Some(f(fit))))

  def pure[A](a: A): MessageReader[A] =
    select(_ => a)

  def fromOption[A](a: Option[A]): MessageReader[A] =
    option(_ => a)

  def lift[A](v: Either[String, Option[A]]): MessageReader[A] =
    instance(_ => v)

  def fromEither[A](eab: Either[String, A]): MessageReader[A] =
    instance(_ => eab.map(Some(_)))

  def empty[A]: MessageReader[A] =
    option(_ => None)

  def get: MessageReader[FitMessage] =
    select(identity)

  def when(f: FitMessage => Boolean): MessageReader[Unit] =
    option(fit => if f(fit) then Some(()) else None)

  def when[M](m: M)(using g: GetMesgNum[M]): MessageReader[Unit] =
    when((fit: FitMessage) => fit.mesgNum == g.get(m))

  def field[N: GetFieldNumber](n: N): MessageReader[FieldValue] =
    option(_.field(n))

  def timestamp: MessageReader[Instant] =
    instance(m => Right(m.timestamp.map(_.asInstant)))

    /** Summary messages represent a time span, defined by the start time and total
      * elapsed time.
      *
      * https://developer.garmin.com/fit/cookbook/decoding-activity-files/#decodingfitactivityfiles
      *
      * Sometimes, this is not provided and instead the startTime and timestamp property
      * denote the time span.
      *
      * This applies only to LengthMsg, LapMsg, SessionMsg, SegmentLapMsg and SplitMsg
      */
  def timespan: MessageReader[Timespan] =
    withDescription("timespan") { fm =>
      val startAndtotalElapsedTime: Either[String, (MsgField, MsgField)] =
        fm.mesgNum match
          case MesgNumType.length =>
            Right(LengthMsg.startTime -> LengthMsg.totalElapsedTime)
          case MesgNumType.lap     => Right(LapMsg.startTime -> LapMsg.totalElapsedTime)
          case MesgNumType.session =>
            Right(SessionMsg.startTime -> SessionMsg.totalElapsedTime)
          case MesgNumType.segmentLap =>
            Right(SegmentLapMsg.startTime -> SegmentLapMsg.totalElapsedTime)
          case MesgNumType.split => Right(SplitMsg.startTime -> SplitMsg.totalElapsedTime)
          case _                 =>
            Left(s"Message ${fm.mesgNum} (${fm.messageName}) not supported for timespan")

      startAndtotalElapsedTime.flatMap { case (sf, ef) =>
        val startTime = fm.field(sf).as[Instant] match
          case Some(e) => e.map(Option(_))
          case None    => Right(None)
        val elapsed = fm.field(ef).as[Double] match
          case Some(e) => e.map(Option(_))
          case None    => Right(None)

        for
          stOpt <- startTime
          elapsedOpt <- elapsed
          result = (stOpt, fm.timestamp.map(_.asInstant), elapsedOpt) match
            case (Some(st), Some(et), None)                => Some(Timespan(st, et))
            case (Some(st), Some(et), _) if et.isAfter(st) => Some(Timespan(st, et))
            case (Some(st), _, Some(secs))                 =>
              Some(Timespan(st, st.plusSeconds(secs.toLong)))
            case _ => None
        yield result
      }
    }

  def forMsg[M: GetMesgNum, A](m: M)(reader: M => MessageReader[A]): MessageReader[A] =
    when(m).flatMap(_ => reader(m))

  def product[A, B](fa: MessageReader[A], fb: MessageReader[B]): MessageReader[(A, B)] =
    fa.flatMap(a => fb.map(b => (a, b)))
    // faster??
    // new MessageReader[(A, B)]:
    //   def read(fit: FitMessage): Either[String, Option[(A, B)]] =
    //     fa.read(fit) match
    //       case Right(Some(a)) =>
    //         fb.read(fit) match
    //           case Right(Some(b)) => Right(Some(a -> b))
    //           case Right(None)    => Right(None)
    //           case f @ Left(_)    => f.asInstanceOf[Either[String, Option[(A, B)]]]
    //       case Right(None) => Right(None)
    //       case f @ Left(_) => f.asInstanceOf[Either[String, Option[(A, B)]]]

  private class MapReader[A, B](r: MessageReader[A], f: A => B) extends MessageReader[B]:
    def read(fit: FitMessage): Either[String, Option[B]] =
      r.read(fit).map(_.map(f))
    override def map[C](g: B => C): MessageReader[C] =
      new MapReader(r, f.andThen(g))

  private class FlatmapReader[A, B](r: MessageReader[A], f: A => MessageReader[B])
      extends MessageReader[B]:
    def read(fit: FitMessage): Either[String, Option[B]] =
      r.read(fit).flatMap {
        case Some(a) => f(a).read(fit)
        case None    => Right(None)
      }

  private class SubflatmapReader[A, B](r: MessageReader[A], f: A => Option[B])
      extends MessageReader[B]:
    def read(fit: FitMessage): Either[String, Option[B]] =
      r.read(fit).map(_.flatMap(f))
    override def subflatMap[C](g: B => Option[C]): MessageReader[C] =
      new SubflatmapReader(r, f.andThen(_.flatMap(g)))

  private class EmapReader[A, B](r: MessageReader[A], f: A => Either[String, B])
      extends MessageReader[B]:
    def read(fit: FitMessage): Either[String, Option[B]] =
      r.read(fit).flatMap {
        case Some(a) => f(a).map(Some(_))
        case None    => Right(None)
      }
    override def emap[C](g: B => Either[String, C]): MessageReader[C] =
      new EmapReader(r, f.andThen(_.flatMap(g)))

  private class TransformReader[A, B](
      r: MessageReader[A],
      transform: Either[String, Option[A]] => Either[String, Option[B]]
  ) extends MessageReader[B]:
    def read(fit: FitMessage): Either[String, Option[B]] =
      transform(r.read(fit))
    override def transform[C](
        g: Either[String, Option[B]] => Either[String, Option[C]]
    ): MessageReader[C] =
      new TransformReader(r, transform.andThen(g))

  private class ConcatReader[A <: Tuple, B <: Tuple](
      a: MessageReader[A],
      b: MessageReader[B]
  ) extends MessageReader[Tuple.Concat[A, B]]:
    def read(fit: FitMessage): Either[String, Option[Concat[A, B]]] =
      product(a, b).read(fit).map(_.map((a: A, b: B) => a ++ b))

  private class ConsReader[A, B <: Tuple](a: MessageReader[A], b: MessageReader[B])
      extends MessageReader[A *: B]:
    def read(fit: FitMessage): Either[String, Option[A *: B]] =
      product(a, b).read(fit).map(_.map(_ *: _))

  extension (self: MessageReader[FieldValue])
    def as[R: FieldReader]: MessageReader[R] =
      self.emap(_.as[R])

    def asEnum: MessageReader[ProfileEnum] =
      self.subflatMap(_.asEnum)

    def asEnumStrict: MessageReader[ProfileEnum] =
      self.subflatMap(_.asEnumStrict)

  extension [X <: Product](self: MessageReader[X])
    def as[A](using
        mirror: Mirror.ProductOf[A] {
          type MirroredElemTypes = X
        }
    ): MessageReader[A] =
      self.map(mirror.fromProduct)

  extension [A, B <: Tuple](self: MessageReader[A])
    def ::(next: MessageReader[B]): MessageReader[A *: B] =
      new ConsReader(self, next)

  extension [A <: Tuple, B <: Tuple](self: MessageReader[A])
    def ++(next: MessageReader[B]): MessageReader[Tuple.Concat[A, B]] =
      new ConcatReader[A, B](self, next)
