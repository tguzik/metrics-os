# metrics-os

![Java CI with Maven](https://github.com/tguzik/metrics-os/workflows/Java%20CI%20with%20Maven/badge.svg?branch=master)

This project provides set of [Dropwizard Metrics](https://github.com/dropwizard/metrics) gauges that capture runtime
environment, operating system and hardware data. This allows reporting and acting on anomalous readings using the
same infrastructure as regular application metrics.

The data is acquired using the [oshi library](https://github.com/oshi/oshi) 


## Version & availability
The current version of this library is: `0.0.1-SNAPSHOT`

Once more features get implemented (load avg, per-cpu, per-network interface and storage gauges), this library
will be published in the Maven Central repository. 


## Usage
To include provided metrics all you have to do is:

```java
public void useNewFancyMetrics( final MetricRegistry registry ) {
    registry.registerAll( new OperatingEnvironmentGaugeSet() );
} 
```

Optionally you can specify a prefix for the metrics, or register a subset of the metrics by using one of the public
methods in the `OperatingEnvironmentGaugeSet` class.


## Provided metrics
At the moment this library provides gauges:

* `self.pid`
* `os.uptime`
* `os.proc.thread.count`
* `os.proc.process.count`
* `os.net.hostname`
* `os.net.domainname`
* `os.net.dnsservers`
* `os.net.ipv4.tcp.segments.received`
* `os.net.ipv6.tcp.connections.established`
* `os.net.ipv4.tcp.connections.active`
* `os.net.ipv4.tcp.segments.sent`
* `os.net.ipv4.udp.datagrams.received`
* `os.net.ipv4.tcp.connections.passive`
* `os.net.ipv4.tcp.connections.established`
* `os.net.ipv6.udp.datagrams.received.no-port`
* `os.net.ipv4.tcp.segments.retransmitted`
* `os.net.ipv6.tcp.connections.passive`
* `os.net.ipv6.tcp.connections.reset`
* `os.net.ipv6.tcp.connections.failures`
* `os.net.ipv6.udp.datagrams.received`
* `os.net.ipv6.udp.datagrams.received.errors`
* `os.net.ipv6.gateway.default`
* `os.net.ipv6.tcp.connections.active`
* `os.net.ipv6.tcp.segments.retransmitted`
* `os.net.ipv4.udp.datagrams.received.no-port`
* `os.net.ipv4.tcp.connections.reset`
* `os.net.ipv6.tcp.segments.received`
* `os.net.ipv4.tcp.connections.failures`
* `os.net.ipv4.udp.datagrams.received.errors`
* `os.net.ipv6.udp.datagrams.sent`
* `os.net.ipv4.udp.datagrams.sent`
* `os.net.ipv4.gateway.default`
* `os.net.ipv6.tcp.segments.sent`
* `os.fs.fd.open`
* `os.fs.fd.max`
* `os.family`
* `os.bits`
* `hw.sensors.cpu.temperature`
* `hw.mem.virtual.used`
* `hw.mem.virtual.total`
* `hw.mem.total`
* `hw.mem.swap.used`
* `hw.mem.swap.total`
* `hw.mem.page.size`
* `hw.mem.available`
* `hw.cpu.physical.packages`
* `hw.cpu.physical.count`
* `hw.cpu.logical.count`
* `hw.cpu.interrupts`
* `hw.cpu.id`
* `hw.cpu.freq.max`
* `hw.cpu.context-switches`


## FAQ

**Q: Is this production ready?**

A: No, not yet. You are welcome to experiment with it, but the API is not stable yet. 

Please keep in mind we acquire data through the [oshi](https://github.com/oshi/oshi) library, which in turn
acquires the data using JNA, which may have performance and stability implications. Both data acquisition and
publishing may have direct and indirect security implications. 

Please use your best judgement when it comes to handling this kind of data. Publishing it over a publicly available
interface may not be what you wish to do.


**Q: Are any of the metrics dependent on the operating system (\*nix, Windows)?**

A: Yes, please refer to [oshi](https://github.com/oshi/oshi) library documentation on what is available on which 
operating systems. 


**Q: I wanna help out, how do I do it?** 

A: As with pretty much any Open Source project any help is welcome. Depending on what you feel like doing, you can
pick from working on github issues (if any are submitted/open), working on `TODO`s and `FIXME`s scattered through
code and/or writing documentation. The lowest hanging fruit at the moment is adding documentation what each metric is
representing and a sample value.


## License 

This project is available under MIT license. 


