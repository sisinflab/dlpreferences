package it.poliba.enasca.ontocpnets.tree;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A skeletal implementation of {@link BasePreferenceForest}.
 */
public abstract class AbstractPreferenceForest<P, S extends BaseStream<P, S>>
        implements BasePreferenceForest<P, S> {
    /**
     * The list of leaf nodes.
     */
    protected List<BaseNode<P, S>> leaves;

    @Override
    public List<S> branches() {
        return Lists.transform(leaves, BaseNode::getReachable);
    }

    @Override
    public int size() {
        return leaves.size();
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
    public void expand(boolean[] mask) {
        Objects.requireNonNull(mask);
        if (mask.length != leaves.size()) {
            throw new IllegalArgumentException();
        }
        leaves = IntStream.range(0, mask.length).parallel()
                .filter(i -> mask[i])
                .mapToObj(i -> leaves.get(i))
                .flatMap(BaseNode::children)
                .collect(Collectors.toList());
    }
}
