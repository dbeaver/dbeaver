package org.jkiss.dbeaver.model.stm;

import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public interface STMTreeNode extends Tree {
    
    void fixup(@NotNull STMParserOverrides parserCtx);

    @NotNull
    String getNodeName();
        
    @NotNull
    Interval getRealInterval();

    @Nullable
    default String getTextContent() {
        String result = null;
        if (this instanceof STMTreeRuleNode) {
            STMTreeRuleNode ruleNode = ((STMTreeRuleNode) this);
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
            if (first instanceof TerminalNode && last instanceof TerminalNode) {
                TerminalNode a = (TerminalNode) first;
                TerminalNode b = (TerminalNode) last;
                Interval textRange = Interval.of(a.getSymbol().getStartIndex(), b.getSymbol().getStopIndex());
                result = b.getSymbol().getTokenSource().getInputStream().getText(textRange);
            }
        }
        return result;
    }

    @NotNull
    String getText();
    
}
