package com.tguzik.metrics.os;

import static com.codahale.metrics.MetricRegistry.name;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.codahale.metrics.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OperatingSystem;

/*
 * TODO: Split this into multiple gauge set classes - say, one with basic operational metrics (cpu, memory, load),
 * one with network activity, one with disk details, and one with the rest.
 *
 * It's a breaking change, but we're still before 1.0
 */
public class OperatingEnvironmentGaugeSet implements MetricSet {
    private final TimeUnit cacheTimeoutUnit;
    private final SystemInfo systemInfo;
    private final long cacheTimeout;
    private final Clock clock;

    public OperatingEnvironmentGaugeSet() {
        this( 1, TimeUnit.SECONDS );
    }

    public OperatingEnvironmentGaugeSet( final long cacheTimeout, final TimeUnit cacheTimeoutUnit ) {
        this( new SystemInfo(), Clock.defaultClock(), cacheTimeout, cacheTimeoutUnit );
    }

    public OperatingEnvironmentGaugeSet( final SystemInfo systemInfo,
                                         final Clock clock,
                                         final long cacheTimeout,
                                         final TimeUnit cacheTimeoutUnit ) {
        this.cacheTimeoutUnit = Objects.requireNonNull( cacheTimeoutUnit );
        this.systemInfo = Objects.requireNonNull( systemInfo );
        this.clock = Objects.requireNonNull( clock );
        this.cacheTimeout = cacheTimeout;
    }

    private Gauge<String> oneShotGauge( Supplier<?> uncachedSupplier ) {
        return new SupplierBasedLazyGauge( uncachedSupplier );
    }

    private CachedGauge<String> cachedGauge( Supplier<?> uncachedSupplier ) {
        return new SupplierBasedCachedGauge( clock, cacheTimeout, cacheTimeoutUnit, uncachedSupplier );
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> product = new HashMap<>();

        product.put( "self.pid", oneShotGauge( () -> systemInfo.getOperatingSystem().getProcessId() ) );

        operatingSystemGauges().forEach( ( key, gauge ) -> product.put( name( "os", key ), gauge ) );
        hardwareGauges().forEach( ( key, gauge ) -> product.put( name( "hw", key ), gauge ) );

        return product;
    }

    public Map<String, Gauge<?>> operatingSystemGauges() {
        final Map<String, Gauge<?>> product = new HashMap<>();
        final Supplier<OperatingSystem> os = systemInfo::getOperatingSystem;

        product.put( "bits", oneShotGauge( () -> os.get().getBitness() ) );
        product.put( "family", oneShotGauge( () -> os.get().getFamily() ) );
        product.put( "version", oneShotGauge( () -> os.get().getVersionInfo().toString() ) );
        product.put( "uptime", cachedGauge( () -> os.get().getSystemUptime() ) );

        product.put( "proc.process.count", cachedGauge( () -> os.get().getProcessCount() ) );
        product.put( "proc.thread.count", cachedGauge( () -> os.get().getThreadCount() ) );

        product.put( "fs.fd.open", cachedGauge( () -> os.get().getFileSystem().getOpenFileDescriptors() ) );
        product.put( "fs.fd.max", cachedGauge( () -> os.get().getFileSystem().getMaxFileDescriptors() ) );

        product.put( "net.hostname", cachedGauge( () -> os.get().getNetworkParams().getHostName() ) );
        product.put( "net.domainname", cachedGauge( () -> os.get().getNetworkParams().getDomainName() ) );
        product.put( "net.dnsservers",
                     cachedGauge( () -> String.join( "; ", os.get().getNetworkParams().getDnsServers() ) ) );

        this.ipGaugeSet( () -> os.get().getNetworkParams().getIpv4DefaultGateway(),
                         () -> os.get().getInternetProtocolStats().getTCPv4Stats(),
                         () -> os.get().getInternetProtocolStats().getUDPv4Stats() )
            .forEach( ( key, gauge ) -> product.put( name( "net.ipv4", key ), gauge ) );

        this.ipGaugeSet( () -> os.get().getNetworkParams().getIpv6DefaultGateway(),
                         () -> os.get().getInternetProtocolStats().getTCPv6Stats(),
                         () -> os.get().getInternetProtocolStats().getUDPv6Stats() )
            .forEach( ( key, gauge ) -> product.put( name( "net.ipv6", key ), gauge ) );

        return product;
    }

    protected Map<String, Gauge<?>> ipGaugeSet( final Supplier<String> defaultGatewaySupplier,
                                                final Supplier<InternetProtocolStats.TcpStats> tcp,
                                                final Supplier<InternetProtocolStats.UdpStats> udp ) {
        final Map<String, Gauge<?>> product = new HashMap<>();

        product.put( "gateway.default", cachedGauge( defaultGatewaySupplier ) );

        product.put( "tcp.connections.active", cachedGauge( () -> tcp.get().getConnectionsActive() ) );
        product.put( "tcp.connections.established", cachedGauge( () -> tcp.get().getConnectionsEstablished() ) );
        product.put( "tcp.connections.passive", cachedGauge( () -> tcp.get().getConnectionsPassive() ) );
        product.put( "tcp.connections.reset", cachedGauge( () -> tcp.get().getConnectionsReset() ) );
        product.put( "tcp.connections.failures", cachedGauge( () -> tcp.get().getConnectionFailures() ) );

        product.put( "tcp.segments.sent", cachedGauge( () -> tcp.get().getSegmentsSent() ) );
        product.put( "tcp.segments.received", cachedGauge( () -> tcp.get().getSegmentsReceived() ) );
        product.put( "tcp.segments.retransmitted", cachedGauge( () -> tcp.get().getSegmentsRetransmitted() ) );

        product.put( "udp.datagrams.sent", cachedGauge( () -> udp.get().getDatagramsSent() ) );
        product.put( "udp.datagrams.received", cachedGauge( () -> udp.get().getDatagramsReceived() ) );
        product.put( "udp.datagrams.received.errors", cachedGauge( () -> udp.get().getDatagramsReceivedErrors() ) );
        product.put( "udp.datagrams.received.no-port", cachedGauge( () -> udp.get().getDatagramsNoPort() ) );

        return product;
    }

    private Map<String, Gauge<?>> hardwareGauges() {
        final Map<String, Gauge<?>> product = new HashMap<>();
        final Supplier<HardwareAbstractionLayer> hw = systemInfo::getHardware;
        final Supplier<CentralProcessor> cpu = () -> hw.get().getProcessor();

        product.put( "cpu.id", oneShotGauge( () -> cpu.get().getProcessorIdentifier().toString() ) );
        product.put( "cpu.logical.count", oneShotGauge( () -> cpu.get().getLogicalProcessorCount() ) );
        product.put( "cpu.physical.count", oneShotGauge( () -> cpu.get().getPhysicalProcessorCount() ) );
        product.put( "cpu.physical.packages", oneShotGauge( () -> cpu.get().getPhysicalPackageCount() ) );

        // TODO: create a metric for each CPU
        //product.put( "cpu.freq.current", cachedGauge( () -> cpu.get().getCurrentFreq() ) );

        // TODO: Implement support for CPU load, disks and network interfaces
        //hw.getProcessor().getSystemCpuLoadTicks();
        //hw.getProcessor().getSystemLoadAverage();

        product.put( "cpu.freq.max", cachedGauge( () -> cpu.get().getMaxFreq() ) );

        product.put( "cpu.interrupts", cachedGauge( () -> cpu.get().getInterrupts() ) );
        product.put( "cpu.context-switches", cachedGauge( () -> cpu.get().getContextSwitches() ) );

        product.put( "mem.total", cachedGauge( () -> hw.get().getMemory().getTotal() ) );
        product.put( "mem.available", cachedGauge( () -> hw.get().getMemory().getAvailable() ) );
        product.put( "mem.page.size", cachedGauge( () -> hw.get().getMemory().getPageSize() ) );
        product.put( "mem.swap.used", cachedGauge( () -> hw.get().getMemory().getVirtualMemory().getSwapUsed() ) );
        product.put( "mem.swap.total", cachedGauge( () -> hw.get().getMemory().getVirtualMemory().getSwapTotal() ) );
        product.put( "mem.virtual.used",
                     cachedGauge( () -> hw.get().getMemory().getVirtualMemory().getVirtualInUse() ) );
        product.put( "mem.virtual.total",
                     cachedGauge( () -> hw.get().getMemory().getVirtualMemory().getVirtualMax() ) );

        product.put( "sensors.cpu.temperature", cachedGauge( () -> hw.get().getSensors().getCpuTemperature() ) );

        return product;
    }

}
