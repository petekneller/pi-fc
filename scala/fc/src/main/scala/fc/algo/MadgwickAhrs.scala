package fc.algo

case class MadgwickAhrs(beta: Double = 0.1) extends Ahrs {

  private val deg2rad = math.Pi / 180.0

  def update(state: AhrsState, gyro: (Double, Double, Double), accel: (Double, Double, Double), dt: Double): AhrsState = {
    val (q0, q1, q2, q3) = (state.q0, state.q1, state.q2, state.q3)
    val (gx, gy, gz) = (gyro._1 * deg2rad, gyro._2 * deg2rad, gyro._3 * deg2rad)
    val (ax, ay, az) = accel

    // Quaternion derivative from gyro
    var qDot0 = 0.5 * (-q1 * gx - q2 * gy - q3 * gz)
    var qDot1 = 0.5 * ( q0 * gx + q2 * gz - q3 * gy)
    var qDot2 = 0.5 * ( q0 * gy - q1 * gz + q3 * gx)
    var qDot3 = 0.5 * ( q0 * gz + q1 * gy - q2 * gx)

    val aNorm = math.sqrt(ax * ax + ay * ay + az * az)

    if (aNorm > 1e-6) {
      val nax = ax / aNorm
      val nay = ay / aNorm
      val naz = az / aNorm

      // Objective function: f = R(q) * [0,0,1] - [ax,ay,az]
      val f0 = 2.0 * (q1 * q3 - q0 * q2) - nax
      val f1 = 2.0 * (q0 * q1 + q2 * q3) - nay
      val f2 = 2.0 * (0.5 - q1 * q1 - q2 * q2) - naz

      // Gradient: J^T * f
      val s0 = -2.0 * q2 * f0 + 2.0 * q1 * f1                  // not contributing f2 to q0 via simplified Jacobian
      val s1 =  2.0 * q3 * f0 + 2.0 * q0 * f1 - 4.0 * q1 * f2
      val s2 = -2.0 * q0 * f0 + 2.0 * q3 * f1 - 4.0 * q2 * f2
      val s3 =  2.0 * q1 * f0 + 2.0 * q2 * f1

      // Normalize gradient
      val sNorm = math.sqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3)
      if (sNorm > 1e-12) {
        qDot0 -= beta * (s0 / sNorm)
        qDot1 -= beta * (s1 / sNorm)
        qDot2 -= beta * (s2 / sNorm)
        qDot3 -= beta * (s3 / sNorm)
      }
    }

    // Integrate
    val nq0 = q0 + qDot0 * dt
    val nq1 = q1 + qDot1 * dt
    val nq2 = q2 + qDot2 * dt
    val nq3 = q3 + qDot3 * dt

    // Normalize quaternion
    val qNorm = math.sqrt(nq0 * nq0 + nq1 * nq1 + nq2 * nq2 + nq3 * nq3)
    AhrsState(nq0 / qNorm, nq1 / qNorm, nq2 / qNorm, nq3 / qNorm, 0.0, 0.0, 0.0)
  }
}
