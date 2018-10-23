package fc.device.api

trait DeviceTestUtils {

  class MockDeviceAddress extends Address {
    type Bus = Boolean
    def toFilename = "unused"
  }

  trait MockByteController extends Controller { type Bus = Boolean; type Register = Byte }

  trait MockStringController extends Controller { type Bus = Boolean; type Register = String }
}
