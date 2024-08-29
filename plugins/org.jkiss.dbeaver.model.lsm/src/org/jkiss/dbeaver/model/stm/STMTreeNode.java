package org.jkiss.dbeaver.model.stm;

import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The interface describing the node of the syntax tree
 */
public interface STMTreeNode extends Tree {

    /**
     * Provides information about the grammar rule to the syntax tree nodes
     */
    void fixup(@NotNull STMParserOverrides parserCtx);

    default int getNodeKindId() {
        return -1;
    }

    /**
     * Get the state of the antlr parser finite state machine associated with the entry point to the corresponding syntax rule for the text
     */
    int getAtnState();

    /**
     * Get the name of the syntax tree node
     */
    @NotNull
    String getNodeName();

    /**
     * Get the text range interval covered by the node
     */
    @NotNull
    Interval getRealInterval();

    /**
     * Get the text fragment covered by the node
     */
    @NotNull
    String getTextContent();

    /**
     * Get the text fragment provided by antlr (without hidden channel tokens)
     */
    @NotNull
    String getText();

    @Nullable
    default STMTreeNode getParentNode() {
        return getParent() instanceof STMTreeNode parent ? parent : null;
    }

    /**
     * Returns child node by index
     */
    default STMTreeNode getChildNode(int index) {
        throw new UnsupportedOperationException();
    }

    boolean hasErrorChildren();

    @Nullable
    default STMTreeNode findFirstNonErrorChild() {
        if (this.hasErrorChildren()) {
            for (int i = 0; i < this.getChildCount(); i++) {
                STMTreeNode cn = this.getChildNode(i);
                if (cn != null && !(cn instanceof ErrorNode)) {
                    return cn;
                }
            }
        } else if (this.getChildCount() > 0) {
            return this.getChildNode(0);
        }
        return null;
    }

    @Nullable
    default STMTreeNode findLastNonErrorChild() {
        if (this.hasErrorChildren()) {
            for (int i = this.getChildCount() - 1; i >= 0; i--) {
                STMTreeNode cn = this.getChildNode(i);
                if (cn != null && !(cn instanceof ErrorNode)) {
                    return cn;
                }
            }
        } else if (this.getChildCount() > 0) {
            return this.getChildNode(this.getChildCount() - 1);
        }
        return null;
    }

    /**
     * Returns child node by name
     */
    @Nullable
    default STMTreeNode findFirstChildOfName(@NotNull String nodeName) {
        for (int i = 0; i < this.getChildCount(); i++) {
            STMTreeNode cn = this.getChildNode(i);
            if (cn != null && cn.getNodeName().equals(nodeName)) {
                return cn;
            }
        }
        return null;
    }

    @Nullable
    default STMTreeNode findLastChildOfName(@NotNull String nodeName) {
        for (int i = this.getChildCount(); i >= 0; i--) {
            STMTreeNode cn = this.getChildNode(i);
            if (cn != null && cn.getNodeName().equals(nodeName)) {
                return cn;
            }
        }
        return null;
    }

    @NotNull
    default List<STMTreeNode> getChildren() {
        return new AbstractList<>() {
            @Override
            public STMTreeNode get(int index) {
                return getChildNode(index);
            }

            @Override
            public int size() {
                return getChildCount();
            }
        };
    }

    @NotNull
    default List<STMTreeNode> findChildrenOfName(@NotNull String nodeName) {
        List<STMTreeNode> children = new ArrayList<>(this.getChildCount());
        for (int i = 0; i < this.getChildCount(); i++) {
            STMTreeNode cn = this.getChildNode(i);
            if (cn != null && cn.getNodeName().equals(nodeName)) {
                children.add(cn);
            }
        }
        return children;
    }

    @NotNull
    default List<STMTreeNode> findNonErrorChildren() {
        if (this.hasErrorChildren()) {
            List<STMTreeNode> children = new ArrayList<>(this.getChildCount());
            for (int i = 0; i < this.getChildCount(); i++) {
                STMTreeNode cn = this.getChildNode(i);
                if (cn != null && !(cn instanceof ErrorNode)) {
                    children.add(cn);
                }
            }
            return children;
        } else {
            return this.getChildren();
        }
    }

    @NotNull
    default Map<String, List<STMTreeNode>> classifyChildren() {
        Map<String, List<STMTreeNode>> childrenByName = new HashMap<>(this.getChildCount());
        for (int i = 0; i < this.getChildCount(); i++) {
            STMTreeNode cn = this.getChildNode(i);
            if (cn != null) {
                childrenByName.computeIfAbsent(cn.getNodeName(), k -> new ArrayList<>()).add(cn);
            }
        }
        return childrenByName;
    }
}
