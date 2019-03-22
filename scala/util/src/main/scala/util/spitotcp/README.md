# SPI to TCP

## v0

As close a port of https://github.com/emlid/Navio2/blob/master/Utilities/ublox-spi-to-tcp/ublox-spi-to-tcp.c to scala as I can get (though it does forgo direct use of ioctl for the higher level SpiController API).

Copies bytes bidirectionally between SPI and the TCP socket bound to an interested client. Terribly inefficient as it simply runs back and forth between the two devices copying empty bytes if nothing new is available.

The following is a screenshot of the CPU/mem usage of this running on the navio2:
![spi-to-tcp-scala](spi-to-tcp-scala.png SPI to TCP in Scala)

And this is a screenshot of the original C programming doing the same:
![spi-to-tcp-c](spi-to-tcp-c.png SPI to TCP in C)

Note that as well as the extra CPU usage by the scala version (and of course memory) the C CPU usage is mostly system, whereas with scala there's much more user time.

## v1

A very basic attempt to improve efficiency:
* transfers chunks rather than just 1 byte at a time
* adds a `Thread.sleep` so that the thread is not constantly spinning

Superficially this does improve CPU usage:
* transferring 100 bytes instead of 1 lead to CPU usage ~5%
* with 100 byte transfers and a `Thread.sleep(100)` - CPU ~1%
