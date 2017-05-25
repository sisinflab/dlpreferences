package it.poliba.sisinflab.dlpreferences.tree;

import com.google.common.collect.ImmutableMap;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A generic implementation of <code>BasePreferenceForest</code>
 * for preference variables of type {@link P}.
 * @param <P> the type of objects representing preference variables
 */
public class PreferenceForest<P> extends AbstractPreferenceForest<P, Stream<P>>
        implements BasePreferenceForest<P, Stream<P>> {

    /**
     * A map where each preference variable is associated with
     * a list view on its successors.
     */
    private Map<P, List<P>> successors;

    /**
     * Constructs a <code>PreferenceForest</code> from a list of the form
     * <pre>p1, not(p1), p2, not(p2), &hellip; pN, not(pN)</pre>
     * where <code>N == zippedList.size()</code>.
     * @param zippedList
     */
    private PreferenceForest(List<P> zippedList) {
        if ((zippedList.size() % 2) != 0) {
            throw new IllegalArgumentException("the size of the input list is not a multiple of 2");
        }
        // Build the map of successors.
        ImmutableMap.Builder<P, List<P>> builder = ImmutableMap.builder();
        for (int i = 0; i < zippedList.size(); i += 2) {
            builder.put(zippedList.get(i), zippedList.subList(i+2, zippedList.size()));
            builder.put(zippedList.get(i+1), zippedList.subList(i+2, zippedList.size()));
        }
        successors = builder.build();
        // Build the initial fringe.
        leaves = successors.keySet().stream()
                .map(Node::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns the successors of <code>elem</code>.
     * @param elem
     * @return
     * @throws NoSuchElementException if <code>elem</code> is not handled by this forest
     */
    public Stream<P> successor(P elem) {
        List<P> s = successors.get(Objects.requireNonNull(elem));
        if (s == null) {
            throw new NoSuchElementException(String.format("element %s not found", elem));
        }
        return s.stream();
    }

    /**
     * Returns a <code>Collector</code> that accumulates elements
     * into a <code>PreferenceForest</code>. Logical complements are obtained
     * by applying <code>complementOf</code> to each input element.
     * @param complementOf
     * @param <T> the type of objects representing preference variables
     * @return
     */
    public static <T> Collector<T, ?, PreferenceForest<T>> toForest(UnaryOperator<T> complementOf) {
        Objects.requireNonNull(complementOf);
        // Zip the input elements and their complements into a list.
        Collector<T, List<T>, List<T>> toZippedList = Collector.of(
                ArrayList::new,
                (elements, e) -> { elements.add(e); elements.add(complementOf.apply(e)); },
                (left, right) -> { left.addAll(right); return left; });
        return Collectors.collectingAndThen(toZippedList, PreferenceForest::new);
    }

    /**
     * A node that stores an element of type {@link P} and a reference to its parent.
     */
    public class Node implements BaseNode<P, Stream<P>> {
        private Node parent;
        private P element;

        /**
         * Constructs a <code>Node</code> that stores an element and a reference to its parent.
         * @param element the object stored in this node
         * @param parent the parent node
         */
        private Node(P element, Node parent) {
            this.element = Objects.requireNonNull(element);
            this.parent = Objects.requireNonNull(parent);
        }

        /**
         * Constructs a root <code>Node</code> that stores the specified element.
         * @param element
         */
        private Node(P element) {
            this.element = Objects.requireNonNull(element);
            parent = null;
        }

        @Override
        public Optional<Node> getParent() {
            return Optional.ofNullable(parent);
        }

        @Override
        public Stream<P> getReachable() {
            Stream.Builder<P> builder = Stream.<P>builder().add(element);
            Optional<Node> ancestor = getParent();
            while (ancestor.isPresent()) {
                Node n = ancestor.get();
                builder.accept(n.element);
                ancestor = n.getParent();
            }
            return builder.build();
        }

        @Override
        public Stream<Node> children() {
            return successor(element)
                    .map(childElement -> new Node(childElement, this));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PreferenceForest<?>.Node other = (PreferenceForest<?>.Node) o;
            return element.equals(other.element);
        }

        @Override
        public int hashCode() {
            return element.hashCode();
        }
    }
}
