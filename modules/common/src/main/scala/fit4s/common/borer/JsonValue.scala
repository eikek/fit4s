package fit4s.common.borer

import io.bullet.borer.Encoder

enum JsonValue:
  case Null
  case Bool(v: Boolean)
  case Str(v: String)
  case Num(n: Long)

object JsonValue:
  def apply(n: Long): JsonValue = JsonValue.Num(n)
  def apply(s: String): JsonValue = JsonValue.Str(s)
  def apply(b: Boolean): JsonValue = JsonValue.Bool(b)

  given Encoder[JsonValue] =
    Encoder { (w, v) =>
      v match
        case JsonValue.Null    => w.writeNull()
        case JsonValue.Bool(b) => w.writeBoolean(b)
        case JsonValue.Str(s)  => w.writeString(s)
        case JsonValue.Num(n)  => w.writeLong(n)
    }
