package it.poliba.enasca.ontocpnets.tree;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Stream.Builder<BaseNode<P, S>> builder = Stream.builder();
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) leaves.get(i).children().forEachOrdered(builder);
        }
        leaves = builder.build().collect(Collectors.toList());
    }
}
