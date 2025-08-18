package fit4s.core

import java.nio.charset.StandardCharsets
import java.nio.file.*

import scodec.bits.ByteVector

object FileUtil:

  def init(file: String, content: String): Unit =
    val path = Paths.get(file)
    Files.deleteIfExists(path)
    Files.writeString(path, content, StandardOpenOption.CREATE)

  def append(file: String, content: String): Unit =
    val path = Paths.get(file)
    Files.writeString(path, content, StandardOpenOption.APPEND)

  def truncate(file: String, n: Long): Unit =
    val path = Paths.get(file)
    val ch = Files.newByteChannel(path, StandardOpenOption.WRITE)
    ch.truncate(ch.size() - n)
    ch.close()

  def writeAll(file: String, content: ByteVector): Unit =
    val path = Paths.get(file)
    Files.write(
      path,
      content.toArray,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE
    )

  def writeAll(file: String, content: String): Unit =
    writeAll(file, ByteVector.view(content.getBytes(StandardCharsets.UTF_8)))

  def writeJsArray[A](file: String, data: Iterable[A])(f: A => String): Unit =
    init(file, "[\n")
    data.foreach(line => append(file, s"${f(line)},\n"))
    truncate(file, 2)
    append(file, "]")
