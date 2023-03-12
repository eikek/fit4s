package fit4s

final case class FitFile(
    header: FileHeader,
    records: List[Record],
    crc: Int
)

object FitFile {}
