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
package org.jkiss.dbeaver.model.lsm.sql.impl.expression;

import org.jkiss.dbeaver.model.lsm.mapping.*;
import org.jkiss.dbeaver.model.lsm.sql.impl.SelectionQuery;

import java.util.List;

public abstract class Expression extends AbstractSyntaxNode {

    public abstract class RowValueExpressionSpecification implements SyntaxSubnodesSpecificationDefinition<RowValue> {

        private final static String valueExpressionPath = "self::rowValueConstructor/rowValueConstructorElement/valueExpression/";
        
        @SyntaxSubnode(type = RowSubqueryValue.class, xpath = "self::rowValueConstructor/rowSubquery")
        @SyntaxSubnode(type = RowValueListExpression.class, xpath = "self::rowValueConstructor/rowValueConstructorList")
        @SyntaxSubnode(type = DefaultValue.class, xpath = "self::rowValueConstructor/rowValueConstructorElement/defaultSpecification")
        @SyntaxSubnode(type = NullValue.class, xpath = "self::rowValueConstructor/rowValueConstructorElement/nullSpecification")
        // @SyntaxSubnode(type = null, xpath =
        // "self::rowValueConstructor/rowValueConstructorElement/valueExpression/...")
        @SyntaxSubnode(type = Sum.class, xpath = valueExpressionPath + "numericValueExpression/*[last()][name()='plusTerm']")
        @SyntaxSubnode(type = Sub.class, xpath = valueExpressionPath + "numericValueExpression/*[last()][name()='minusTerm']")
        @SyntaxSubnode(type = Mul.class, xpath = valueExpressionPath + "numericValueExpression[count(./*) = 1]/term/*[last()][name()='multiplyFactor']")
        @SyntaxSubnode(type = Div.class, xpath = valueExpressionPath + "numericValueExpression[count(./*) = 1]/term/*[last()][name()='divideFactor']")
        @SyntaxSubnode(type = Number.class, xpath = valueExpressionPath + "numericValueExpression[count(./*) = 1]/term[count(./*) = 1]/factor/numericPrimary/unsignedValueSpecification/unsignedLiteral")
        public abstract RowValue member();
    }

    public static abstract class RowValue extends Expression {
    }

    public static abstract class SimpleRowValue extends RowValue {
    }

    public static abstract class ComplexRowValue extends RowValue {

    }

    @SyntaxNode(name = "rowSubquery")
    public static class RowSubqueryValue extends ComplexRowValue {
        
        @SyntaxSubnode(type = SelectionQuery.class, xpath = "./subquery/queryExpression")
        public SelectionQuery subquery;
    }

    @SyntaxNode(name = "rowValueConstructorList")
    public static class RowValueListExpression extends ComplexRowValue {
        
        @SyntaxSubnode(type = DefaultValue.class, xpath = "./rowValueConstructorElement/defaultSpecification")
        @SyntaxSubnode(type = NullValue.class, xpath = "./rowValueConstructorElement/nullSpecification")
        // @SyntaxSubnode(type = null, xpath =
        // "./rowValueConstructorElement/valueExpression/...")
        public List<SimpleRowValue> elements;
    }

    @SyntaxNode(name = "defaultSpecification")
    public static class DefaultValue extends SimpleRowValue {
    }

    @SyntaxNode(name = "nullSpecification")
    public static class NullValue extends SimpleRowValue {
    }

    public static abstract class ValueExpression extends SimpleRowValue {
        // (numericValueExpression|stringValueExpression|datetimeValueExpression|intervalValueExpression);
    }

    public static abstract class NumericValue extends ValueExpression {

    }

    @SyntaxNode(name = "plusTerm")
    public static class Sum extends NumericValue {
        
        @SyntaxSubnode(type = Sum.class, xpath = "./preceding-sibling::*[1][name()='plusTerm']")
        @SyntaxSubnode(type = Sub.class, xpath = "./preceding-sibling::*[1][name()='minusTerm']")
        @SyntaxSubnode(type = Mul.class, xpath = "./preceding-sibling::*[1][name()='term']/*[last()][name()='multiplyFactor']")
        @SyntaxSubnode(type = Div.class, xpath = "./preceding-sibling::*[1][name()='term']/*[last()][name()='divideFactor']")
        @SyntaxSubnode(type = Number.class, xpath = "./preceding-sibling::*[1][name()='term' and count(./*) = 1]/factor/numericPrimary/valueExpressionPrimary/unsignedValueSpecification/unsignedLiteral")
        public NumericValue left;
        
        @SyntaxSubnode(type = Mul.class, xpath = "./term/*[last()][name()='multiplyFactor']")
        @SyntaxSubnode(type = Div.class, xpath = "./term/*[last()][name()='divideFactor']")
        @SyntaxSubnode(type = Number.class, xpath = "./term[count(./*) = 1]/factor/numericPrimary/valueExpressionPrimary/unsignedValueSpecification/unsignedLiteral")
        public NumericValue right;
    }

    @SyntaxNode(name = "minusTerm")
    public static class Sub extends NumericValue {
        @SyntaxSubnode(type = Sum.class, xpath = "./preceding-sibling::*[1][name()='plusTerm']")
        @SyntaxSubnode(type = Sub.class, xpath = "./preceding-sibling::*[1][name()='minusTerm']")
        @SyntaxSubnode(type = Mul.class, xpath = "./preceding-sibling::*[1][name()='ter m']/*[last()][name()='multiplyFactor']")
        @SyntaxSubnode(type = Div.class, xpath = "./preceding-sibling::*[1][name()='term']/*[last()][name()='divideFactor']")
        @SyntaxSubnode(type = Number.class, xpath = "./preceding-sibling::*[1][name()='term' and count(./*) = 1]/factor/numericPrimary/valueExpressionPrimary/unsignedValueSpecification/unsignedLiteral")
        public NumericValue left;
        
        @SyntaxSubnode(type = Mul.class, xpath = "./term/*[last()][name()='multiplyFactor']")
        @SyntaxSubnode(type = Div.class, xpath = "./term/*[last()][name()='divideFactor']")
        @SyntaxSubnode(type = Number.class, xpath = "./term[count(./*) = 1]/factor/numericPrimary/valueExpressionPrimary/unsignedValueSpecification/unsignedLiteral")
        public NumericValue right;
    }

    @SyntaxNode(name = "multiplyFactor")
    public static class Mul extends NumericValue {
        @SyntaxSubnode(type = Mul.class, xpath = "./preceding-sibling::*[1][name()='multiplyFactor']")
        @SyntaxSubnode(type = Div.class, xpath = "./preceding-sibling::*[1][name()='divideFactor']")
        @SyntaxSubnode(type = Number.class, xpath = "./preceding-sibling::*[1][name()='factor' and count(./*) = 1]/numericPrimary/valueExpressionPrimary/unsignedValueSpecification/unsignedLiteral")
        public NumericValue left;
        
        @SyntaxSubnode(type = Number.class, xpath = "./factor/numericPrimary/valueExpressionPrimary/unsignedValueSpecification/unsignedLiteral")
        public NumericValue right;
    }

    @SyntaxNode(name = "divideFactor")
    public static class Div extends NumericValue {
        @SyntaxSubnode(type = Mul.class, xpath = "./preceding-sibling::*[1][name()='multiplyFactor']")
        @SyntaxSubnode(type = Div.class, xpath = "./preceding-sibling::*[1][name()='divideFactor']")
        @SyntaxSubnode(type = Number.class, xpath = "./preceding-sibling::*[1][name()='factor' and count(./*) = 1]/numericPrimary/valueExpressionPrimary/unsignedValueSpecification/unsignedLiteral")
        public NumericValue left;
        
        @SyntaxSubnode(type = Number.class, xpath = "./factor/numericPrimary/valueExpressionPrimary/unsignedValueSpecification/unsignedLiteral")
        public NumericValue right;
    }

    @SyntaxNode(name = "unsignedLiteral")
    public static class Number extends NumericValue {
        @SyntaxTerm(xpath = "./text()")
        public double value;
    }
}
