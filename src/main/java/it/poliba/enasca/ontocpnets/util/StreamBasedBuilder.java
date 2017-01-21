package it.poliba.enasca.ontocpnets.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A base class for a builder that collects items into {@link Stream}s.
 * @param <T> the type of object being built
 */
public abstract class StreamBasedBuilder<T> {
    /**
     * Adds elements of type {@link E} to the given {@link Stream.Builder}.
     * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
     */
    @SafeVarargs
    protected final <E> void addElements(Stream.Builder<E> builder, E... elements) {
        addElements(builder, Arrays.asList(elements));
    }

    /**
     * Adds elements of type {@link E} to the given {@link Stream.Builder}.
     * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
     */
    protected <E> void addElements(Stream.Builder<E> builder, Iterable<E> elements) {
        for (E e : Objects.requireNonNull(elements)) {
            builder.accept(Objects.requireNonNull(e));
        }
    }

    /**
     * Builds an instance of type {@link T}
     * @return
     */
    abstract public T build();
}
