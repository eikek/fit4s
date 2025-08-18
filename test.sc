val chars = for (n <- 33 to 126) yield n
def manyChars: LazyList[Int] = LazyList.from(chars) #::: manyChars

def testPlus(n: Int) = {
  val time = System.nanoTime()
  var res = ""
  for (c <- manyChars.take(n)) {
    res = res + c.toChar
  }
  val diff = System.nanoTime() - time
  println(res.take(2))
  println(s"Plus: ${diff / 1_000_000}")
}

def testBuffer(n: Int) = {
  val time = System.nanoTime()
  val buf = new StringBuilder()
  for (c <- manyChars.take(n)) {
    buf.append(c.toChar)
  }
  val res = buf.toString
  val diff = System.nanoTime() - time
  println(res.take(2))
  println(s"Buffer: ${diff / 1_000_000}")
}

for (n <- 1 to 1000) {
testPlus(50000)
testBuffer(50000)
}
