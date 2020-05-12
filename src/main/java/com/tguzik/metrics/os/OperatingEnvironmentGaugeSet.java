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
import oshi.util.Memoizer;

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

    private <T> Gauge<T> memoizedGauge( Supplier<T> uncachedSupplier ) {
        return () -> Memoizer.memoize( uncachedSupplier ).get();
    }

    private <T> CachedGauge<T> cachedGauge( Supplier<T> uncachedSupplier ) {
        return new SupplierBasedCachedGauge<>( clock, cacheTimeout, cacheTimeoutUnit, uncachedSupplier );
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> product = new HashMap<>();

        // TODO: Split this into multiple gauge set classes?

        product.put( "self.pid", memoizedGauge( () -> systemInfo.getOperatingSystem().getProcessId() ) );

        operatingSystemGauges().forEach( ( key, gauge ) -> product.put( name( "os", key ), gauge ) );
        hardwareGauges().forEach( ( key, gauge ) -> product.put( name( "hw", key ), gauge ) );

        return product;
    }

    public Map<String, Metric> operatingSystemGauges() {
        final Map<String, Metric> product = new HashMap<>();
        final Supplier<OperatingSystem> os = systemInfo::getOperatingSystem;

        product.put( "bits", memoizedGauge( () -> os.get().getBitness() ) );
        product.put( "family", memoizedGauge( () -> os.get().getFamily() ) );
        product.put( "version", memoizedGauge( () -> os.get().getVersionInfo().toString() ) );
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

    protected Map<String, Metric> ipGaugeSet( final Supplier<String> defaultGatewaySupplier,
                                              final Supplier<InternetProtocolStats.TcpStats> tcpStatsSupplier,
                                              final Supplier<InternetProtocolStats.UdpStats> udpStatsSupplier ) {
        // FIXME: Implement
        return Map.of();
    }

    private Map<String, Metric> hardwareGauges() {
        final Map<String, Metric> product = new HashMap<>();
        final Supplier<HardwareAbstractionLayer> hw = systemInfo::getHardware;
        final Supplier<CentralProcessor> cpu = () -> hw.get().getProcessor();

        product.put( "cpu.id", memoizedGauge( () -> cpu.get().getProcessorIdentifier().toString() ) );
        product.put( "cpu.logical.count", memoizedGauge( () -> cpu.get().getLogicalProcessorCount() ) );
        product.put( "cpu.physical.count", memoizedGauge( () -> cpu.get().getPhysicalProcessorCount() ) );
        product.put( "cpu.physical.packages", memoizedGauge( () -> cpu.get().getPhysicalPackageCount() ) );

        // TODO: create a metric for each CPU
        //product.put( "cpu.freq.current", cachedGauge( () -> cpu.get().getCurrentFreq() ) );

        // TODO: Implement support for these:
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

        // TODO: Implement stats for disks and network interfaces

        return product;
    }

}
