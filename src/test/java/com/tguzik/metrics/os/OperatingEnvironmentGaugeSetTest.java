package com.tguzik.metrics.os;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import oshi.SystemInfo;

class OperatingEnvironmentGaugeSetTest {

    private SystemInfo deeplyStubbedSystemInfo;
    private OperatingEnvironmentGaugeSet gaugeSet;

    @BeforeEach
    void setUp() {
        this.deeplyStubbedSystemInfo = mock( SystemInfo.class, Mockito.RETURNS_DEEP_STUBS );

        this.gaugeSet = spy( new OperatingEnvironmentGaugeSet( deeplyStubbedSystemInfo,
                                                               Clock.defaultClock(),
                                                               1,
                                                               TimeUnit.SECONDS ) );
    }

    @Nested
    class GetMetrics {

        @Test
        void returns_map_with_expected_keys() {
            final Map<String, Metric> actual = gaugeSet.getMetrics();

            assertThat( actual ).isNotNull()
                                .containsOnlyKeys( "self.pid",
                                                   "os.version",
                                                   "os.uptime",
                                                   "os.proc.thread.count",
                                                   "os.proc.process.count",
                                                   "os.net.hostname",
                                                   "os.net.domainname",
                                                   "os.net.dnsservers",
                                                   "os.fs.fd.open",
                                                   "os.fs.fd.max",
                                                   "os.family",
                                                   "os.bits",
                                                   "hw.sensors.cpu.temperature",
                                                   "hw.mem.virtual.used",
                                                   "hw.mem.virtual.total",
                                                   "hw.mem.total",
                                                   "hw.mem.swap.used",
                                                   "hw.mem.swap.total",
                                                   "hw.mem.page.size",
                                                   "hw.mem.available",
                                                   "hw.cpu.physical.packages",
                                                   "hw.cpu.physical.count",
                                                   "hw.cpu.logical.count",
                                                   "hw.cpu.interrupts",
                                                   "hw.cpu.id",
                                                   "hw.cpu.freq.max",
                                                   "hw.cpu.context-switches" );
        }
    }

}