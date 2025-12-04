package fit4s.core.internal

import fit4s.core.data.*

private[core] object PolylineUtils:

  def latngIterator(pl: Polyline, chunkSize: Int): Iterator[LatLng] =
    if pl.isEmpty then Iterator.empty
    else if pl.size <= chunkSize then pl.toLatLngs.iterator
    else
      new Iterator[LatLng] {
        private var chunk = pl.decodeN(chunkSize)
        private var iter = chunk.map(_._1.iterator)
        def hasNext: Boolean = iter match
          case None     => false
          case Some(it) =>
            if it.hasNext then true
            else
              chunk = chunk.flatMap(_._2.decodeN(chunkSize))
              iter = chunk.map(_._1.iterator)
              hasNext

        def next: LatLng =
          iter
            .map(_.next)
            .getOrElse(throw new NoSuchElementException("iterator exhausted"))
      }

  def distance(pl: Polyline, chunkSize: Int): Distance =
    if pl.isEmpty || pl.size == 1 then Distance.zero
    else
      val iter = latngIterator(pl, chunkSize)
      val init: (Distance, LatLng) = (Distance.zero, { iter.hasNext; iter.next })
      val (result, _) =
        iter.foldLeft(init) { case ((sum, prev), el) =>
          (sum + prev.distance(el), el)
        }
      result

  def approximity(pl1: Polyline, pl2: Polyline) =
    ???
