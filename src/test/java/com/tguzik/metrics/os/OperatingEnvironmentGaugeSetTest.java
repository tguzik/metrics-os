package com.tguzik.metrics.os;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.codahale.metrics.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import oshi.SystemInfo;

class OperatingEnvironmentGaugeSetTest {

    private SystemInfo shallowStubbedSystemInfo;
    private SystemInfo deeplyStubbedSystemInfo;
    private ImmutableSet<String> expectedKeys;
    private MetricRegistry registry;

    @BeforeEach
    void setUp() {
        this.deeplyStubbedSystemInfo = mock( SystemInfo.class, Mockito.RETURNS_DEEP_STUBS );
        this.shallowStubbedSystemInfo = mock( SystemInfo.class, Mockito.RETURNS_SMART_NULLS );

        this.registry = new MetricRegistry();

        this.expectedKeys = ImmutableSet.of( "self.pid",
                                             "os.uptime",
                                             "os.proc.thread.count",
                                             "os.proc.process.count",
                                             "os.net.hostname",
                                             "os.net.domainname",
                                             "os.net.dnsservers",
                                             "os.net.ipv4.tcp.segments.received",
                                             "os.net.ipv6.tcp.connections.established",
                                             "os.net.ipv4.tcp.connections.active",
                                             "os.net.ipv4.tcp.segments.sent",
                                             "os.net.ipv4.udp.datagrams.received",
                                             "os.net.ipv4.tcp.connections.passive",
                                             "os.net.ipv4.tcp.connections.established",
                                             "os.net.ipv6.udp.datagrams.received.no-port",
                                             "os.net.ipv4.tcp.segments.retransmitted",
                                             "os.net.ipv6.tcp.connections.passive",
                                             "os.net.ipv6.tcp.connections.reset",
                                             "os.net.ipv6.tcp.connections.failures",
                                             "os.net.ipv6.udp.datagrams.received",
                                             "os.net.ipv6.udp.datagrams.received.errors",
                                             "os.net.ipv6.gateway.default",
                                             "os.net.ipv6.tcp.connections.active",
                                             "os.net.ipv6.tcp.segments.retransmitted",
                                             "os.net.ipv4.udp.datagrams.received.no-port",
                                             "os.net.ipv4.tcp.connections.reset",
                                             "os.net.ipv6.tcp.segments.received",
                                             "os.net.ipv4.tcp.connections.failures",
                                             "os.net.ipv4.udp.datagrams.received.errors",
                                             "os.net.ipv6.udp.datagrams.sent",
                                             "os.net.ipv4.udp.datagrams.sent",
                                             "os.net.ipv4.gateway.default",
                                             "os.net.ipv6.tcp.segments.sent",
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

    @Nested
    class GetMetrics {
        private OperatingEnvironmentGaugeSet gaugeSet;

        @BeforeEach
        void setUp() {
            this.gaugeSet = spy( new OperatingEnvironmentGaugeSet( shallowStubbedSystemInfo,
                                                                   Clock.defaultClock(),
                                                                   1,
                                                                   TimeUnit.SECONDS ) );
        }

        @Test
        void returns_map_with_expected_keys() {
            final Map<String, Metric> actual = gaugeSet.getMetrics();

            assertThat( actual ).isNotNull().containsOnlyKeys( expectedKeys );
        }

        @Test
        void each_metric_is_a_gauge() {
            final Map<String, Metric> actual = gaugeSet.getMetrics();

            assertThat( actual ).allSatisfy( ( k, v ) -> assertThat( v ).describedAs( "%s is not a gauge", k )
                                                                        .isInstanceOf( Gauge.class ) );
        }

        @Test
        void returned_metrics_are_accepted_by_MetricRegistry() {
            registry.registerAll( gaugeSet );

            assertThat( registry.getMetrics() ).containsOnlyKeys( expectedKeys );
            assertThat( registry.getGauges() ).containsOnlyKeys( expectedKeys );

            assertThat( registry.getHistograms() ).isEmpty();
            assertThat( registry.getCounters() ).isEmpty();
            assertThat( registry.getMeters() ).isEmpty();
            assertThat( registry.getTimers() ).isEmpty();
        }

    }

    @Nested
    class UsingMockedSystemInfo {

        private BiConsumer<String, Metric> gaugeReturnsOneOf( final String... acceptedValues ) {
            return ( name, metric ) -> {
                assertThat( metric ).describedAs( "%s is not a gauge", name ).isInstanceOf( Gauge.class );

                final Gauge<?> gauge = (Gauge<?>) metric;
                final Object returnedValue = gauge.getValue();

                assertThat( returnedValue ).describedAs( "%s one of desired values", name )
                                           .isNotNull()
                                           .isInstanceOf( String.class )
                                           .isIn( ImmutableList.copyOf( acceptedValues ) );
            };
        }

        @Nested
        class UsingShallowlyStubbedSystemInfo {
            private OperatingEnvironmentGaugeSet gaugeSet;

            @BeforeEach
            void setUp() {
                this.gaugeSet = spy( new OperatingEnvironmentGaugeSet( shallowStubbedSystemInfo,
                                                                       Clock.defaultClock(),
                                                                       1,
                                                                       TimeUnit.SECONDS ) );
            }

            @Test
            void each_metric_returns_empty_string() {
                final Map<String, Metric> actual = gaugeSet.getMetrics();

                assertThat( actual ).isNotNull().containsOnlyKeys( expectedKeys ).allSatisfy( gaugeReturnsOneOf( "" ) );
            }

            @Test
            void metrics_can_be_reported_after_adding_them_to_a_registry() {
                registry.registerAll( gaugeSet );

                Slf4jReporter.forRegistry( registry ).build().report();

                // (no exceptions)
            }

        }

        @Nested
        class UsingDeeplyStubbedSystemInfo {
            private OperatingEnvironmentGaugeSet gaugeSet;

            @BeforeEach
            void setUp() {
                this.gaugeSet = spy( new OperatingEnvironmentGaugeSet( deeplyStubbedSystemInfo,
                                                                       Clock.defaultClock(),
                                                                       1,
                                                                       TimeUnit.SECONDS ) );
            }

            /**
             * Some SystemInfo values return an {@code int} or a {@code double}, so we can't easily recognize
             * whether it was a failure or the underlying is really at zero.
             */
            @Test
            void each_metric_returns_zero_or_an_empty_string() {
                final Map<String, Metric> actual = gaugeSet.getMetrics();

                assertThat( actual ).isNotNull()
                                    .containsOnlyKeys( expectedKeys )
                                    .allSatisfy( gaugeReturnsOneOf( "", "0", "0.0" ) );
            }

            @Test
            void metrics_can_be_reported_after_adding_them_to_a_registry() {
                registry.registerAll( gaugeSet );

                Slf4jReporter.forRegistry( registry ).build().report();

                // (no exceptions)
            }

        }

    }

    @Nested
    class UsingRealSystemInfo {
        private OperatingEnvironmentGaugeSet gaugeSet;

        @BeforeEach
        void setUp() {
            this.gaugeSet = spy( new OperatingEnvironmentGaugeSet( new SystemInfo(),
                                                                   Clock.defaultClock(),
                                                                   1,
                                                                   TimeUnit.SECONDS ) );
        }

        private void returnsString( final String name, final Metric metric ) {
            assertThat( metric ).describedAs( "%s is not a gauge", name ).isInstanceOf( Gauge.class );

            final Gauge<?> gauge = (Gauge<?>) metric;
            final Object returnedValue = gauge.getValue();

            assertThat( returnedValue ).describedAs( "%s returns a string, any string", name )
                                       .isNotNull()
                                       .isInstanceOf( String.class );
        }

        /** We can't assume that we will have access to all metrics each time we run these tests */
        @Test
        void each_metric_returns_a_string() {
            final Map<String, Metric> actual = gaugeSet.getMetrics();

            assertThat( actual ).isNotNull().containsOnlyKeys( expectedKeys ).allSatisfy( this::returnsString );
        }

        @Test
        void metrics_can_be_reported_after_adding_them_to_a_registry() {
            registry.registerAll( gaugeSet );

            Slf4jReporter.forRegistry( registry ).build().report();

            // (no exceptions)
        }

    }

}