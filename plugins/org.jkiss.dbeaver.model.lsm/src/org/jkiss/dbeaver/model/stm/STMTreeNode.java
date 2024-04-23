package org.jkiss.dbeaver.model.stm;

import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

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
    @Nullable
    default String getTextContent() {
        String result = null;
        if (this instanceof STMTreeRuleNode ruleNode) {
            Interval textRange = ruleNode.getRealInterval();
            result = ruleNode.getStart().getInputStream().getText(textRange);
        } else if (this instanceof TerminalNode) {
            Interval textRange = this.getRealInterval();
            result = ((TerminalNode) this).getSymbol().getInputStream().getText(textRange);
        } else if (this instanceof ParseTree) {
            result = ((ParseTree) this).getText(); // consider extracting whitespaces but not comments
        } else {
            Tree first = this;
            Tree last = this;

            while (!(first instanceof TerminalNode) && first.getChildCount() > 0) {
                first = first.getChild(0);
            }
            while (!(last instanceof TerminalNode) && last.getChildCount() > 0) {
                last = last.getChild(last.getChildCount() - 1);
            }
            if (first instanceof TerminalNode a && last instanceof TerminalNode b) {
                Interval textRange = Interval.of(a.getSymbol().getStartIndex(), b.getSymbol().getStopIndex());
                result = b.getSymbol().getTokenSource().getInputStream().getText(textRange);
            }
        }
        return result;
    }

    /**
     * Get the text fragment provided by antlr (without hidden channel tokens)
     */
    @NotNull
    String getText();

    @Nullable
    default STMTreeNode getStmParent() {
        return getParent() instanceof STMTreeNode parent ? parent : null;
    }

    /**
     * Returns child node by index
     */
    default STMTreeNode getStmChild(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns child node by name
     */
    @Nullable
    default STMTreeNode findChildOfName(@NotNull String nodeName) {
        for (int i = 0; i < this.getChildCount(); i++) {
            STMTreeNode cn = this.getStmChild(i);
            if (cn != null && cn.getNodeName().equals(nodeName)) {
                return cn;
            }
        }
        return null;
    }
}
