package org.jkiss.dbeaver.parser.grammar;

import java.util.Collections;
import java.util.List;

public abstract class GroupExpression extends RuleExpression {

    public final List<RuleExpression> children;

    protected GroupExpression(List<RuleExpression> exprs) {
        this.children = Collections.unmodifiableList(exprs);
    }
}
