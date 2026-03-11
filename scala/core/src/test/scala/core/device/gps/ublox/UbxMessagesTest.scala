package core.device.gps.ublox

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import core.device.gps.{ ExampleMessage, ParserTestSupport }
import UbxMessage.Unknown

class UbxMessagesTest extends AnyFunSpec with Matchers with TypeCheckedTripleEquals with ParserTestSupport {

  describe("A generic message") {
    describe(".toBytes") {
      it("should have 0xB5 as the first byte") {
        examples.unknown.msg.toBytes(0) should === (0xB5.toByte)
      }

      it("should have 0x62 as the second byte") {
        examples.unknown.msg.toBytes(1) should === (0x62.toByte)
      }

      it("should have the message class as the third byte") {
        examples.unknown.msg.toBytes(2) should === (examples.unknown.clazz)
      }

      it("should have the message id as the fourth byte") {
        examples.unknown.msg.toBytes(3) should === (examples.unknown.id)
      }

      it ("should have the lo-byte of the length as the fifth byte") {
        examples.unknown.msg.toBytes(4) should === (0x03.toByte)
      }

      it ("should have the hi-byte of the length as the sixth byte") {
        examples.unknown.msg.toBytes(5) should === (0x00.toByte)
      }

      it("should have the first checksum byte as the second to last byte") {
        examples.unknown.msg.toBytes.init.last should === (examples.unknown.checksum1)
      }

      it("should have the second checksum byte as the last byte") {
        examples.unknown.msg.toBytes.last should === (examples.unknown.checksum2)
      }
      it("should match the value of the recorded bytes") {
        examples.unknown.msg.toBytes should === (examples.unknown.bytes)
      }
    }
    describe("when parsed from test data") {
      it("should match the expected message") {
        consume(UbxParser(), examples.unknown.bytes) should be (Done(examples.unknown.msg))
      }
    }
  }

  examples.all.foreach { case ExampleMessage(name, msg, bytes) =>
    describe(name) {
      describe(".toBytes") {
        it("should have 0xB5 as the first byte") {
          msg.toBytes(0) should === (0xB5.toByte)
        }

        it("should have 0x62 as the second byte") {
          msg.toBytes(1) should === (0x62.toByte)
        }

        it("should have the message class as the third byte") {
          msg.toBytes(2) should === (msg.clazz)
        }

        it("should have the message id as the fourth byte") {
          msg.toBytes(3) should === (msg.id)
        }
        it("should match the value of the recorded bytes") {
          msg.toBytes should === (bytes)
        }
      }
      describe("when parsed from test data") {
        it("should match the expected message") {
          consume(UbxParser(), bytes) should be (Done(msg))
        }
      }
    }
  }

  describe("object UbxMessage") {
    describe(".parse()") {
      it("should return Right for a valid, recognised message") {
        val example = examples.UbxMonitorTxBufferPoll.msg
        UbxMessage.parse(example.clazz, example.id, Seq.empty[Byte]) should ===(Right(example))
      }

      it("should return Unknown when the class is not recognised") {
        val notAClazz = 0xFF.toByte
        val example = examples.UbxMonitorTxBufferPoll.msg
        UbxMessage.parse(notAClazz, example.id, Seq.empty[Byte]) should ===(Right(Unknown(notAClazz, example.id, Seq.empty[Byte])))
      }

      it("should return Unknown when the id is not recognised") {
        val notAnId = 0xFF.toByte
        val example = examples.UbxMonitorTxBufferPoll.msg
        UbxMessage.parse(example.clazz, notAnId, Seq.empty[Byte]) should ===(Right(Unknown(example.clazz, notAnId, Seq.empty[Byte])))
      }
      //TODO: it("should return Left when the underlying message cannot recognise its payload") {}
    }
  }

  type Msg = UbxMessage // ParserTestSupport
}
