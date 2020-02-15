package se.kth.spork.merge;

/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2016 Floréal Morandat <florealm@gmail.com>
 */

import com.github.gumtreediff.tree.ITree;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * This file is adapted from GumTree, see the original license statement above. The only non-trivial thing that
 * is borrowed is the fromTree method, everything else is either trivial or original.
 *
 * Representation of a Parent/Child/Successor triple. Note that the revision does not (and should not) impact hashing
 * or equality, it is just there as metainformation.
 */
public class Pcs<T> {
    private T root;
    private T predecessor;
    private T successor;
    private Revision revision;
    private Function<T, String> toStr;
    private Function<T, Integer> hash;

    public Pcs(T root, T predecessor, T successor) {
        this(root, predecessor, successor, T::toString, T::hashCode);
    }

    public Pcs(T root, T predecessor, T successor, Function<T, String> toStr, Function<T, Integer> hash) {
        this.root = root;
        this.predecessor = predecessor;
        this.successor = successor;
        revision = null;
        this.toStr = toStr;
        this.hash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pcs pcs = (Pcs) o;
        return Objects.equals(root, pcs.root)
                && Objects.equals(predecessor, pcs.predecessor)
                && Objects.equals(successor, pcs.successor);
    }

    @Override
    public String toString() {
        return "PCS(" + (revision != null ? revision + "," : "")
                + applyToStr(root) + ","
                + applyToStr(predecessor) + ","
                + applyToStr(successor) + ")";
    }

    private String applyToStr(T elem) {
        if (elem == null)
            return "null";
        return toStr.apply(elem);
    }

    public T getRoot() {
        return root;
    }

    public T getPredecessor() {
        return predecessor;
    }

    public T getSuccessor() {
        return successor;
    }

    public Revision getRevision() {
        return revision;
    }

    public void setRevision(Revision revision) {
        this.revision = revision;
    }

    private int applyHash(T elem) {
        return elem == null ? 0 : hash.apply(elem);
    }

    @Override
    public int hashCode() {
        int rootHash = applyHash(root);
        int predecessorHash = applyHash(predecessor);
        int successorHash = applyHash(successor);
        return Objects.hash(rootHash, predecessorHash, successorHash);
    }

}