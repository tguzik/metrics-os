package com.tguzik.metrics.os;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Clock;

public class SupplierBasedCachedGauge<T> extends CachedGauge<T> {
    protected final Supplier<T> uncachedSupplier;

    public SupplierBasedCachedGauge( final long timeout, final TimeUnit timeoutUnit, final Supplier<T> uncachedSupplier ) {
        this( Clock.defaultClock(), timeout, timeoutUnit, uncachedSupplier );
    }

    public SupplierBasedCachedGauge( final Clock clock,
                                     final long timeout,
                                     final TimeUnit timeoutUnit,
                                     final Supplier<T> uncachedSupplier ) {
        super( clock, timeout, timeoutUnit );
        this.uncachedSupplier = Objects.requireNonNull( uncachedSupplier );
    }

    @Override
    protected T loadValue() {
        return uncachedSupplier.get();
    }

}
