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
package org.jkiss.dbeaver.model.sql.parser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.parser.SimpleNode;

/**
 * CustomExpression
 */
public class CustomExpression implements Expression {

    private final String expression;
    private SimpleNode simpleNode;

    public CustomExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(new StringValue(expression));
    }

    @Override
    public <T, S> T accept(ExpressionVisitor<T> expressionVisitor, S context) {
        return expressionVisitor.visit(new StringValue(), context);
    }

    @Override
    public String toString() {
        return expression;
    }

    @Override
    public SimpleNode getASTNode() {
        return simpleNode;
    }

    @Override
    public void setASTNode(SimpleNode simpleNode) {
        this.simpleNode = simpleNode;
    }
}
