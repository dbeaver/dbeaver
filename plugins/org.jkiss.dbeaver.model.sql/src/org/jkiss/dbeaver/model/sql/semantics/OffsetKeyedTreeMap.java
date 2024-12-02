/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.Nullable;

import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
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
        return (Node<T>) Node.SENTINEL;
    }

    private static class Node<T> {
        public static final Node<?> SENTINEL = new Node<>(0, null, false, null, null);

        public boolean isSentinel() {
            return this == SENTINEL;
        }
        public boolean isNotSentinel() {
            return this != SENTINEL;
        }

        public int blackHeight = 0; // blackHeight needed for efficient split-join operations, which we are not using at the moment  
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
        
        @Override
        public String toString() {
            return this.getClass().getSimpleName() + (this.isSentinel() ? "[SENTINEL]" : ("[" + this.offset + ", '" + this.content + "']"));
        }
    }

    private Node<T> root;
    private int size, tombstonesCount;

    public OffsetKeyedTreeMap() {
        this.root = sentinel();
        this.size = 0;
        this.tombstonesCount = 0;
    }

    public void clear() {
        this.root = sentinel();
        this.size = 0;
        this.tombstonesCount = 0;
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
        
        return new NodeAndParentAtOffset<>(parent, node, isLeft, relPos);
    }
    
    public T find(int position) {
        NodeAndParentAtOffset<T> result = this.findImpl(position);
        return result.node.isNotSentinel() ? result.node.content : null;
    }
    
    public T put(int pos, T value) {
        return this.put(pos, value, null);
    }
    
    public T put(int pos, T value, RemappingFunction<T> remappingFunction) {
        T oldValue;

        if (this.root.isSentinel()) {
            this.root = new Node<>(pos, value, false, null, null);
            this.root.refreshBlackHeight();
            this.size++;
            oldValue = null;
        } else {
            NodeAndParentAtOffset<T> location = this.findImpl(pos);
            if (location.node.isNotSentinel()) {
                oldValue = location.node.content;
                T newValue;
                if (oldValue == null) {
                    this.tombstonesCount--;
                    newValue = value;
                } else if (remappingFunction == null) {
                    newValue = value;
                } else {
                    newValue = remappingFunction.apply(pos, value, oldValue);
                }
                location.node.content = newValue;
            } else {
                oldValue = null;
                Node<T> newNode = new Node<>(location.offset, value, true, null, null);
                Node<T> parent = location.parent;
              
                if (location.isLeft) {
                    parent.left = newNode;
                } else {
                    parent.right = newNode;
                }
                newNode.parent = parent;
                this.size++;
        
                this.restoreAfterInsert(newNode);
            }
        }
        
        return oldValue;
    }

    ///<summary>
    /// RestoreAfterInsert
    /// Additions to red-black trees usually destroy the red-black 
    /// properties. Examine the tree and restore. Rotations are normally 
    /// required to restore it
    ///</summary>
    private void restoreAfterInsert(Node<T> x) {
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
            if (x.parent == x.parent.parent.left) {
                // determine traversal path
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
            } else {
                // x's parent is on the right subtree
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

        this.fixupRoot();
    }

    private void fixupRoot() {
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

    public int size() {
        return this.size - this.tombstonesCount;
    }

    public NodesIterator<T> nodesIteratorAt(int position) {
        NodeAndParentAtOffset<T> initialLocation = this.findImpl(position);
        return switch (this.size) {
            case 0 -> new NodesIterator<T>() {
                @Nullable
                @Override
                public T getCurrValue() {
                    return null;
                }

                @Override
                public int getCurrOffset() {
                    return position;
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
                final Node<T> theOnlyNode = OffsetKeyedTreeMap.this.root;

                @Nullable
                @Override
                public T getCurrValue() {
                    return !this.beforeFirst && !this.afterLast ? this.theOnlyNode.content : null;
                }

                @Override
                public int getCurrOffset() {
                    return this.beforeFirst ? Integer.MIN_VALUE : (this.afterLast ? Integer.MAX_VALUE : this.theOnlyNode.offset);
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
                NodeAndOffset<T> currentLocation = new NodeAndOffset<>(
                    initialLocation.node.isSentinel() ? null : initialLocation.node, 
                    initialLocation.node.isSentinel() ? position : position - initialLocation.node.offset
                );

                @Nullable
                @Override
                public T getCurrValue() {
                    return this.currentLocation.node == null ? null : this.currentLocation.node.content;
                }

                @Override
                public int getCurrOffset() {
                    return this.currentLocation.offset + (this.currentLocation.node == null ? 0 : this.currentLocation.node.offset);
                }

                @Override
                public boolean prev() {
                    if (this.initial && initialLocation.node.isSentinel()) {
                        NodeAndOffset<T> parentLocation = new NodeAndOffset<>(
                            initialLocation.parent, position - initialLocation.offset - (initialLocation.isLeft ? 0 : initialLocation.parent.offset)
                        );
                        this.currentLocation = initialLocation.isLeft ? findPrev(parentLocation) : parentLocation;
                    } else if (this.beforeFirst) {
                        return false;
                    } else if (this.afterLast) {
                        this.currentLocation = findLast(new NodeAndOffset<>(OffsetKeyedTreeMap.this.root, 0));
                        this.afterLast = false;
                    } else {
                        this.currentLocation = findPrev(this.currentLocation);
                    }
                    while (this.currentLocation.node != null && this.currentLocation.node.content == null) { // due to tombstones
                        this.currentLocation = findPrev(this.currentLocation);
                    }
                    this.initial = false;
                    this.beforeFirst = this.currentLocation.node == null;
                    return this.currentLocation.node != null;
                }

                @Override
                public boolean next() {
                    if (this.initial && initialLocation.node.isSentinel()) {
                        // the exact initial position not found, so proceed with its parent
                        NodeAndOffset<T> parentLocation = new NodeAndOffset<>(
                            initialLocation.parent,
                            position - initialLocation.offset - (initialLocation.isLeft ? 0 : initialLocation.parent.offset)
                        );
                        this.currentLocation = initialLocation.isLeft ? parentLocation : findNext(parentLocation);
                    } else if (this.afterLast) {
                        return false;
                    } else if (this.beforeFirst) {
                        this.currentLocation = findFirst(new NodeAndOffset<>(OffsetKeyedTreeMap.this.root, 0));
                        this.beforeFirst = false;
                    } else {
                        this.currentLocation = findNext(this.currentLocation);
                    }
                    while (this.currentLocation.node != null && this.currentLocation.node.content == null) { // due to tombstones
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
        return new NodeAndOffset<>(node, offset);
    }
    
    private NodeAndOffset<T> findLast(NodeAndOffset<T> location) {
        Node<T> node = location.node;
        int offset = location.offset;
        while (node.right.isNotSentinel()) {
            offset += node.offset;
            node = node.right;
        }
        return new NodeAndOffset<>(node, offset);
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
            while (prev != null && prev.right != node) {
                node = prev;
                prev = node.parent;
            }
            if (prev != null && prev.right == node) {
                offset -= prev.offset;
            }
        }
        return new NodeAndOffset<>(prev, prev == null ? Integer.MIN_VALUE : offset);
    }
        
    private NodeAndOffset<T> findNext(NodeAndOffset<T> location) {
        Node<T> node = location.node;
        int offset = location.offset;
        Node<T> next;
        if (node.right.isNotSentinel()) {
            next = node.right;
            offset += node.offset;
            while (next.left.isNotSentinel()) {
                next = next.left;
            }
        } else {
            next = node.parent;
            if (next != null) {
                if (next.right == node) {
                    offset -= next.offset;
                }
                while (next != null && next.left != node) {
                    node = next;
                    next = node.parent;
                    if (next != null && next.right == node) {
                        offset -= next.offset;
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
        Set<Integer> bh = this.root.isSentinel() ? Set.<Integer>of(0) : StreamSupport.stream(
            flatten(
                evaluateNodeBlackHeight(this.root, 0), 
                n -> Stream.of(n.node.left, n.node.right).filter(Node::isNotSentinel)
                    .map(c -> evaluateNodeBlackHeight(c, n.offset)).toList()
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
            StreamSupport.stream(
                flatten(
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
        if (this.size == 0) {
            return;
        }

        NodeAndParentAtOffset<T> location = this.findImpl(position);
        if (location.node.isSentinel() && location.isLeft) {
            location.parent.offset += delta;
        }
        if (location.node.isNotSentinel()) {
            location.node.offset += delta;
        }

        Node<T> node = location.node.isSentinel() ? location.parent : location.node;
        while (node != null && node.parent != null) {
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

    public void forEach(BiConsumer<Integer, T> action) {
        if (root.isNotSentinel()) {
            int currOffset = 0;
            Node<T> node = root;
            Node<T> prev = null;
            Node<T> next = null;

            do {
                if (prev == node.parent || prev == null) {
                    // we've just arrived, so investigate left, then right

                    if (node.left.isNotSentinel()) {
                        // left will be created later
                        next = node.left;
                    } else if (node.right.isNotSentinel()) {
                        // node's left is empty, go to the right
                        action.accept(currOffset + node.offset, node.content);
                        currOffset += node.offset;
                        next = node.right;
                    } else {
                        // both left and right are sentinels, so this is leaf
                        action.accept(currOffset + node.offset, node.content);
                        currOffset -= node.parent != null && node.parent.right == node ? node.parent.offset : 0;
                        next = node.parent;
                    }
                } else if (prev == node.left) {
                    // left is ready, prepare right
                    action.accept(currOffset + node.offset, node.content);

                    if (node.right.isNotSentinel()) {
                        // right will be created later
                        currOffset += node.offset;
                        next = node.right;
                    } else {
                        // right is sentinel
                        currOffset -= node.parent != null && node.parent.right == node ? node.parent.offset : 0;
                        next = node.parent;
                    }
                } else if (prev == node.right) {
                    // both subnodes are ready, rightmost was the last one
                    currOffset -= node.parent != null && node.parent.right == node ? node.parent.offset : 0;
                    next = node.parent;
                } else {
                    throw new IllegalStateException("RB-tree inconsistency detected");
                }

                prev = node;
                node = next;
            } while (node != root.parent);
        }
    }

    private static <T> void stringifyNode(StringBuilder sb, int depth, int offset, Node<T> node, String prefix) {
        sb.append(String.join("", "  ".repeat(depth))).append(prefix)
            .append("[").append(offset).append(" as ").append(node.offset).append("] ")
            .append(node.content).append("\n");
    }

    private void collectImpl(StringBuilder sb, int depth, int offCtx, Node<T> node, String prefix) {
        stringifyNode(sb, depth, offCtx + node.offset, node, prefix);
        if (node.left.isNotSentinel()) {
            collectImpl(sb, depth + 1, offCtx, node.left, "L");
        }
        if (node.right.isNotSentinel()) {
            collectImpl(sb, depth + 1, offCtx + node.offset, node.right, "R");
        }
    }

    public String collect() {
        StringBuilder sb = new StringBuilder();
        collectImpl(sb, 0, 0, this.root, "");
        return sb.toString();
    }

    public boolean removeAt(int position) {
        NodeAndParentAtOffset<T> location = this.findImpl(position);
        if (location.node.isNotSentinel()) {
            this.deleteNode(location.node);
            return true;
        } else {
            return false;
        }
    }

    private void deleteNode(Node<T> z) {
        // A node to be deleted will be: 
        //    1. a leaf with no children
        //    2. have one child
        //    3. have two children
        // If the deleted node is red, the red black properties still hold.
        // If the deleted node is black, the tree needs rebalancing

        Node<T> y;                 // work node

        // find the replacement node (the successor to x) - the node one with at *most* one child. 
        if (z.left.isSentinel() || z.right.isSentinel()) {
            y = z;                      // node has sentinel as a child
        } else {
            // z has two children, find replacement node which will be the leftmost node greater than z
            y = z.right;                        // traverse right subtree
//            while (y.left.isNotSentinel()) {    // to find next node in sequence
//                y = y.left;
//            }
            if (y.left.isNotSentinel()) { // FIXME standard rb-tree removal strategy cannot be applied, just mark z as tombstone for now
                z.content = null;
                this.tombstonesCount++;
                if (this.tombstonesCount > this.size / 2) {
                    var t = new OffsetKeyedTreeMap<T>();
                    NodesIterator<T> it = this.nodesIteratorAt(Integer.MAX_VALUE);
                    while (it.prev()) {
                        t.put(it.getCurrOffset(), it.getCurrValue());
                    }
                    this.root = t.root;
                    this.size = t.size;
                    this.tombstonesCount = 0;
                }
                return;
            }
        }

        // at this point, y contains the replacement node. it's content will be copied to the values in the node to be deleted

        // x (y's only child) is the node that will be linked to y's old parent. 
        Node<T> x; // work node to contain the replacement node
        int xDelta;
        if (y.left.isNotSentinel()) {
            x = y.left;
            xDelta = 0;
        } else {
            x = y.right;
            xDelta = y.offset;
        }

        // replace x's parent with y's parent and link x to proper subtree in parent; this removes y from the chain
        x.parent = y.parent;
        if (y.parent != null) {
            if (y == y.parent.left) {
                y.parent.left = x;
            } else {
                y.parent.right = x;
            }
        } else {
            this.root = x;         // make x the root node
        }

        if (xDelta > 0) {
            // y's offset was applied to x-rooted subtree, now y is gone, so apply the delta explicitly
            // TODO consider merging with the same loop below
            for (Node<T> t = x; t.isNotSentinel(); t = t.left) {
                t.offset += xDelta;
            }
        }

        // copy the values from y (the replacement node) to the node being deleted.
        // note: this effectively deletes the node. 
        if (y != z) {
            z.content = y.content;
            z.offset += y.offset; // z's global position should be the same as y's now, 
            for (Node<T> t = z.right; t.isNotSentinel(); t = t.left) { // but it'll affect the whole right subtree, which is unwanted,
                t.offset -= y.offset; // so fix it up
                if (t.offset < 0) {
                    // should never happen due to consistency rules, as we already applied xDelta
                    throw new IllegalStateException("relative offsets invariant being positive violated during after-delete fixup");
                    // FIXME this does happen apparently, so we need another removal strategy
                    // FIXME take a look at AVL tree or something with rotation-based balancing, as rotations seems to be ok
                }
            }
        }

        if (!y.isRed) {
            this.restoreAfterDelete(x);
        }

        this.size--;
    }

    private void restoreAfterDelete(Node<T> x) {
        // maintain Red-Black tree balance after deleting node

        Node<T> y;

        while (x != this.root && !x.isRed) {
            if (x == x.parent.left) {         // determine sub tree from parent
                y = x.parent.right;         // y is x's sibling 
                if (y.isRed) {   // x is black, y is red - make both black and rotate
                    y.isRed = false;
                    x.parent.isRed = true;
                    this.rotateLeft(x.parent);
                    y = x.parent.right;
                }
                if (!y.left.isRed && !y.right.isRed) {   // children are both black
                    y.isRed = true;     // change parent to red
                    x = x.parent;                   // move up the tree
                } else {
                    if (!y.right.isRed) {
                        y.left.isRed = false;
                        y.isRed = true;
                        this.rotateRight(y);
                        y = x.parent.right;
                    }
                    y.isRed = x.parent.isRed;
                    x.parent.isRed = false;
                    y.right.isRed = false;
                    this.rotateLeft(x.parent);
                    x = this.root;
                }
            } else {   // right subtree - same as code above with right and left swapped
                y = x.parent.left;
                if (y.isRed) {
                    y.isRed = false;
                    x.parent.isRed = true;
                    this.rotateRight(x.parent);
                    y = x.parent.left;
                }
                if (!y.right.isRed && !y.left.isRed) {
                    y.isRed = true;
                    x = x.parent;
                } else {
                    if (!y.left.isRed) {
                        y.right.isRed = false;
                        y.isRed = true;
                        this.rotateLeft(y);
                        y = x.parent.left;
                    }
                    y.isRed = x.parent.isRed;
                    x.parent.isRed = false;
                    y.left.isRed = false;
                    this.rotateRight(x.parent);
                    x = this.root;
                }
            }
        }
        x.isRed = false;
    }
}
