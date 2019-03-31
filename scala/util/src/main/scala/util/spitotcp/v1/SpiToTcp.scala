package util.spitotcp.v1

import java.net.ServerSocket
import scala.math.min
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import fc.device.controller.spi.{ SpiAddress, SpiController }

object SpiToTcp {

  def main(args: Array[String]): Unit = {
    val gps = SpiAddress(busNumber = 0, chipSelect = 0)
    val spiController = SpiController()

    val serverSocket = new ServerSocket(args(0).toInt, 0)
    println(s"Listening on ${serverSocket.getLocalPort}")
    println(s"\tReceive buffer size (bytes): ${serverSocket.getReceiveBufferSize}")
    println(s"\tSocket timeout (milliseconds): ${serverSocket.getSoTimeout}")
    println(s"\tSO_REUSEADDR is enabled: ${serverSocket.getReuseAddress}")
    println(Seq.fill(50)("-").mkString)

    while (true) {
      val clientSocket = serverSocket.accept()
      println(s"Connected to ${clientSocket.getInetAddress.getHostAddress}:${clientSocket.getPort}")
      println(s"\tSO_KEEPALIVE enabled: ${clientSocket.getKeepAlive}")
      println(s"\tTCP_NODELAY enabled: ${clientSocket.getTcpNoDelay}")
      println(s"\tTraffic class: ${clientSocket.getTrafficClass}")

      val clientInput = clientSocket.getInputStream
      val clientOutput = clientSocket.getOutputStream
      while (true) {
        val maxBytesToTransfer: Int Refined NonNegative = 100

        val dataToSpi = (0 until min(maxBytesToTransfer, clientInput.available())) map {_ => clientInput.read.toByte }

        val dataFromSpi = spiController.transferN(gps, dataToSpi, maxBytesToTransfer).fold(l => throw new RuntimeException(l.toString), identity)

        clientOutput.write(dataFromSpi.toArray)

        Thread.sleep(100)

      } // while(true)
    } // while(true)
  }

}
