package se.kth.spork.merge.spoon;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.utils.Pair;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class for storing matches between tree nodes in two Spoon trees. Inspired by the MappingStore class from gumtree.
 *
 * @author Simon Larsén
 */
public class SpoonMapping {
    private static final String KEY_METADATA = "spoon_mapping_key";

    private Map<CtWrapper, CtWrapper> srcs;
    private Map<CtWrapper, CtWrapper> dsts;


    private SpoonMapping() {
        srcs = new HashMap<>();
        dsts = new HashMap<>();
    }

    /**
     * Create a Spoon mapping from a GumTree mapping. Every GumTree node must have a "spoon_object" metadata object that
     * refers back to a Spoon node. As this mapping does not cover the whole Spoon tree, additional mappings are
     * inferred.
     *
     * TODO verify that the mapping inference is actually correct
     *
     * @param gumtreeMapping A GumTree mapping in which each mapped node has a "spoon_object" metadata object.
     * @return A SpoonMapping corresponding to the passed GumTree mapping.
     */
    public static SpoonMapping fromGumTreeMapping(MappingStore gumtreeMapping) {
        SpoonMapping mapping = new SpoonMapping();

        for (Mapping m : gumtreeMapping.asSet()) {
            CtElement spoonSrc = getSpoonNode(m.first);
            CtElement spoonDst = getSpoonNode(m.second);
            if (spoonSrc == null || spoonDst == null) {
                if (spoonSrc != spoonDst) { // at least one was non-null
                    throw new IllegalStateException();
                }
                if (m.first.getType() != -1) { // -1 is the type given to root node in SpoonGumTreeBuilder
                    throw new IllegalStateException("non-root node " + m.first.toShortString()
                            + " had no mapped Spoon object");
                }
            } else {
                mapping.put(spoonSrc, spoonDst);
            }
        }

        mapping.inferAdditionalMappings(mapping.asList());
        return mapping;
    }

    private List<Pair<CtElement, CtElement>> asList() {
        return srcs.values().stream()
                .map(dst -> new Pair<>(getSrc(dst).getElement(), dst.getElement()))
                .collect(Collectors.toList());
    }

    private void inferAdditionalMappings(List<Pair<CtElement, CtElement>> matches) {
        while (!matches.isEmpty()) {
            List<Pair<CtElement, CtElement>> newMatches = new ArrayList<>();
            for (CtWrapper dst : new ArrayList<>(srcs.values())) {
                CtWrapper src = getSrc(dst);
                newMatches.addAll(inferAdditionalMappings(src.getElement(), dst.getElement()));
            }
            matches = newMatches;
        }
    }

    private List<Pair<CtElement, CtElement>> inferAdditionalMappings(CtElement src, CtElement dst) {
        List<CtElement> srcChildren = src.getDirectChildren();
        List<CtElement> dstChildren = dst.getDirectChildren();
        List<Pair<CtElement, CtElement>> newMatches = new ArrayList<>();

        int srcIdx = 0;
        int dstIdx = 0;

        while (srcIdx < srcChildren.size() && dstIdx < dstChildren.size()) {
            CtElement srcChild = srcChildren.get(srcIdx);
            CtElement dstChild = dstChildren.get(dstIdx);

            if (hasSrc(srcChild) || !isToIgnore(srcChild)) {
                srcIdx++;
            } else if (hasDst(dstChild) || !isToIgnore(dstChild)) {
                dstIdx++;
            } else {
                //assert srcChild.equals(dstChild);
                assert srcChild.getClass() == dstChild.getClass();

                put(srcChild, dstChild);
                newMatches.add(new Pair<>(srcChild, dstChild));
            }
        }

        return newMatches;
    }

    /**
     * This method is taken from gumtree-spoon-ast-diff and identifies Spoon nodes that should not be mapped into
     * the GumTree that's build by SpoonGumTreeBuilder.
     *
     * @author Mattias Martinez
     *
     * See <a href="https://github.com/SpoonLabs/gumtree-spoon-ast-diff/blob/dae908192bee7773b38d149baff831ee616ec524/src/main/java/gumtree/spoon/builder/TreeScanner.java#L71-L84">TreeScanner</a>
     * for the original source.
     *
     * TODO Don't duplicate this code...
     *
     * @param element An element to check if it is to be ignored.
     * @return Whether or not to ignore the argument.
     */
    private boolean isToIgnore(CtElement element) {
        if (element instanceof CtStatementList && !(element instanceof CtCase)) {
            return element.getRoleInParent() != CtRole.ELSE && element.getRoleInParent() != CtRole.THEN;
        }

        if (element instanceof CtReference && element.getRoleInParent() == CtRole.SUPER_TYPE) {
            return false;
        }

        return element.isImplicit() || element instanceof CtReference;
    }

    public boolean hasSrc(CtWrapper src) {
        return srcs.containsKey(src);
    }

    public boolean hasDst(CtWrapper dst) {
        return dsts.containsKey(dst);
    }

    public boolean hasSrc(CtElement src) {
        return hasSrc(WrapperFactory.wrap(src));
    }

    public boolean hasDst(CtElement dst) {
        return hasDst(WrapperFactory.wrap(dst));
    }

    public CtWrapper getDst(CtWrapper src) {
        return srcs.get(src);
    }

    public CtElement getDst(CtElement src) {
        return getDst(WrapperFactory.wrap(src)).getElement();
    }

    public CtWrapper getSrc(CtWrapper dst) {
        return dsts.get(dst);
    }

    public CtElement getSrc(CtElement dst) {
        return getSrc(WrapperFactory.wrap(dst)).getElement();
    }

    public void put(CtElement src, CtElement dst) {
        put(WrapperFactory.wrap(src), WrapperFactory.wrap(dst));
    }

    public void put(CtWrapper src, CtWrapper dst) {
        srcs.put(src, dst);
        dsts.put(dst, src);
    }

    private static CtElement getSpoonNode(ITree gumtreeNode) {
        return (CtElement) gumtreeNode.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
    }

    private String formatEntry(Map.Entry<CtWrapper, CtWrapper> entry) {
        return "(" + entry.getKey() + ", " + entry.getValue() + ")";
    }

    @Override
    public String toString() {
        return "SpoonMappingStore{" +
                "srcs=" + srcs.entrySet().stream().map(this::formatEntry).collect(Collectors.toList()) +
                ", dsts=" + dsts.entrySet().stream().map(this::formatEntry).collect(Collectors.toList()) +
                '}';
    }
}