package it.poliba.enasca.ontocpnets.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A specialized implementation of <code>BasePreferenceForest</code>
 * for preference variables represented as DIMACS literals.
 */
public class IntPreferenceForest extends AbstractPreferenceForest<Integer, IntStream>
        implements BasePreferenceForest<Integer, IntStream> {
    private int maxLiteral;

    /**
     * Constructs an <code>IntPreferenceForest</code> from the following set of DIMACS literals:
     * <pre>1, -1, 2, -2, &hellip; maxLiteral, -maxLiteral</pre>
     * @param maxLiteral
     * @throws IllegalArgumentException if <code>maxLiteral &lt; 1</code>
     */
    public IntPreferenceForest(int maxLiteral) {
        if (maxLiteral < 1) throw new IllegalArgumentException();
        this.maxLiteral = maxLiteral;
        leaves = literals(1, maxLiteral)
                .mapToObj(Node::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns the successors of <code>elem</code>.
     * @param elem
     * @return
     * @throws NoSuchElementException if <code>elem</code> is not handled by this forest
     */
    public IntStream successor(int elem) {
        if (elem == 0) {
            throw new NoSuchElementException("invalid DIMACS literal: 0");
        }
        int positive = elem > 0 ? elem : -elem;
        if (positive == maxLiteral) {
            return IntStream.empty();
        }
        if (positive > maxLiteral) {
            throw new NoSuchElementException(
                    String.format("element %d is not handled by this forest", elem));
        }
        return literals(positive + 1, maxLiteral);
    }

    /**
     * Generates an <code>IntStream</code> of
     * <pre>start, -start, start+1, -(start+1), &hellip; end, -end</pre>
     * @param start a positive DIMACS literal
     * @param end a positive DIMACS literal such that <code>end &gt; start</code>
     * @return
     */
    private static IntStream literals(int start, int end) {
        if (start < 1 || end < start) throw new IllegalArgumentException();
        return IntStream.iterate(start, n -> (n > 0) ? -n : ((-n) + 1))
                .limit((1 + end - start) * 2);
    }

    /**
     * A node that stores a DIMACS literal and a reference to its parent.
     */
    public class Node implements BaseNode<Integer, IntStream> {
        private Node parent;
        private int element;

        /**
         * Constructs a <code>Node</code> that stores a DIMACS literal
         * and a reference to its parent.
         * @param element the element stored in this node
         * @param parent the parent node
         * @throws IllegalArgumentException if <code>element</code> is 0
         */
        private Node(int element, Node parent) {
            this(element);
            this.parent = Objects.requireNonNull(parent);
        }

        /**
         * Constructs a root <code>Node</code> that stores the specified DIMACS literal.
         * @param element
         * @throws IllegalArgumentException if <code>element</code> is 0
         */
        private Node(int element) {
            if (element == 0) throw new IllegalArgumentException("invalid element: 0");
            this.element = element;
            parent = null;
        }

        /**
         * Returns an <code>Optional</code> containing the parent node.
         * If the returned <code>Optional</code> is empty, the calling object is a root node.
         * @return
         */
        @Override
        public Optional<Node> getParent() {
            return Optional.ofNullable(parent);
        }

        /**
         * Returns a sequential <code>IntStream</code> that starts with the element
         * of the current node and traverses the node chain towards the root.
         * If this node is a leaf, the returned <code>IntStream</code> contains the elements
         * of the current branch, in reverse order.
         * @return
         */
        @Override
        public IntStream getReachable() {
            IntStream.Builder builder = IntStream.builder().add(element);
            Optional<Node> parent = getParent();
            while (parent.isPresent()) {
                Node n = parent.get();
                builder.accept(n.element);
                parent = n.getParent();
            }
            return builder.build();
        }

        @Override
        public Stream<Node> children() {
            return successor(element)
                    .mapToObj(childElement -> new Node(childElement, this));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node other = (Node) o;
            return element == other.element;
        }

        @Override
        public int hashCode() {
            return element;
        }
    }
}
