package core.device.controller.filesystem

trait FileSystemRegister {
  def toFilename(address: FileSystemAddress): String
}

object FileSystemRegister {
  def apply(register: String): FileSystemRegister = new FileSystemRegister {
    override def toFilename(address: FileSystemAddress): String = s"${address.toFilename}/${register}"
  }

  def singleton: FileSystemRegister = new FileSystemRegister {
    override def toFilename(address: FileSystemAddress): String = address.toFilename
  }
}
