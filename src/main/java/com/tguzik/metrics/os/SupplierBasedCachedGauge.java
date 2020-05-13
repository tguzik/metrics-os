package com.tguzik.metrics.os;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Clock;

/**
 * This class is meant to package metric values that change over time, but for performance reasons should not be
 * retrieved more often than set period of time.
 *
 * When the retrieval is successful, this gauge will return the value stringified via {@link String#valueOf}.
 *
 * When the retrieval throws an exception or returns a null, this gauge will return an empty string.
 */
public class SupplierBasedCachedGauge extends CachedGauge<String> {
    public static final String EMPTY_STRING = "";
    protected final Supplier<?> uncachedSupplier;

    public SupplierBasedCachedGauge( final long timeout,
                                     final TimeUnit timeoutUnit,
                                     final Supplier<?> uncachedSupplier ) {
        this( Clock.defaultClock(), timeout, timeoutUnit, uncachedSupplier );
    }

    public SupplierBasedCachedGauge( final Clock clock,
                                     final long timeout,
                                     final TimeUnit timeoutUnit,
                                     final Supplier<?> uncachedSupplier ) {
        super( clock, timeout, timeoutUnit );
        this.uncachedSupplier = Objects.requireNonNull( uncachedSupplier );
    }

    @Override
    protected String loadValue() {
        try {
            return Optional.ofNullable( uncachedSupplier.get() ).map( String::valueOf ).orElse( EMPTY_STRING );
        }
        catch ( Exception e ) {
            // We don't know how often this will be called, we don't know if a metric makes sense on target operating
            // system and we don't know if the user cares in the first place.
            //
            // Let's not blow up user code by throwing an exception and/or returning a null.
            return EMPTY_STRING;
        }
    }

}
