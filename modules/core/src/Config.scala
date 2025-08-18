package fit4s.core

final case class Config(
    expandComponents: Boolean = true,
    expandSubFields: Boolean = true,
    expandDeveloperFields: Boolean = true,
    checkCrc: Boolean = true
)
