package it.poliba.enasca.ontocpnets.util;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base interface for preference forests.
 *
 * <p>Given an input set
 * <pre>Q = {p1, p2, &hellip; pN}</pre>
 * where each <em>p</em> is a propositional variable of type <code>P</code>, a preference forest
 * is a collection of rooted trees that generates a family of subsets of
 * <pre>F = {p1, ¬p1, p2, ¬p2, &hellip; pN, ¬pN}</pre>
 * At level <em>k &lt; N</em>, the forest generates the <em>k-subsets</em> of <em>F</em>,
 * excluding the ones in which a propositional variable <em>p</em> appears
 * as both <em>p</em> and <em>¬p</em>.
 *
 * <p>After the <em>N</em>-th expansion, the forest will be empty.
 *
 * <p>The distinctive feature of a preference forest is the ability to control the expansion process
 * by filtering branches. See {@link #expand(Predicate)} and {@link #expandOrdered(Predicate)}
 * for more information.
 *
 * <p>The following snippet shows how to fully expand a forest while retrieving
 * the list of branches at each level:
 * <pre>{@code
 *      while(!forest.isEmpty()) {
 *          List<S> branches = forest.branches();
 *          forest.expand();
 *      }
 * }</pre>
 * @param <P> the type of elements representing propositional variables
 * @param <S> a stream type representing a branch
 */
public interface BasePreferenceForest<P, S extends BaseStream<P, S>> {
    /**
     * Returns the leaf nodes.
     * @return
     */
    Stream<? extends BaseNode<P, S>> leaves();

    /**
     * Returns the list of branches.
     * @return
     */
    default List<S> branches() {
        return leaves().map(BaseNode::getReachable)
                .collect(Collectors.toList());
    }

    /**
     * Returns <code>true</code> if all branches have been cut.
     * @return
     */
    default boolean isEmpty() {
        return !leaves().findAny().isPresent();
    }

    /**
     * Equivalent to
     * <pre>{@code expand(branch -> true); }</pre>
     */
    default void expand() {
        expand(branch -> true);
    }

    /**
     * Expands each eligible branch by one level.
     * A branch is eligible for expansion if it satisfies both of the following conditions:
     * <ul>
     *     <li>applying the input <code>Predicate</code> on the branch produces <code>true</code>;</li>
     *     <li>the branch has reached its maximum length.</li>
     * </ul>
     * If a branch is not eligible for expansion, it is cut from the forest.
     *
     * <p>After <em>k</em> invocations of this method on a forest constructed
     * from the propositional variables
     * <pre>p1, p2, &hellip; pN</pre>
     * {@link #branches()} will return the <em>k-subsets</em> of
     * <pre>p1, ¬p1, p2, ¬p2, &hellip; pN, ¬pN</pre>
     * excluding: <em>a)</em> the ones in which a propositional variable <em>p</em> appears
     * as both <em>p</em> and <em>¬p</em>; and <em>b)</em> the ones that were filtered out
     * by the input <code>Predicate</code>s
     *
     * <p>After the <em>N</em>-th and subsequent invocations, {@link #isEmpty()}
     * will always evaluate to <code>true</code>.
     *
     * <p>This method aims for concurrent processing of the branches.
     * The input <code>Predicate</code> must follow the same behavioral constraints
     * specified by {@link Stream#filter(Predicate)}.
     * @param branchFilter a <em>non-interfering</em>, <em>stateless</em> predicate to apply
     *                     to each branch of the forest before expansion. If <code>false</code>,
     *                     the branch is cut from the forest.
     */
    void expand(Predicate<S> branchFilter);

    /**
     * Equivalent to
     * <pre>{@code expandOrdered(branch -> true); }</pre>
     */
    default void expandOrdered() {
        expandOrdered(branch -> true);
    }

    /**
     * Equivalent to {@link #expand(Predicate)}, with the exception that
     * branches are processed one at a time, in the encountered order.
     * Consequently, the input <code>Predicate</code> is not restricted by behavioral constraints.
     * @param branchFilter
     */
    void expandOrdered(Predicate<S> branchFilter);

    /**
     * A node that stores an element of type {@link P} and a reference to its parent.
     * @param <P> the type of elements representing propositional variables
     * @param <S> a stream type representing a branch
     */
    interface BaseNode<P, S extends BaseStream<P, S>> {
        /**
         * Returns an <code>Optional</code> containing the parent node.
         * If the returned <code>Optional</code> is empty, the calling object is a root node.
         * @return
         */
        Optional<? extends BaseNode<P, S>> getParent();

        /**
         * Returns a sequence of elements of type {@link P}, starting with the element
         * of the current node and traversing the node chain towards the root.
         * If this node is a leaf, the returned sequence represents the current branch.
         * @return
         */
        S getReachable();

        /**
         * Builds a sequential <code>Stream</code> of children of this node.
         * @return
         */
        Stream<? extends BaseNode<P, S>> children();
    }

}
