/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class OffsetKeyedTreeMap<T> {
    
    @FunctionalInterface
    public interface RemappingFunction<T> {
        T apply(int pos, T newValue, T oldValue);
    }
    
    public interface NodesIterator<T> {
        int getCurrOffset();
        T getCurrValue();
        
        boolean next();
        boolean prev();
    }
    
    private static class NodeAndOffset<T> {
        public final Node<T> node;
        public final int offset;
        
        public NodeAndOffset(Node<T> node, int offset) {
            this.node = node;
            this.offset = offset;
        }
    }
    
    private static class NodeAndParentAtOffset<T> extends NodeAndOffset<T> {
        public final Node<T> parent;
        public final boolean isLeft;
        
        public NodeAndParentAtOffset(Node<T> parent, Node<T> node, boolean isLeft, int offset) {
            super(node, offset);
            this.parent = parent;
            this.isLeft = isLeft;
        }
    }
    
    private static class Subtree<T> {
        public final Node<T> node;
        public final int size;
        
        public Subtree(Node<T> node, int size) {
            super();
            this.node = node;
            this.size = size;
        }
    }
    
    public static class ValueAndOffset<T> {
        public final T value;
        public final int offset;
        
        public ValueAndOffset(T value, int offset) {
            this.value = value;
            this.offset = offset;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Node<T> sentinel() {
        return (Node<T>)sentinel();
    }

    private static class Node<T> {
        public static final Node<?> SENTINEL = new Node<>(0, null, false, null, null);

        public boolean isSentinel() {
            return this == SENTINEL;
        }
        public boolean isNotSentinel() {
            return this != SENTINEL;
        }

        public int blackHeight = 0;
        public boolean isRed = false;
                
        public Node<T> left = null;
        public Node<T> right = null;
        public Node<T> parent = null;

        public int offset = 0;
        public T content;
        
        public Node(int offset, T content, boolean isRed, Node<T> left, Node<T> right) {
            this.left = left != null ? left : sentinel();
            this.right = right != null ? right : sentinel();
            this.offset = offset;
            this.content = content;
            this.isRed = isRed;
            this.parent = null;
        }

        public void refreshBlackHeight() {
            if (this.isNotSentinel()) {
                this.blackHeight = Math.max(this.left.blackHeight, this.right.blackHeight) + (this.isRed ? 0 : 1);
            }
        }

        public void refreshParentRefs() {
            if (this.left.isNotSentinel()) {
                this.left.parent = this;
            }
            if (this.right.isNotSentinel()) {
                this.right.parent = this;
            }
        }
    }

    private Node<T> root;
    private int size;

    public OffsetKeyedTreeMap() {
        this.root = sentinel();
        this.size = 0;
    }

    private OffsetKeyedTreeMap(Node<T> root, int size) {
        this.root = root;
        this.size = size;
        this.FixupRoot();
    }

    public void clear() {
        this.root = sentinel();
        this.size = 0;
    }

    private NodeAndParentAtOffset<T> findImpl(int pos) {
        int relPos = pos;
        Node<T> parent = this.root;
        boolean isLeft = false;
        
        var node = root;
        while (node.isNotSentinel()) {
            parent = node;
            if (relPos < node.offset) {
                node = node.left;
                isLeft = true;
            } else if (relPos > node.offset) {
                relPos -= node.offset;
                node = node.right;
                isLeft = false;
            } else {
                break;
            }
        }
        
        return new NodeAndParentAtOffset<>(node, parent, isLeft, relPos);
    }
    
    public T find(int position) {
        NodeAndParentAtOffset<T> result = this.findImpl(position);
        return result.node.isNotSentinel() ? result.node.content : null;
    }
    
    public T put(int pos, T value, RemappingFunction<T> remappingFunction) {
        T result;

        if (this.root.isSentinel()) {
            this.root = new Node<>(pos, value, false, null, null);
            this.root.refreshBlackHeight();
            this.size++;
            result = value;
        } else {
            NodeAndParentAtOffset<T> location = this.findImpl(pos);
            if (location.node.isNotSentinel()) {
                if (remappingFunction == null) {
                    result = value;
                } else {
                    result = remappingFunction.apply(pos, value, location.node.content);
                }
                location.node.content = result;
            } else {
                Node<T> newNode = new Node<>(location.offset, value, true, null, null);
                Node<T> parent = location.parent;
              
                if (location.isLeft) {
                    parent.left = newNode;
                    newNode.parent = parent;
                    this.size++;
                } else {
                    parent.right = newNode;
                    newNode.parent = parent;
                    this.size++;
                }
        
                this.RestoreAfterInsert(newNode);
                
                result = value;
            }
        }
        
        return result;
    }

    ///<summary>
    /// RestoreAfterInsert
    /// Additions to red-black trees usually destroy the red-black 
    /// properties. Examine the tree and restore. Rotations are normally 
    /// required to restore it
    ///</summary>
    private void RestoreAfterInsert(Node<T> x) {
        var xx = x;

        // x and y are used as variable names for brevity, in a more formal
        // implementation, you should probably change the names

        Node<T> y;

        // maintain red-black tree properties after adding x
        while (x != this.root && x.parent.isRed) {
            x.left.refreshBlackHeight();
            x.right.refreshBlackHeight();
            x.refreshBlackHeight();

            // parent node is .Colored red; 
            if (x.parent == x.parent.parent.left) { // determine traversal path         
                                                    // is it on the left or right subtree?
                y = x.parent.parent.right;          // get uncle
                if (y != null && y.isRed) {   // uncle is red; change x's parent and uncle to black
                    x.parent.isRed = false;
                    y.isRed = false;
                    // grandparent must be red. Why? Every red node that is not 
                    // a leaf has only black children 
                    x.parent.parent.isRed = true;
                    x = x.parent.parent;    // continue loop with grandparent
                } else {
                    // uncle is black; determine if x is greater than parent
                    if (x == x.parent.right) {   // yes, x is greater than parent; rotate left
                        // make x a left child
                        x = x.parent;
                        this.rotateLeft(x);
                    }
                    // no, x is less than parent
                    x.parent.isRed = false;    // make parent black
                    x.parent.parent.isRed = true;       // make grandparent black
                    this.rotateRight(x.parent.parent);                   // rotate right
                }
            } else {   // x's parent is on the right subtree
                       // this code is the same as above with "left" and "right" swapped
                y = x.parent.parent.left;
                if (y != null && y.isRed) {
                    x.parent.isRed = false;
                    y.isRed = false;
                    x.parent.parent.isRed = true;
                    x = x.parent.parent;
                } else {
                    if (x == x.parent.left) {
                        x = x.parent;
                        this.rotateRight(x);
                    }
                    x.parent.isRed = false;
                    x.parent.parent.isRed = true;
                    this.rotateLeft(x.parent.parent);
                }
            }
        }

        if (xx != this.root) {
            do {
                xx.left.refreshBlackHeight();
                xx.right.refreshBlackHeight();
                xx.refreshBlackHeight();
                xx = xx.parent;
            } while (xx != this.root);
        }

        this.FixupRoot();
    }

    private void FixupRoot() {
        if (this.root.isNotSentinel()) {
            this.root.parent = null;
            this.root.isRed = false;
            this.root.left.refreshBlackHeight();
            this.root.right.refreshBlackHeight();;
            this.root.refreshBlackHeight();
        }
    }

    ///<summary>
    /// RotateLeft
    /// Rebalance the tree by rotating the nodes to the left
    ///</summary>
    private Node<T> rotateLeft(Node<T> x) {
        // pushing node x down and to the left to balance the tree. x's right child (y)
        // replaces x (since y > x), and y's left child becomes x's right child 
        // (since it's < y but > x).
        
        

        Node<T> y = x.right;           // get x's right node, this becomes y

        // set x's right link
        x.right = y.left;                   // y's left child's becomes x's right child

        // modify parents
        if (y.left.isNotSentinel()) {
            y.left.parent = x;              // sets y's left parent to x
        }

        if (y.isNotSentinel()) {
            y.parent = x.parent;            // set y's parent to x's parent
        }

        if (x.parent != null) {   // determine which side of it's parent x was on
            if (x == x.parent.left) {
                x.parent.left = y;          // set left parent to y
            } else {
                x.parent.right = y;         // set right parent to y
            }
        } else if (this.root == x) {
            this.root.parent = null;
            this.root = y;                  // at rbTree, set it to y
        }

        // link x and y 
        y.left = x;                         // put x on y's left 
        if (x.isNotSentinel()) {            // set y as x's parent
            x.parent = y;
        }

        x.refreshBlackHeight();
        y.refreshBlackHeight();

        y.offset += x.offset;
        
        return y;
    }

    ///<summary>
    /// RotateRight
    /// Rebalance the tree by rotating the nodes to the right
    ///</summary>
    private Node<T> rotateRight(Node<T> x) {
        // pushing node x down and to the right to balance the tree. x's left child (y)
        // replaces x (since x < y), and y's right child becomes x's left child 
        // (since it's < x but > y).

        Node<T> y = x.left;            // get x's left node, this becomes y

        // set x's right link
        x.left = y.right;                   // y's right child becomes x's left child

        // modify parents
        if (y.right.isNotSentinel()) {
            y.right.parent = x;             // sets y's right parent to x
        }

        if (y.isNotSentinel()) {
            y.parent = x.parent;            // set y's parent to x's parent
        }

        if (x.parent != null) {             // null=rbTree, could also have used rbTree
            // determine which side of it's parent x was on
            if (x == x.parent.right) {
                x.parent.right = y;         // set right parent to y
            } else {
                x.parent.left = y;          // set left parent to y
            }
        } else if (this.root == x) {
            this.root.parent = null;
            this.root = y;                     // at rbTree, set it to y
        }

        // link x and y 
        y.right = x;                        // put x on y's right
        if (x.isNotSentinel()) {            // set y as x's parent
            x.parent = y;
        }

        x.refreshBlackHeight();
        y.refreshBlackHeight();
        
        x.offset -= y.offset;
        if (x.offset < 0) { // should never happen due to consistency rules 
            throw new IllegalStateException("relative offsets invariant being positive violated during balancing rotation procedure");
        }
        
        return y;
    }

    public NodesIterator<T> nodesIteratorAt(int position) {
        NodeAndParentAtOffset<T> initialLocation = this.findImpl(position);
        return switch(this.size) {
            case 0 -> new NodesIterator<T>() {
                @Override
                public T getCurrValue() {
                    return null;
                }

                @Override
                public int getCurrOffset() {
                    return 0;
                }

                @Override
                public boolean prev() {
                    return false;
                }

                @Override
                public boolean next() {
                    return false;
                }
            };
            case 1 -> new NodesIterator<T>() {
                boolean beforeFirst = position < OffsetKeyedTreeMap.this.root.offset;
                boolean afterLast = position > OffsetKeyedTreeMap.this.root.offset;
                Node<T> theOnlyNode = OffsetKeyedTreeMap.this.root;
                @Override
                public T getCurrValue() {
                    return !this.beforeFirst && !this.afterLast ? this.theOnlyNode.content : null;
                }

                @Override
                public int getCurrOffset() {
                    return this.beforeFirst ? 0 : (this.afterLast ? Integer.MAX_VALUE : this.theOnlyNode.offset);
                }

                @Override
                public boolean prev() {
                    if (this.beforeFirst) {
                        return false;
                    } else if (this.afterLast) {
                        this.afterLast = false;
                        return true;
                    } else {
                        this.beforeFirst = true;
                        return false;
                    }
                }

                @Override
                public boolean next() {
                    if (this.afterLast) {
                        return false;
                    } else if (this.beforeFirst) {
                        this.beforeFirst = false;
                        return true;
                    } else {
                        this.afterLast = true;
                        return false;
                    }
                }
            };
            default -> new NodesIterator<T>() {
                boolean beforeFirst = false;
                boolean afterLast = false;
                boolean initial = true;
                NodeAndOffset<T> currentLocation = new NodeAndOffset<>(initialLocation.node.isSentinel() ? null : initialLocation.node, position);

                @Override
                public T getCurrValue() {
                    return this.currentLocation.node != null ? this.currentLocation.node.content : null;
                }

                @Override
                public int getCurrOffset() {
                    return this.currentLocation.offset;
                }

                @Override
                public boolean prev() {
                    if (this.initial && initialLocation.node.isSentinel()) {
                        // TODO validate position
                        NodeAndOffset<T> parentLocation = new NodeAndOffset<>(initialLocation.parent, position - initialLocation.offset);
                        this.currentLocation = initialLocation.isLeft ? findPrev(parentLocation) : parentLocation;
                    } else if (this.beforeFirst) {
                        return false;
                    } else if (this.afterLast) {
                        this.currentLocation = findLast(new NodeAndOffset<>(OffsetKeyedTreeMap.this.root, 0));
                        this.afterLast = false;
                    } else {
                        this.currentLocation = findPrev(this.currentLocation);
                    }
                    this.initial = false;
                    this.beforeFirst = this.currentLocation.node == null;
                    return this.currentLocation.node != null;
                }

                @Override
                public boolean next() {
                    if (this.initial && initialLocation.node.isSentinel()) {
                        // TODO validate position
                        NodeAndOffset<T> parentLocation = new NodeAndOffset<>(initialLocation.parent, position - initialLocation.offset);
                        this.currentLocation = initialLocation.isLeft ? parentLocation : findNext(parentLocation);
                    } else if (this.afterLast) {
                        return false;
                    } else if (this.beforeFirst) {
                        this.currentLocation = findFirst(new NodeAndOffset<>(OffsetKeyedTreeMap.this.root, 0));
                        this.beforeFirst = false;
                    } else {
                        this.currentLocation = findNext(this.currentLocation);
                    }
                    this.initial = false;
                    this.afterLast = this.currentLocation.node == null;
                    return this.currentLocation.node != null;
                }
            };
        };
    }

    private NodeAndOffset<T> findFirst(NodeAndOffset<T> location) {
        Node<T> node = location.node;
        int offset = location.offset;
        while (node.left.isNotSentinel()) {
            node = node.left;
        }
        return new NodeAndOffset<>(node, offset + node.offset);
    }
    
    private NodeAndOffset<T> findLast(NodeAndOffset<T> location) {
        Node<T> node = location.node;
        int offset = location.offset;
        while (node.right.isNotSentinel()) {
            offset += node.offset;
            node = node.right;
        }
        return new NodeAndOffset<>(node, offset + node.offset);
    }
    
    private NodeAndOffset<T> findPrev(NodeAndOffset<T> location) {
        Node<T> node = location.node;
        int offset = location.offset;
        Node<T> prev;
        if (node.left.isNotSentinel()) {
            prev = node.left;
            while (prev.right.isNotSentinel()) {
                offset += prev.offset;
                prev = prev.right;
            }
        } else {
            prev = node.parent;
            if (prev != null) {
                if (prev.right == node) {
                    offset -= node.offset;
                }
                while (prev.right != node) {
                    node = prev;
                    prev = node.parent;
                    if (prev.right == node) {
                        offset -= node.offset;
                    }
                }
            }
        }
        return new NodeAndOffset<>(prev, prev == null ? Integer.MIN_VALUE : offset);
    }
        
    private NodeAndOffset<T> findNext(NodeAndOffset<T> location) {
        Node<T> node = location.node;
        int offset = location.offset;
        Node<T> next;
        if (node.right.isNotSentinel()) {
            offset += node.offset;
            next = node.right;
            while (next.left.isNotSentinel()) {
                next = next.left;
            }
        } else {
            next = node.parent;
            if (next != null) {
                if (next.right == node) {
                    offset -= node.offset;
                }
                while (next != null && next.left != node) {
                    node = next;
                    next = node.parent;
                    if (next.right == node) {
                        offset -= node.offset;
                    }
                }
            }
        }
        return new NodeAndOffset<>(next, next == null ? Integer.MAX_VALUE : offset);
    }
    
    private static class FlatteningIterator<T> implements Iterator<T> {
        private final T node;
        private final Iterator<T> childrenIterator;
        private final Function<T, Iterable<T>> childrenSelector;
        
        private T current;
        private Iterator<T> expansionIterator;
        
        public FlatteningIterator(T node, Function<T, Iterable<T>> childrenSelector) { 
            this.node = node;
            this.childrenIterator = childrenSelector.apply(node).iterator();
            this.childrenSelector = childrenSelector;
            this.current = node;
            this.expansionIterator = null;
        }

        @Override
        public boolean hasNext() {
            return this.current != null;
        }
        
        public T next() {
            T result = this.current;
            if (expansionIterator != null && this.expansionIterator.hasNext()) {
                this.current = this.expansionIterator.next();
            } else if (childrenIterator.hasNext()) {
                do {
                    this.expansionIterator = flatten(childrenIterator.next(), this.childrenSelector).iterator();
                } while (!this.expansionIterator.hasNext() && childrenIterator.hasNext());
                this.current = this.expansionIterator.hasNext() ? this.expansionIterator.next() : null;
            } else {
                this.current = null;
            }
            return result;
        }
    }
    
    private static <T> Iterable<T> flatten(T node, Function<T, Iterable<T>> childrenSelector) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new FlatteningIterator<>(node, childrenSelector);
            }
        };
    }

    private static <T> NodeAndOffset<T> evaluateNodeBlackHeight(Node<T> node, int p) {
        return new NodeAndOffset<>(node, p + (node.isRed ? 0 : 1));
    }

    public int validateBlackHeights() {
        Set<Integer> bh = this.root.isSentinel() ? Set.<Integer>of(0) : StreamSupport.stream(flatten(
                evaluateNodeBlackHeight(this.root, 0), 
                n -> Stream.of(n.node.left, n.node.right).filter(c -> c.isNotSentinel()).map(c -> evaluateNodeBlackHeight(c, n.offset)).toList()
            ).spliterator(), false).filter(xn -> xn.node.left.isSentinel() && xn.node.right.isSentinel())
                                   .map(xn -> xn.offset).collect(Collectors.toSet());

        if (bh.size() != 1 || !bh.stream().findFirst().get().equals(this.root.blackHeight)) {
            throw new IllegalStateException("Black height constraint violation");
        }

        long count = this.root.isSentinel() ? 0 : StreamSupport.stream(flatten(
                this.root, n -> Stream.of(n.left, n.right).filter(Node::isNotSentinel).toList()
            ).spliterator(), false).count();
            
        if (count != (long) this.size) {
            throw new IllegalStateException("size property is inconsistent");
        }

        if (this.root.isNotSentinel()) {
            StreamSupport.stream(flatten(
                    this.root, n -> Stream.of(n.left, n.right).filter(Node::isNotSentinel).toList()
                ).spliterator(), false).forEach(n -> {
                    if (n != this.root && n.parent == null || n.parent == sentinel()) {
                        throw new IllegalStateException("Missing parent reference detected");
                    }
                }
            );
        }

        return bh.stream().findFirst().get();
    }
    
    // TODO there is a mess with relative offsets at the moment not sure how to solve it correctly
    /*
    
    public void removeBefore(int position) {
        Pair<OffsetKeyedTreeMap<T>, OffsetKeyedTreeMap<T>> pair = this.split(position);
        this.splice(pair.getSecond());
    }
    
    public void removeAfter(int position) {
        Pair<OffsetKeyedTreeMap<T>, OffsetKeyedTreeMap<T>> pair = this.split(position);
        this.splice(pair.getFirst());
    }
    
    public void removeFragment(int position, int length) {
        if (length > 0) {
            Pair<OffsetKeyedTreeMap<T>, OffsetKeyedTreeMap<T>> tailPair = this.split(position + length);
            Pair<OffsetKeyedTreeMap<T>, OffsetKeyedTreeMap<T>> headPair = tailPair.getFirst().split(position);
    
            this.splice(headPair.getFirst().concat(tailPair.getSecond()));
        }
    }
        
    private static <T> OffsetKeyedTreeMap<T> cloneSubtree(Node<T> root) { // TODO we can get rid of this thing... theoretically
        var newNodes = new Stack<Node<T>>();
        int count = 0;

        if (root.isNotSentinel()) {
            Node<T> node = root, prev = null, next = null;

            do {
                if (prev == node.parent || prev == null) {
                    // we've just arrived, so investigate left, then right

                    if (node.left.isNotSentinel()) {
                        // left will be created later
                        next = node.left;
                    } else if (node.right.isNotSentinel()) {
                        // node's left is empty, go to the right
                        newNodes.push(sentinel());
                        next = node.right;
                    } else {
                        // both left and right are sentinels, so this is leaf
                        var newNode = new Node<>(node.offset, node.content, node.isRed, null, null); count++;
                        newNode.refreshBlackHeight();
                        newNodes.push(newNode);
                        next = node.parent;
                    }
                } else if (prev == node.left) {
                    // left is ready, prepare right

                    if (node.right.isNotSentinel()) {
                        // right will be created later
                        next = node.right;
                    } else {
                        // right is sentinel
                        var newNode = new Node<>(node.offset, node.content, node.isRed, newNodes.pop(), sentinel()); count++;
                        if (newNode.left.isNotSentinel())
                            newNode.left.parent = newNode;
                        newNode.refreshBlackHeight();
                        newNodes.push(newNode);
                        next = node.parent;
                    }
                } else if (prev == node.right) {
                    // both subnodes are ready, rightmost was the last one
                    var right = newNodes.pop();
                    var left = newNodes.pop();
                    var newNode = new Node<>(node.offset, node.content, node.isRed, left, right); count++;
                    if (newNode.left.isNotSentinel())
                        newNode.left.parent = newNode;
                    if (newNode.right.isNotSentinel())
                        newNode.right.parent = newNode;
                    newNode.refreshBlackHeight();
                    newNodes.push(newNode);
                    next = node.parent;
                } else {
                    throw new IllegalStateException("RB-tree inconsistency detected");
                }

                prev = node;
                node = next;
            } while (node != root.parent);
        }

        if (newNodes.size() > 1) {
            throw new IllegalStateException();
        }

        return new OffsetKeyedTreeMap<>(newNodes.size() > 0 ? newNodes.pop() : sentinel(), count);
    }

    public OffsetKeyedTreeMap<T> clone() {
        OffsetKeyedTreeMap<T> clone = cloneSubtree(this.root);
        if (clone.size != this.size) {
            throw new IllegalStateException("Cloning procedure inconsistency detected");
        } else {
            return clone;
        }
    }

    public OffsetKeyedTreeMap<T> concat(OffsetKeyedTreeMap<T> other) {
        if (this.root.isSentinel()) {
            return other.clone();
        }
        if (other.root.isSentinel()) {
            return this.clone();
        }

        return concatImpl(this.root, 0, other.root, 0);
    }

    private void splice(OffsetKeyedTreeMap<T> other) {
        this.root = other.root;
        this.size = other.size;
        this.FixupRoot();
    }
    
    private OffsetKeyedTreeMap<T> concatImpl(Node<T> l, int loff, Node<T> r, int roff) {
        OffsetKeyedTreeMap<T> a = cloneSubtree(l);
        OffsetKeyedTreeMap<T> b = cloneSubtree(r);
        
        if (l.blackHeight > r.blackHeight) {
            Subtree<T> ts = concatImplJoinRight(a.root, loff, b.root, roff);
            if (ts.node.isRed && ts.node.right.isRed)
                ts.node.isRed = false;

            return new OffsetKeyedTreeMap<>(ts.node, a.size + b.size + ts.size);
        } else if (l.blackHeight < r.blackHeight) {
            Subtree<T> ts = concatImplJoinLeft(a.root, loff, b.root, roff);
            if (ts.node.isRed && ts.node.left.isRed)
                ts.node.isRed = false;

            return new OffsetKeyedTreeMap<>(ts.node, a.size + b.size + ts.size);
        }

        { // TODO consider an approach of retrieving the center node from one of the subtrees being merged
            Node<T> newRoot = new Node<T>(roff - loff, null, true, l, r); // TODO validate position
            newRoot.refreshParentRefs();

            if (!(!l.isRed && !r.isRed)) {
                newRoot.isRed = false;
            }

            newRoot.refreshBlackHeight();
            return new OffsetKeyedTreeMap<>(newRoot, a.size + b.size + 1);
        }
    }

    private Pair<OffsetKeyedTreeMap<T>, OffsetKeyedTreeMap<T>> split(int pos) {
        return splitImpl(this.root, pos);     // TODO can be optimized
    }
    
    private void append(Node<T> other, int off) {
        this.splice(this.concatImpl(this.root, 0, other, off));
    }
    
    private void prepend(Node<T> other, int off) {
        this.splice(this.concatImpl(other, off, this.root, 0));
    }
    

    private Pair<OffsetKeyedTreeMap<T>, OffsetKeyedTreeMap<T>> splitImpl(Node<T> root, int pos) {
        //func split(T, k)
        //    if T = nil
        //        return ⟨nil, nil⟩
        //    if k < T.key
        //      ⟨L',R'⟩ = split(L,k)
        //      return ⟨L',join(R',T.key,R)⟩
        //    else 
        //      ⟨L',R'⟩ = split(R,k)
        //      return ⟨join(L,T.key,L'),R)⟩

        if (root.isSentinel()) {
            return new Pair<>(new OffsetKeyedTreeMap<T>(), new OffsetKeyedTreeMap<T>());
        }
        {
            if (pos < root.offset) {
                Pair<OffsetKeyedTreeMap<T>, OffsetKeyedTreeMap<T>> xy = this.splitImpl(root.left, pos);
                xy.getSecond().put(root.offset, root.content, null);
                xy.getSecond().append(root.right, root.offset);
                return xy;
            } else {
                Pair<OffsetKeyedTreeMap<T>, OffsetKeyedTreeMap<T>> xy = this.splitImpl(root.right, pos - root.offset);
                xy.getFirst().put(root.offset, root.content, null);
                xy.getFirst().prepend(root.right, root.offset);
                return xy;
            }
        }
    }
    
    private Subtree<T> concatImplJoinRight(Node<T> TL, int loff, Node<T> TR, int roff) {
        if (!TL.isRed && TL.blackHeight == TR.blackHeight) {
            Node<T> t = new Node<>(roff - loff, null, true, TL, TR);
            t.refreshBlackHeight();
            t.refreshParentRefs();
            return new Subtree<>(t, 1);
        } else {
            Subtree<T> crsr = concatImplJoinRight(TL.right, loff + TL.offset, TR, roff);
            Node<T> t = new Node<>(TL.offset, TL.content, TL.isRed, TL.left, crsr.node); // is it TL.right = cr ??
            t.refreshParentRefs();

            if (!TL.isRed && t.right.isRed && t.right.right.isRed) {
                if (t.right.right.isSentinel())
                    throw new IllegalStateException("Inconsistency during right-hand concat procedure");

                t.right.right.isRed = false;
                t.refreshBlackHeight();
                return new Subtree<>(this.rotateLeft(t), crsr.size);
            } else {
                t.refreshBlackHeight();
                return new Subtree<>(t, crsr.size);
            }
        }
    }

    private Subtree<T> concatImplJoinLeft(Node<T> TL, int loff, Node<T> TR, int roff) {
        if (!TR.isRed && TR.blackHeight == TL.blackHeight) {
            Node<T> t = new Node<>(null, true, TL, TR);
            t.refreshBlackHeight();
            t.refreshParentRefs();
            return new Subtree<>(t, 1);
        } else {
            Subtree<T> clsl = concatImplJoinLeft(TL, TR.left);
            Node<T> t = new Node<>(TR.content, TR.isRed, clsl.node, TR.right);  // is it TR.left = cl ??
            t.refreshParentRefs();

            if (!TR.isRed && t.left.isRed && t.left.left.isRed) {
                if (t.left.left.isSentinel())
                    throw new IllegalStateException("Inconsistency during left-hand concat procedure");

                t.left.left.isRed = false;
                t.refreshBlackHeight();
                return new Subtree<>(this.rotateRight(t), clsl.size);
            } else {
                t.refreshBlackHeight();
                return new Subtree<>(t, clsl.size);
            }
        }
    }
    
    */
    
    public void applyOffset(int position, int delta) {
    	if (delta == 0) {
    		return;
    	}
    	if (delta < 0) {
    		throw new UnsupportedOperationException("Negative delta not supported at the moment");
    	}
    	
    	NodeAndParentAtOffset<T> location = this.findImpl(position);		
    	Node<T> node = location.node.isSentinel() ? location.parent : location.node;
    	
    	while (node != null) {
    		node.offset += delta;
    		Node<T> parent = node.parent;
    		while (parent != null) {
    			if (node == parent.left) {
    				parent.offset += delta;
    				node = parent;
    				parent = node.parent;
    			} else {
    				while (parent != null && node == parent.right) {
    					node = parent;
    					parent = node.parent;
    				}
    			}
    		}
    	}
    }
    
    
}
