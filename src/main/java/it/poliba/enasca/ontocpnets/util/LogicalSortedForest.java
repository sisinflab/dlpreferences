package it.poliba.enasca.ontocpnets.util;

import com.google.common.collect.ImmutableMap;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An ordered collection of rooted trees containing logical elements and their complements,
 * both of type <code>N</code>. The ordering of the logical elements is established
 * by a shared <em>ordering map</em>, which is an immutable {@link Map} that associates
 * logical elements with their complements.
 *
 * <p>A tree may only generate nodes that are elements of the ordering map.
 * More specifically, the map defines an ordering over the nodes that
 * {@link #successor(Object)} is allowed to generate.
 *
 * <p>The forest is constructed by creating <i>2*K</i> trees, <i>K</i> being the number of entries in the ordering map.
 * Each tree is rooted at a different element of the ordering map. Elements are wrapped in {@link Node<N>} objects.
 *
 * @param <N> the type of element that will be stored in each node
 */
public class LogicalSortedForest<N> {

    /**
     * An equivalent representation of the ordering map, where each element is mapped to its list of successors.
     * The implementation of this <code>Map</code> guarantees a predictable iteration order.
     *
     * <p>The list returned by
     * <pre>{@code successors.keySet().asList()}</pre>
     * provides a flattened view of the ordering map, where each even-indexed element
     * is immediately followed by its logical complement.
     */
    private Map<N, List<N>> successors;

    /**
     * The list of leaf nodes.
     */
    private List<Node<N>> leaves;

    private LogicalSortedForest(ImmutableMap<N, N> orderingMap) {
        List<N> orderingList = orderingMap.entrySet().stream()
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        ImmutableMap.Builder<N, List<N>> successorsBuilder = ImmutableMap.builder();
        for (int i = 0; i < orderingList.size(); i += 2) {
            successorsBuilder.put(orderingList.get(i), orderingList.subList(i+2, orderingList.size()));
            successorsBuilder.put(orderingList.get(i+1), orderingList.subList(i+2, orderingList.size()));
        }
        successors = successorsBuilder.build();
        leaves = successors.keySet().stream()
                .map(Node::root)
                .collect(Collectors.toList());
    }

    /**
     * Returns a sequential stream with the leaf nodes of this forest as its source.
     * @return
     */
    public Stream<N> fringe() {
        return leaves.stream().map(Node::getElement);
    }

    /**
     * Returns a view on the elements that follow <code>elem</code> in the ordering map.
     * @param elem an element of the ordering map
     * @return a view on the successors of <code>elem</code>
     * @throws NoSuchElementException if the ordering map does not contain <code>elem</code>
     * @throws NullPointerException if <code>elem</code> is <code>null</code>
     */
    public List<N> successor(N elem) {
        List<N> s = successors.get(Objects.requireNonNull(elem));
        if (s == null) throw new NoSuchElementException(String.format("element %s not found", elem));
        return s;
    }

    /**
     * Builds a sequential <code>Stream</code> of nodes whose parent is <code>n</code>,
     * and whose elements are obtained by invoking {@link #successor(Object)}
     * on the element stored in <code>n</code>.
     * @param n
     * @return
     */
    private Stream<Node<N>> buildChildren(Node<N> n) {
        return successor(n.getElement()).stream()
                .map(childElement -> new Node<>(childElement, n));
    }

    /**
     * Returns <code>true</code> if the fringe of this forest is empty.
     * @return
     */
    public boolean isEmpty() {
        return leaves.isEmpty();
    }

    /**
     * Expands the leaf nodes of this forest by one level.
     * For each node <code>n</code> in the fringe queue, the successor list is obtained
     * by invoking {@link #successor(Object)}; then, each successor is inserted at the tail of the fringe,
     * with an edge directed to <code>n</code>. If a node has no successors, the branch it belongs to is cut.
     *
     * <p>The following example shows how to perform a breadth-first expansion of the forest
     * while reading the fringe elements at each level:
     * <pre>{@code
     *      Stream<N> elements = ...
     *      LogicalSortedForest<N> forest = elements.collect(LogicalSortedForest.toLogicalSortedForest(...));
     *      for(; !forest.isEmpty(); forest.expand()) {
     *          Stream<N> f = forest.fringe();
     *      }
     * }</pre>
     *
     * <p>Only the first <i>K-1</i> invocations will produce new nodes, <i>K</i> being the size
     * of the ordering map. The <i>K-th</i> and subsequent invocations will be ineffective.
     */
    public void expand() {
        expand(branch -> false);
    }

    /**
     * Extends the functionality of {@link #expand()} with a visitor that performs a test
     * on each branch of the forest, before the corresponding leaf node is expanded.
     * The input <code>Predicate</code> receives the current branch (a sequential <code>Stream</code>
     * that starts with the current element and traverses the node chain towards the root)
     * and must return a boolean value: if <code>true</code>, the current branch
     * is removed from the fringe and is <em>not</em> expanded;
     * if <code>false</code>, the expansion proceeds normally.
     *
     * <p>This method aims for concurrent processing of leaf nodes. The input <code>Predicate</code>
     * must follow the same behavioral constraints specified by {@link Stream#filter(Predicate)}.
     * @param flagForRemoval a <em>non-interfering</em>, <em>stateless</em> predicate to apply to each branch
     *                       of the forest before expansion. If the return value is <code>true</code>,
     *                       the current branch is removed from the fringe and is <em>not</em> expanded;
     *                       if <code>false</code>, the expansion proceeds normally.
     */
    public void expand(Predicate<Stream<N>> flagForRemoval) {
        Objects.requireNonNull(flagForRemoval);
        leaves = leaves.parallelStream()
                .filter(leaf -> !flagForRemoval.test(leaf.getReachable()))
                .flatMap(this::buildChildren)
                .collect(Collectors.toList());
    }

    /**
     * Equivalent to {@link #expand(Predicate)}, with the exception that leaf nodes are processed
     * one at a time, in the encountered order.
     * Consequently, the input <code>Predicate</code> is not restricted by behavioral constraints.
     * @param flagForRemoval
     */
    public void expandOrdered(Predicate<Stream<N>> flagForRemoval) {
        Objects.requireNonNull(flagForRemoval);
        Stream.Builder<Node<N>> newLeaves = Stream.builder();
        for (Node<N> leaf : leaves) {
            if (!flagForRemoval.test(leaf.getReachable())) {
                buildChildren(leaf).forEachOrdered(newLeaves);
            }
        }
        leaves = newLeaves.build().collect(Collectors.toList());
    }

    /**
     * Returns a <code>Collector</code> that builds a <code>LogicalSortedForest</code> based on
     * an <em>ordering map</em> whose keys are the input elements, and whose values are the result
     * of applying <code>complementOf</code> to each input element.
     *
     * <p>If duplicates are found among the input elements, an {@link IllegalStateException} is thrown.
     * @param complementOf a mapping function that produces the logical complement of an input element
     * @param <T> the type of the input elements
     * @return
     */
    public static <T> Collector<T, ?, LogicalSortedForest<T>> toLogicalSortedForest(UnaryOperator<T> complementOf) {
        Objects.requireNonNull(complementOf);
        return Collectors.collectingAndThen(
                Collectors.toMap(
                        Function.identity(), complementOf,
                        (key, key2) -> { throw new IllegalStateException(String.format("duplicate key '%s'", key)); },
                        LinkedHashMap::new),
                orderingMap -> new LogicalSortedForest<T>(ImmutableMap.copyOf(orderingMap)));
    }

    /**
     * A tree node that stores a reference to its parent.
     */
    private static class Node<N> {
        private Node<N> parent;
        private N element;

        /**
         * Constructs a <code>Node</code> that stores an element and a reference to a parent node.
         * The <code>parent</code> argument may be <code>null</code> to allow the construction of a root node.
         * @param element the object stored in this node
         * @param parent the parent node. If <code>null</code>, the node being constructed is a root node.
         * @throws NullPointerException if <code>element</code> is <code>null</code>
         */
        public Node(N element, Node<N> parent) {
            this.element = Objects.requireNonNull(element);
            this.parent = parent;
        }

        public N getElement() {
            return element;
        }

        /**
         * Returns an <code>Optional</code> containing the parent node.
         * If the returned <code>Optional</code> is empty, the calling object is a root node.
         * @return
         */
        public Optional<Node<N>> getParent() {
            return Optional.ofNullable(parent);
        }

        /**
         * Returns a sequential <code>Stream</code> that starts with the current node element
         * and traverses the node chain towards the root.
         * If this node is a leaf, the returned <code>Stream</code> contains the elements
         * of the current branch, in reverse order.
         * @return
         */
        public Stream<N> getReachable() {
            Stream.Builder<N> builder = Stream.<N>builder().add(element);
            Optional<Node<N>> parent = getParent();
            while (parent.isPresent()) {
                Node<N> n = parent.get();
                builder.accept(n.element);
                parent = n.getParent();
            }
            return builder.build();
        }

        /**
         * Constructs a <code>Node</code> that stores the specified element, with a <code>null</code> parent.
         * @param element
         * @param <T>
         * @return
         */
        public static <T> Node<T> root(T element) {
            return new Node<>(element, null);
        }

        /**
         * Returns <code>true</code> if <code>o</code> is a <code>Node</code> and
         * <pre>{@code this.getElement().equals(o.getElement())}</pre>
         * otherwise returns <code>false</code>.
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node<?> node = (Node<?>) o;
            return element.equals(node.element);
        }

        @Override
        public int hashCode() {
            return element.hashCode();
        }
    }
}
