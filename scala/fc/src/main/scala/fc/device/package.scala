package fc

package device {

  case class DeviceRegister(value: Int)

  trait DeviceAddress {
    type Bus

    def toFilename: String
  }

  trait Controller { self =>
    type Bus

    def read(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, numBytes: Int): Seq[Byte]

    def write(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, data: Byte): Unit
  }

}

package object device {

  def readRegister(device: DeviceAddress, register: DeviceRegister)(implicit controller: Controller { type Bus = device.Bus }): Byte = controller.read(device, register, 1).head

  def readRegisterBytes(device: DeviceAddress, register: DeviceRegister, numBytes: Int)(implicit controller: Controller { type Bus = device.Bus }): Seq[Byte] = controller.read(device, register, numBytes)

  def writeRegister(device: DeviceAddress, register: DeviceRegister, data: Byte)(implicit controller: Controller { type Bus = device.Bus }): Unit = controller.write(device, register, data)

}
