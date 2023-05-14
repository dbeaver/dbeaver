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

import java.util.ArrayList;
import java.util.List;

public abstract class SearchCondition implements SyntaxSubnodesSpecificationDefinition<SearchCondition.BooleanExpression> {

    @SyntaxSubnode(type = OrExpression.class, xpath = "self::searchCondition[count(./*) > 1]")
    @SyntaxSubnode(type = AndExpression.class, xpath = "self::searchCondition[count(./*) = 1]/booleanTerm[count(./*) > 1]")
    @SyntaxSubnode(type = ComparisonExpression.class, xpath = "self::searchCondition[count(./*) = 1]/booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary/predicate/comparisonPredicate")
    @SyntaxSubnode(type = SubconditionExpression.class, xpath = "self::searchCondition[count(./*) = 1]/booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary[count(./searchCondition) > 0]")
    public abstract BooleanExpression member();


    public static abstract class BooleanExpression extends AbstractSyntaxNode {

    }

    @SyntaxNode(name = "searchCondition")
    public static class OrExpression extends BooleanExpression {
        @SyntaxSubnode(type = AndExpression.class, xpath = "./booleanTerm[count(./*) > 1]")
        @SyntaxSubnode(type = ComparisonExpression.class, xpath = "./booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary/predicate/comparisonPredicate")
        @SyntaxSubnode(type = SubconditionExpression.class, xpath = "./booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary[count(./searchCondition) > 0]")
        public List<BooleanExpression> children;
    }

    @SyntaxNode(name = "booleanTerm")
    public static class AndExpression extends BooleanExpression {
        @SyntaxSubnode(type = ComparisonExpression.class, xpath = "./booleanFactor/booleanTest/booleanPrimary/predicate/comparisonPredicate")
        @SyntaxSubnode(type = SubconditionExpression.class, xpath = "./booleanFactor/booleanTest/booleanPrimary[count(./searchCondition) > 0]")
        public List<BooleanExpression> children;
    }


    public static abstract class PredicateExpression extends BooleanExpression {
        @SyntaxTerm(xpath = "boolean(../../text()[1]='NOT' or ../../../../text()[1] = 'NOT')")
        public boolean isInverted;

        @SyntaxTerm(xpath = "../truthValue | ../../../truthValue")
        public PredicateClarificationKind clarification;
    }

    @SyntaxLiteral(name = "truthValue", xstring = "x:joinStrings('_', x:echo(..//text()))")
    public enum PredicateClarificationKind {
        IS_TRUE,
        IS_FALSE,
        IS_UNKNOWN,
        IS_NOT_TRUE,
        IS_NOT_FALSE,
        IS_NOT_UNKNOWN,
    }

    @SyntaxNode(name = "booleanPrimary")
    public final static class SubconditionExpression extends PredicateExpression {
        @SyntaxSubnode(type = OrExpression.class, xpath = "./searchCondition[count(./*) > 1]")
        @SyntaxSubnode(type = AndExpression.class, xpath = "./searchCondition[count(./*) = 1]/booleanTerm[count(./*) > 1]")
        @SyntaxSubnode(type = ComparisonExpression.class, xpath = "./searchCondition[count(./*) = 1]/booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary/predicate/comparisonPredicate")
        @SyntaxSubnode(type = SubconditionExpression.class, xpath = "./searchCondition[count(./*) = 1]/booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary[count(./searchCondition) > 0]")
        public BooleanExpression child;
    }

    @SyntaxNode(name = "comparisonPredicate")
    public final static class ComparisonExpression extends PredicateExpression {

    }
}
