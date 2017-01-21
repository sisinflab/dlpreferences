package it.poliba.enasca.ontocpnets.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A lazy initializer for a value of type {@link T}.
 */
public class Lazy<T> {
    private T value;
    private Supplier<T> valueSupplier;

    public Lazy(Supplier<T> valueSupplier) {
        this.valueSupplier = Objects.requireNonNull(valueSupplier);
    }

    public T getOrCompute() {
        if (value == null) {
            value = valueSupplier.get();
        }
        return value;
    }
}
