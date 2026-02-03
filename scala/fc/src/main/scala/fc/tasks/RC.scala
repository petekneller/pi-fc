package fc.tasks

import cats.effect.IO
import _root_.fs2.Stream
import core.device.api.DeviceResult
import core.device.rc.{ RcInput, RcChannel }

object RC {

  def readChannel(channel: RcChannel): Stream[IO, DeviceResult[RcInput]] = Stream.eval(IO.delay{ channel.read(channel.rx) }).repeat

  def formatRcChannels(one: RcInput, two: RcInput, three: RcInput, four: RcInput, six: RcInput):String = {
    val fmt = "CH %d: [%4d]"
    (fmt.format(1, one.ppm) :: fmt.format(2, two.ppm) :: fmt.format(3, three.ppm) :: fmt.format(4, four.ppm) :: fmt.format(6, six.ppm) :: Nil).mkString(" | ")
  }

}
