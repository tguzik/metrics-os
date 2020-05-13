package com.tguzik.metrics.os;

import java.util.Objects;
import java.util.function.Supplier;

import com.codahale.metrics.Gauge;
import io.vavr.Lazy;
import io.vavr.control.Try;

/**
 * This class is meant to package metric values that should never ever change during the lifetime of the process, like
 * Operating System's being 32 or 64 bit.
 * <p>
 * This gauge will call the supplier at most once.
 * <p>
 * When the retrieval is successful, this gauge will keep returning the value stringified via {@link String#valueOf}.
 * <p>
 * When the retrieval throws an exception or returns a null, this gauge will keep returning an empty string.
 */
public class SupplierBasedLazyGauge implements Gauge<String> {

    // Let's avoid having to write thread safe lazy value acquisition for the umpteenth time
    protected final Lazy<String> lazy;

    public SupplierBasedLazyGauge( final Supplier<?> valueSupplier ) {
        this.lazy = Lazy.of( () -> Try.ofSupplier( valueSupplier )
                                      .filter( Objects::nonNull )
                                      .map( String::valueOf )
                                      .getOrElse( "" ) );
    }

    @Override
    public String getValue() {
        return lazy.get();
    }

}
