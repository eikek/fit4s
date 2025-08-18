package fit4s.core

import java.time.Instant

import fit4s.codec.FitBaseValue
import fit4s.core.data.DateTime
import fit4s.profile.MeasurementUnit

import scodec.bits.ByteVector

trait FieldReader[T]:
  def read(field: FieldValue): Either[String, T]

  def map[B](f: T => B): FieldReader[B] = FieldReader.instance { v =>
    read(v).map(f)
  }

  def emap[B](f: T => Either[String, B]): FieldReader[B] = FieldReader.instance { v =>
    read(v).flatMap(f)
  }

  def flatMap[B](f: T => FieldReader[B]): FieldReader[B] = FieldReader.instance { v =>
    read(v) match
      case Right(t)      => f(t).read(v)
      case err @ Left(_) => err.asInstanceOf[Either[String, B]]
  }

object FieldReader:
  inline def apply[T](using r: FieldReader[T]): FieldReader[T] = r

  def instance[T](f: FieldValue => Either[String, T]): FieldReader[T] =
    new FieldReader[T] {
      def read(v: FieldValue): Either[String, T] = f(v)
    }

  def pure[A](a: A): FieldReader[A] = instance(_ => Right(a))

  def unit(u: MeasurementUnit, more: MeasurementUnit*): FieldReader[MeasurementUnit] =
    val set = more.toSet + u
    instance(v => v.unit.filter(set.contains).toRight(s"Expected unit $u, got ${v.unit}"))

  given identity: FieldReader[Vector[FitBaseValue]] =
    instance(fv => Right(fv.data))

  val firstValue: FieldReader[FitBaseValue] =
    instance(v => v.data.headOption.toRight(s"Expected existing value, got none"))

  private def foldConv[A](
      typeName: String,
      ns: Vector[FitBaseValue],
      conv: FitBaseValue => Option[A]
  ): Either[String, Vector[A]] =
    val init: Either[String, Vector[A]] = Right(Vector.empty)
    ns.foldLeft(init) { (res, v) =>
      res.flatMap(vs =>
        conv(v).map(vs :+ _).toRight(s"Expected $typeName value, got: $v")
      )
    }

  given bytes: FieldReader[Vector[Byte]] =
    instance(fv => foldConv("byte", fv.data, FitBaseValue.toByte))

  given byteVector: FieldReader[ByteVector] =
    bytes.map(ByteVector.apply)

  given ints: FieldReader[Vector[Int]] =
    instance(fv => foldConv("int", fv.data, FitBaseValue.toInt))

  given longs: FieldReader[Vector[Long]] =
    instance(fv => foldConv("long", fv.data, FitBaseValue.toLong))

  given doubles: FieldReader[Vector[Double]] =
    instance(fv => foldConv("double", fv.data, FitBaseValue.toDouble))

  given strings: FieldReader[Vector[String]] =
    instance(fv => foldConv("string", fv.data, FitBaseValue.toString))

  given firstAsDouble: FieldReader[Double] =
    doubles.emap(v => v.headOption.toRight(s"Expected number value"))

  given firstAsInt: FieldReader[Int] =
    ints.emap(v => v.headOption.toRight(s"Expected int value"))

  given firstAsLong: FieldReader[Long] =
    longs.emap(v => v.headOption.toRight(s"Expected long value"))

  given firstAsShort: FieldReader[Short] =
    firstAsInt.map(_.toShort)

  given firstAsByte: FieldReader[Byte] =
    bytes.emap(_.headOption.toRight("Expected byte value"))

  given FieldReader[Instant] = FieldReader[DateTime].map(_.asInstant)

  given FieldReader[String] =
    strings.emap(v => v.headOption.toRight(s"Expected string value, but got: $v"))
