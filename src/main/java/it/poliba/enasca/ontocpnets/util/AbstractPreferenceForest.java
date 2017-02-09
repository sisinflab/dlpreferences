package it.poliba.enasca.ontocpnets.util;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A skeletal implementation of {@link BasePreferenceForest}.
 */
abstract class AbstractPreferenceForest<P, S extends BaseStream<P, S>>
        implements BasePreferenceForest<P, S> {
    /**
     * The list of leaf nodes.
     */
    protected List<? extends BaseNode<P, S>> leaves;

    @Override
    public Stream<? extends BaseNode<P, S>> leaves() {
        return leaves.stream();
    }

    @Override
    public boolean isEmpty() {
        return leaves.isEmpty();
    }

    @Override
    public void expand(Predicate<S> branchFilter) {
        Objects.requireNonNull(branchFilter);
        leaves = leaves.parallelStream()
                .filter(leaf -> branchFilter.test(leaf.getReachable()))
                .flatMap(BaseNode::children)
                .collect(Collectors.toList());
    }

    @Override
    public void expandOrdered(Predicate<S> branchFilter) {
        Objects.requireNonNull(branchFilter);
        Stream.Builder<BaseNode<P, S>> newLeaves = Stream.builder();
        for (BaseNode<P, S> leaf : leaves) {
            if (branchFilter.test(leaf.getReachable())) {
                leaf.children().forEachOrdered(newLeaves);
            }
        }
        leaves = newLeaves.build().collect(Collectors.toList());
    }
}
