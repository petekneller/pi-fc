package core.device.gps

case class ExampleMessage[M <: Message](name: String, msg: M, bytes: Seq[Byte])
