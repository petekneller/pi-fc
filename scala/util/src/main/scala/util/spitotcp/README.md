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

## v2

Not an update to improve efficiency/performance; instead an experiment in separating the two ends of the data pipe and putting an async/thread boundary between them.

The drop in size of chunks read/written to the TCP stream (back down to 1) likely has a small impact on efficiency as CPU usage is back up to 2-4%. Probably this is due to transmitting bytes 1 at a time from the queue to TCP output stream when we know that there will be chunks of at least `maxBytesToTransfer`.

## v3

An update to v2 that replaces the explicit threads with use of a scheduler. The scheduler uses an unlimited-size thread pool as, even though in this version the number of threads shouldn't exceed 3, that is the appropriate pattern when doing blocking IO.

## v4

An update to v3 that replaces java `Runnable`s with `Future`. Which is a bit of a bastardisation of Future, since Future's are best when composed to ultimately a value, while this implementation has side-effects and returns Unit.

# v5

A version using `cats.effect.IO` to construct tasks that are then `parSequence`d to run concurrently (and never end)

# v6

A version using basic `fs2` features

# v7

A more realistic `fs2` example using NIO for the incoming TCP socket
