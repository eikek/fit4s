package fit4s.codec

import java.io.InputStream
import java.net.URL

import fit4s.codec.ByteSource.IterableByteSource

import scodec.bits.ByteVector

/** Stateful object that can retrieve bytes from a source. */
trait ByteSource extends AutoCloseable:
  /** Retrieves the next chunk of bytes from the source. */
  def next: ByteVector

  /** Optional descriptive name for this byte source. */
  def name: Option[String] = None

  /** Concatenates `next` to this byte source. */
  def ++(next: ByteSource): ByteSource = (this, next) match
    case (asrc: ByteSource.IterableByteSource, bsrc: ByteSource.IterableByteSource) =>
      IterableByteSource(asrc.srcs ++ bsrc.srcs)
    case (asrc: ByteSource.IterableByteSource, bsrc) =>
      IterableByteSource(asrc.srcs :+ bsrc)
    case (asrc, bsrc: ByteSource.IterableByteSource) =>
      IterableByteSource(asrc +: bsrc.srcs)
    case (_, _) =>
      ByteSource.concat(Seq(this, next))

  /** Creates a new byte source attaching the given name. */
  def withName(name: String): ByteSource = this match
    case s: ByteSource.WithName => s.copy(_name = name)
    case s                      => ByteSource.WithName(s, name)

object ByteSource:
  private val defaultBufferSize = ByteSize.kibi(32)

  def constant(bv: ByteVector): ByteSource =
    new ByteSource {
      val next: ByteVector = bv
      def close(): Unit = ()
    }

  def fromInputStream(
      in: InputStream,
      bufferSize: ByteSize = defaultBufferSize,
      closeInputStream: Boolean = false
  ): ByteSource =
    new ByteSource {
      private val buffer = Array.fill(bufferSize.toBytes.toInt)(0.toByte)

      def next: ByteVector =
        in.read(buffer) match
          case -1 => close(); ByteVector.empty
          case n  => ByteVector.apply(buffer, 0, n)

      def close(): Unit =
        if (closeInputStream) in.close()
    }

  def fromNIO(
      file: java.nio.file.Path,
      bufferSize: ByteSize = defaultBufferSize
  ): ByteSource =
    fromInputStream(java.nio.file.Files.newInputStream(file), bufferSize, true)

  def fromURL(url: URL, bufferSize: ByteSize = defaultBufferSize): ByteSource =
    fromInputStream(url.openStream(), bufferSize, false)

  def concat(srcs: Iterable[ByteSource]): ByteSource =
    IterableByteSource(srcs.toVector)

  private class IterableByteSource(val srcs: Vector[ByteSource]) extends ByteSource {
    self =>
    val remain = srcs.iterator
    var current: Option[ByteSource] = None
    def next: ByteVector = current match
      case Some(s) =>
        val n = s.next
        if (n.isEmpty && remain.hasNext) {
          current = Some(remain.next())
          self.next
        } else n

      case None =>
        if (remain.hasNext) {
          current = Some(remain.next())
          self.next
        } else {
          ByteVector.empty
        }

    def close(): Unit = srcs.foreach(_.close())
    override def name: Option[String] = current.flatMap(_.name)
  }

  private case class WithName(src: ByteSource, _name: String) extends ByteSource {
    export src.*
    override def name: Option[String] = Some(_name).filter(_.nonEmpty)
  }
