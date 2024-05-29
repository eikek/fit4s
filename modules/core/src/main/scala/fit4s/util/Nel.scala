package fit4s.util

final case class Nel[+A](head: A, tail: List[A]):
  lazy val toList: List[A] = head :: tail

  def map[B](f: A => B): Nel[B] =
    Nel(f(head), tail.map(f))

object Nel:
  def of[A](head: A, tail: A*): Nel[A] =
    Nel(head, tail.toList)

  def fromList[A](list: List[A]): Option[Nel[A]] =
    list match
      case Nil    => None
      case h :: t => Some(Nel(h, t))

  def unsafeFromList[A](list: List[A]): Nel[A] =
    Nel(list.head, list.tail)
