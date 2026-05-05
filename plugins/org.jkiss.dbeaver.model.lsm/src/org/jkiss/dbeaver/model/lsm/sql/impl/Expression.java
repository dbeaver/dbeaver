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
package org.jkiss.dbeaver.model.lsm.sql.impl;

import java.util.List;

public abstract class Expression {

    public abstract class RowValueExpressionSpecification {
        // public abstract RowValue member();
    }

    public abstract static class RowValue extends Expression {
    }

    public abstract static class SimpleRowValue extends RowValue {
    }

    public abstract static class ComplexRowValue extends RowValue {

    }

    public static class RowSubqueryValue extends ComplexRowValue {
        public SelectionQuery subquery;
    }

    public static class RowValueListExpression extends ComplexRowValue {

        public List<SimpleRowValue> elements;
    }

    public static class DefaultValue extends SimpleRowValue {
    }

    public static class NullValue extends SimpleRowValue {
    }

    public abstract static class ValueExpression extends SimpleRowValue {
    }

    public abstract static class NumericValue extends ValueExpression {

    }

    public static class Sum extends NumericValue {
        public NumericValue left;
        public NumericValue right;
    }

    public static class Sub extends NumericValue {
        public NumericValue left;
        public NumericValue right;
    }

    public static class Mul extends NumericValue {
        public NumericValue left;
        public NumericValue right;
    }

    public static class Div extends NumericValue {
        public NumericValue left;
        public NumericValue right;
    }

    public static class Number extends NumericValue {
        public double value;
    }
}