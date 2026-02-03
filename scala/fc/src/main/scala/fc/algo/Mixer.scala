package fc.algo

import core.device.esc.ESC.{ Command, Run }
import core.device.rc.RcInput

trait Mixer {
  def run(throttle: RcInput, pitchIn: Double, rollIn: Double, yawIn: Double): (Command, Command, Command, Command)
}

case class BasicMixer() extends Mixer {
  def run(throttleIn: RcInput, pitchIn: Double, rollIn: Double, yawIn: Double): (Command, Command, Command, Command) = {
    val throttleGain = 1.0
    val throttle = throttleIn.fromZero * throttleGain

    val pitchGain = 0.3
    // pitch is low for pitch down, high for pitch up
    val pitchAdjustment = pitchIn * pitchGain

    val rollGain = 0.3
    // roll is low for roll left, high for roll right
    val rollAdjustment = rollIn * rollGain

    val yawGain = 0.3
    // yaw is low for yaw left, high for yaw right
    val yawAdjustment = yawIn * yawGain

    val motorLF = throttle + pitchAdjustment + rollAdjustment - yawAdjustment
    val motorRF = throttle + pitchAdjustment - rollAdjustment + yawAdjustment
    val motorLR = throttle - pitchAdjustment + rollAdjustment + yawAdjustment
    val motorRR = throttle - pitchAdjustment - rollAdjustment - yawAdjustment

    (Run(motorLF), Run(motorRF), Run(motorLR), Run(motorRR))
  }
}
