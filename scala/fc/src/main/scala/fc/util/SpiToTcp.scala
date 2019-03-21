package fc.util

import java.net.ServerSocket
import fc.device.controller.spi.{ SpiAddress, SpiController }

/*
 As close a port of https://github.com/emlid/Navio2/blob/master/Utilities/ublox-spi-to-tcp/ublox-spi-to-tcp.c to scala as I can get. For now.
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

        println(s"Read from TCP: ${if (dataToSpi.isDefined) 1 else 0 } byte(s)")

        val dataFromSpi = spiController.transfer(gps, dataToSpi).fold(l => throw new RuntimeException(l.toString), identity)

        println(s"Read from SPI byte: ${dataFromSpi.toString}")
        clientOutput.write(Array(dataFromSpi))

      } // while(true)
    } // while(true)
  }

}
