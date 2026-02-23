package fc.algo

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AhrsTest extends AnyFlatSpec with Matchers {

  val filters: Seq[(String, Ahrs)] = Seq(
    ("MahonyAhrs", MahonyAhrs()),
    ("MadgwickAhrs", MadgwickAhrs())
  )

  val dt = 0.002 // 500 Hz
  val levelAccel = (0.0, 0.0, 1.0)
  val zeroGyro = (0.0, 0.0, 0.0)

  for ((name, filter) <- filters) {

    name should "maintain near-zero roll/pitch with zero gyro and level accel" in {
      val state = (1 to 100).foldLeft(AhrsState.identity) { (s, _) =>
        filter.update(s, zeroGyro, levelAccel, dt)
      }
      val (roll, pitch, _) = state.attitude
      math.abs(roll) should be < 0.01
      math.abs(pitch) should be < 0.01
    }

    it should "converge to expected pitch angle with tilted accel" in {
      // Accel vector tilted ~30 degrees forward around Y axis
      val tiltAngle = math.toRadians(30.0)
      val tiltedAccel = (-math.sin(tiltAngle), 0.0, math.cos(tiltAngle))

      val state = (1 to 5000).foldLeft(AhrsState.identity) { (s, _) =>
        filter.update(s, zeroGyro, tiltedAccel, dt)
      }
      val (_, pitch, _) = state.attitude
      // Should converge to approximately 30 degrees (0.524 rad)
      math.abs(pitch - tiltAngle) should be < 0.05
    }

    it should "converge to expected roll angle with tilted accel" in {
      // Accel vector tilted ~20 degrees right around X axis
      val tiltAngle = math.toRadians(20.0)
      val tiltedAccel = (0.0, math.sin(tiltAngle), math.cos(tiltAngle))

      val state = (1 to 5000).foldLeft(AhrsState.identity) { (s, _) =>
        filter.update(s, zeroGyro, tiltedAccel, dt)
      }
      val (roll, _, _) = state.attitude
      math.abs(roll - tiltAngle) should be < 0.05
    }

    it should "not produce NaN with zero accel input" in {
      val state = (1 to 100).foldLeft(AhrsState.identity) { (s, _) =>
        filter.update(s, zeroGyro, (0.0, 0.0, 0.0), dt)
      }
      state.q0.isNaN shouldBe false
      state.q1.isNaN shouldBe false
      state.q2.isNaN shouldBe false
      state.q3.isNaN shouldBe false
      val (roll, pitch, yaw) = state.attitude
      roll.isNaN shouldBe false
      pitch.isNaN shouldBe false
      yaw.isNaN shouldBe false
    }

    it should "rotate quaternion with gyro-only input (zero accel)" in {
      // 90 deg/sec around Z axis for 0.1 seconds = 9 degrees of rotation
      val gyro = (0.0, 0.0, 90.0) // degrees/sec
      val steps = 50
      val state = (1 to steps).foldLeft(AhrsState.identity) { (s, _) =>
        filter.update(s, gyro, (0.0, 0.0, 0.0), dt)
      }
      val (_, _, yaw) = state.attitude
      // Should have rotated roughly 9 degrees (0.157 rad)
      math.abs(yaw) should be > 0.05
    }
  }
}
