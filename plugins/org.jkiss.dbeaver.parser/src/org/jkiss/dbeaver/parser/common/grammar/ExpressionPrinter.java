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
package org.jkiss.dbeaver.parser.common.grammar;

import java.util.Iterator;

public class ExpressionPrinter implements ExpressionVisitor<StringBuilder, StringBuilder> {

    private static final ExpressionPrinter INSTANCE = new ExpressionPrinter();

    public static String format(RuleExpression expr) {
        return expr == null ? "<NULL>" : expr.apply(INSTANCE, new StringBuilder()).toString();
    }

    private ExpressionPrinter() {

    }

    private StringBuilder visit(RuleExpression parent, RuleExpression expr, StringBuilder sb) {
        boolean needsWrapping = (parent instanceof GroupExpression && expr instanceof GroupExpression
                && parent.getClass() != expr.getClass())
                || (parent instanceof UnaryExpression && expr instanceof GroupExpression);

        if (needsWrapping) {
            sb.append("(");
        }

        expr.apply(this, sb);

        if (needsWrapping) {
            sb.append(")");
        }

        return sb;
    }

    private StringBuilder visitUnary(UnaryExpression unary, StringBuilder sb) {
        return this.visit(unary, unary.child, sb);
    }

    private StringBuilder visitGroup(GroupExpression group, StringBuilder sb, String separator) {
        Iterator<RuleExpression> it = group.children.iterator();
        if (it.hasNext()) {
            this.visit(group, it.next(), sb);
            while (it.hasNext()) {
                sb.append(separator);
                this.visit(group, it.next(), sb);
            }
        }
        return sb;
    }

    @Override
    public StringBuilder visitSequence(SequenceExpression sequence, StringBuilder sb) {
        return this.visitGroup(sequence, sb, " ");
    }

    @Override
    public StringBuilder visitAlternative(AlternativeExpression alternative, StringBuilder sb) {
        return this.visitGroup(alternative, sb, "|");
    }

    @Override
    public StringBuilder visitCharacters(CharactersExpression charactersExpression, StringBuilder sb) {
        return sb.append("'").append(charactersExpression.pattern).append("'");
    }

    @Override
    public StringBuilder visitCheck(CheckExpression checkExpression, StringBuilder sb) {
        return this.visitUnary(checkExpression, sb);
    }

    @Override
    public StringBuilder visitCheckNot(CheckNotExpression checkNotExpression, StringBuilder sb) {
        return this.visitUnary(checkNotExpression, sb);
    }

    @Override
    public StringBuilder visitRuleCall(RuleCallExpression ruleCallExpression, StringBuilder sb) {
        return sb.append(ruleCallExpression.ruleName).append("()");
    }

    @Override
    public StringBuilder visitNumber(NumberExpression numberExpression, StringBuilder sb) {
        this.visitUnary(numberExpression, sb).append("{");
        sb.append(numberExpression.min);
        sb.append(",");
        if (numberExpression.max != Integer.MAX_VALUE) {
            sb.append(numberExpression.max);
        }
        sb.append("}");
        return sb;
    }

    @Override
    public StringBuilder visitRegex(RegexExpression regexExpression, StringBuilder sb) {
        return sb.append("\"").append(regexExpression.pattern).append("\"");
    }

}
