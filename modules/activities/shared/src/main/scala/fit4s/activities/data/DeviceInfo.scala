package fit4s.activities.data

import cats.Show
import fit4s.data.DeviceProduct

enum DeviceInfo:
  case Product(p: DeviceProduct)
  case Name(name: String)

object DeviceInfo:
  def fromString(str: String): DeviceInfo =
    DeviceProduct.fromString(str).map(Product.apply).getOrElse(Name(str))

  given Show[DeviceInfo] = Show.show(_.name)

  extension (delegate: DeviceInfo)
    def name: String = delegate match
      case DeviceInfo.Name(n)    => n
      case DeviceInfo.Product(p) => p.name
