package fc.algo

case class MahonyAhrs(kp: Double = 2.0, ki: Double = 0.1) extends Ahrs {

  private val deg2rad = math.Pi / 180.0

  def update(state: AhrsState, gyro: (Double, Double, Double), accel: (Double, Double, Double), dt: Double): AhrsState = {
    val (q0, q1, q2, q3) = (state.q0, state.q1, state.q2, state.q3)
    var (gx, gy, gz) = (gyro._1 * deg2rad, gyro._2 * deg2rad, gyro._3 * deg2rad)
    val (ax, ay, az) = accel
    var (ix, iy, iz) = (state.ix, state.iy, state.iz)

    val aNorm = math.sqrt(ax * ax + ay * ay + az * az)

    if (aNorm > 1e-6) {
      val nax = ax / aNorm
      val nay = ay / aNorm
      val naz = az / aNorm

      // Estimated gravity direction from quaternion
      val vx = 2.0 * (q1 * q3 - q0 * q2)
      val vy = 2.0 * (q0 * q1 + q2 * q3)
      val vz = q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3

      // Error: cross product of measured vs estimated gravity
      val ex = nay * vz - naz * vy
      val ey = naz * vx - nax * vz
      val ez = nax * vy - nay * vx

      // Integral feedback
      ix = ix + ki * ex * dt
      iy = iy + ki * ey * dt
      iz = iz + ki * ez * dt

      // Apply proportional + integral correction
      gx = gx + kp * ex + ix
      gy = gy + kp * ey + iy
      gz = gz + kp * ez + iz
    }

    // Quaternion derivative
    val qDot0 = 0.5 * (-q1 * gx - q2 * gy - q3 * gz)
    val qDot1 = 0.5 * ( q0 * gx + q2 * gz - q3 * gy)
    val qDot2 = 0.5 * ( q0 * gy - q1 * gz + q3 * gx)
    val qDot3 = 0.5 * ( q0 * gz + q1 * gy - q2 * gx)

    // Integrate
    val nq0 = q0 + qDot0 * dt
    val nq1 = q1 + qDot1 * dt
    val nq2 = q2 + qDot2 * dt
    val nq3 = q3 + qDot3 * dt

    // Normalize
    val qNorm = math.sqrt(nq0 * nq0 + nq1 * nq1 + nq2 * nq2 + nq3 * nq3)
    AhrsState(nq0 / qNorm, nq1 / qNorm, nq2 / qNorm, nq3 / qNorm, ix, iy, iz)
  }
}
