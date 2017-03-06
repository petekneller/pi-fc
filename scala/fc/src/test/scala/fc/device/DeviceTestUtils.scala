package fc.device

trait DeviceTestUtils {

  class MockDeviceAddress extends Address {
    type Bus = Boolean
    def toFilename = "unused"
  }

  trait MockController extends Controller { type Bus = Boolean; type Register = Byte }
}
