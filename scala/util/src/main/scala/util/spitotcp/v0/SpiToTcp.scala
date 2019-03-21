package util.spitotcp.v0

import java.net.ServerSocket
import fc.device.controller.spi.{ SpiAddress, SpiController }

/*
 See README.md in util.spitotcp
 */
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

        val dataToSpi = if (clientInput.available() >= 1)
          Some(clientInput.read.toByte)
        else
          None

        val dataFromSpi = spiController.transfer(gps, dataToSpi).fold(l => throw new RuntimeException(l.toString), identity)

        clientOutput.write(Array(dataFromSpi))

      } // while(true)
    } // while(true)
  }

}
