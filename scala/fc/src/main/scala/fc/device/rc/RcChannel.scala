package fc.device.rc

import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import fc.device.api._
import fc.device.controller.filesystem._

trait RcChannel extends HalfDuplexDevice {
  type Ctrl = FileSystemController
  val rx: NumericRx[RcInput]
}

object RcChannel {
  def apply(channel: Int, config: RcChannelConfig = RcChannelConfig.default)(implicit c: FileSystemController): RcChannel = new RcChannel {
    val address = RcAddress(channel)
    implicit val controller: FileSystemController = c

    val rx = NumericRx[RcInput](register, { l => RcInput.fromPpm(l.toInt, config.min, config.max, config.mid) })
  }

  val register = FileSystemRegister.singleton
}

case class RcChannelConfig(min: PpmValue, max: PpmValue, mid: PpmValue)

object RcChannelConfig {
  val default = RcChannelConfig(
    min = 1000,
    max = 2000,
    mid = 1500
  )
}
