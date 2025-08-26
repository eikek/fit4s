package fit4s.core.data

trait Display[A]:
  def show(v: A): String

object Display:
  inline def apply[A](using s: Display[A]): Display[A] = s

  def instance[A](f: A => String): Display[A] =
    (a: A) => f(a)

  given Display[String] = instance(identity)

  object syntax:
    extension [A](self: A)(using d: Display[A]) def display: String = d.show(self)
