package util.spitotcp

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.auto.{ autoRefineV, autoUnwrap }

object OfBytes {
  type Port = Int Refined Interval.Closed[W.`1`.T, W.`65535`.T]

  def apply(port: Port): Unit = {
    v7.SpiToTcp.main(Array(port.toString))
  }
}
