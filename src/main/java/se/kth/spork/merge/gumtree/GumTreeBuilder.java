package se.kth.spork.merge.gumtree;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import se.kth.spork.merge.Content;
import se.kth.spork.merge.Pcs;

import java.util.*;
import java.util.function.Consumer;

/**
 * Builds GumTree trees from (assumed to be) merged PCS structures.
 *
 * TODO handle conflicts
 *
 * @author Simon Larsén
 */
public class GumTreeBuilder {

    /**
     * Transform a well-formed PCS structure to a GumTree ITree. To be well-formed, the PCS structure must have
     * precisely one PCS triple with root=null.
     *
     * @param pcses A PCS structure.
     * @param nodeContents The contents associated with the predecessors of each PCS triple.
     * @return A tree representing the PCS structure.
     */
    public static ITree pcsToGumTree(Set<Pcs<ITree>> pcses, Map<ITree, Set<Content<ITree>>> nodeContents) {
        Builder builder = new Builder(nodeContents);
        traversePcs(pcses, builder);

        ITree root = builder.currentRoot;
        while (root != null && root.getParent() != null) {
            root = root.getParent();
        }
        root.refresh();

        return root;
    }

    /**
     * Visits each node twice. In the first visit, the node is a child of the previously visited root, and in the
     * second visit, it is the root of the current subtree.
     *
     * Consequently, the first node(s) to be visited are children of a virtual root node.
     *
     * @param pcses A well-formed PCS structure.
     * @param func A function to apply to the nodes in the PCS structure.
     */
    public static <T> void traversePcs(Set<Pcs<T>> pcses, Consumer<T> func) {
        Map<T, Map<T, Pcs<T>>> rootToChildren = new HashMap<>();
        for (Pcs<T> pcs : pcses) {
            Map<T, Pcs<T>> children = rootToChildren.getOrDefault(pcs.getRoot(), new HashMap<>());
            if (children.isEmpty()) rootToChildren.put(pcs.getRoot(), children);
            children.put(pcs.getPredecessor(), pcs);
        }

        traversePcs(rootToChildren, null, func);
    }

    private static <T> void traversePcs(Map<T, Map<T, Pcs<T>>> rootToChildren, T currentRoot, Consumer<T> func) {
        if (currentRoot != null)
            func.accept(currentRoot);

        Map<T, Pcs<T>> children = rootToChildren.get(currentRoot);
        T pred = null;
        List<T> sortedChildren = new ArrayList<>();
        while (true) {
            Pcs<T> nextPcs = children.get(pred);
            pred = nextPcs.getSuccessor();
            if (pred == null) {
                break;
            }
            sortedChildren.add(pred);
            func.accept(pred);
        };
        sortedChildren.forEach(child -> traversePcs(rootToChildren, child, func));
    }

    private static class Builder implements Consumer<ITree> {
        private ITree currentRoot;
        private Map<ITree, ITree> nodes;
        private Map<ITree, Set<Content<ITree>>> nodeContents;

        private Builder(Map<ITree, Set<Content<ITree>>> nodeContents) {
            nodes = new HashMap<>();
            this.nodeContents = nodeContents;
        }

        @Override
        public void accept(ITree tree) {
            ITree treeCopy = nodes.get(tree);

            if (treeCopy == null) { // first time we see this node; it's a child node of the current root
                Set<Content<ITree>> contents = nodeContents.get(tree);
                assert contents.size() == 1;
                Content<ITree> cont = contents.iterator().next();

                treeCopy = copyTree(tree, cont, currentRoot);

                if (currentRoot != null)
                    currentRoot.addChild(treeCopy);

                nodes.put(tree, treeCopy);
            } else { // second time we see this node; it's now the root
                currentRoot = treeCopy;
            }
        }

        private static ITree copyTree(ITree tree, Content<ITree> content, ITree root) {
            ITree treeCopy = new Tree(tree.getType(), (String) content.getValue());
            treeCopy.setParent(root);
            // TODO remove this cast, the content is always a String
            tree.getMetadata().forEachRemaining(entry -> treeCopy.setMetadata(entry.getKey(), entry.getValue()));
            return treeCopy;
        }
    }

}