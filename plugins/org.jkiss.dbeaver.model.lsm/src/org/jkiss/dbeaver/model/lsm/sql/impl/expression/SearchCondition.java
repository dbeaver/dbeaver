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

import java.util.List;

public abstract class SearchCondition implements SyntaxSubnodesSpecificationDefinition<SearchCondition.BooleanExpression> {
    private final static String predicatePath = "self::searchCondition[count(./*) = 1]/booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary/predicate/";
    
    @SyntaxSubnode(type = OrExpression.class, xpath = "self::searchCondition[count(./*) > 1]")
    @SyntaxSubnode(type = AndExpression.class, xpath = "self::searchCondition[count(./*) = 1]/booleanTerm[count(./*) > 1]")
    @SyntaxSubnode(type = ComparisonPredicate.class, xpath = predicatePath + "comparisonPredicate")
    @SyntaxSubnode(type = BetweenPredicate.class, xpath = predicatePath + "betweenPredicate")
    @SyntaxSubnode(type = InPredicate.class, xpath = predicatePath + "inPredicate")
    @SyntaxSubnode(type = LikePredicate.class, xpath = predicatePath + "likePredicate")
    @SyntaxSubnode(type = NullPredicate.class, xpath = predicatePath + "nullPredicate")
    @SyntaxSubnode(type = QuantifiedComparisonPredicate.class, xpath = predicatePath + "quantifiedComparisonPredicate")
    @SyntaxSubnode(type = ExistsPredicate.class, xpath = predicatePath + "existsPredicate")
    @SyntaxSubnode(type = MatchPredicate.class, xpath = predicatePath + "matchPredicate")
    @SyntaxSubnode(type = OverlapsPredicate.class, xpath = predicatePath + "overlapsPredicate")
    @SyntaxSubnode(type = SubconditionExpression.class, xpath = "self::searchCondition[count(./*) = 1]/booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary[count(./searchCondition) > 0]")
    public abstract BooleanExpression member();


    public static abstract class BooleanExpression extends AbstractSyntaxNode {

    }

    @SyntaxNode(name = "searchCondition")
    public static class OrExpression extends BooleanExpression {
        private final static String predicatePath = "./booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary/predicate/";
        
        @SyntaxSubnode(type = AndExpression.class, xpath = "./booleanTerm[count(./*) > 1]")
        @SyntaxSubnode(type = ComparisonPredicate.class, xpath = predicatePath + "comparisonPredicate")
        @SyntaxSubnode(type = BetweenPredicate.class, xpath = predicatePath + "betweenPredicate")
        @SyntaxSubnode(type = InPredicate.class, xpath = predicatePath + "inPredicate")
        @SyntaxSubnode(type = LikePredicate.class, xpath = predicatePath + "likePredicate")
        @SyntaxSubnode(type = NullPredicate.class, xpath = predicatePath + "nullPredicate")
        @SyntaxSubnode(type = QuantifiedComparisonPredicate.class, xpath = predicatePath + "quantifiedComparisonPredicate")
        @SyntaxSubnode(type = ExistsPredicate.class, xpath = predicatePath + "existsPredicate")
        @SyntaxSubnode(type = MatchPredicate.class, xpath = predicatePath + "matchPredicate")
        @SyntaxSubnode(type = OverlapsPredicate.class, xpath = predicatePath + "overlapsPredicate")
        @SyntaxSubnode(type = SubconditionExpression.class, xpath = "./booleanTerm[count(./*) = 1]/booleanFactor/booleanTest/booleanPrimary[count(./searchCondition) > 0]")
        public List<BooleanExpression> children;
    }

    @SyntaxNode(name = "booleanTerm")
    public static class AndExpression extends BooleanExpression {
        private final static String predicatePath = "./booleanFactor/booleanTest/booleanPrimary/predicate/";
        
        @SyntaxSubnode(type = ComparisonPredicate.class, xpath = predicatePath + "comparisonPredicate")
        @SyntaxSubnode(type = BetweenPredicate.class, xpath = predicatePath + "betweenPredicate")
        @SyntaxSubnode(type = InPredicate.class, xpath = predicatePath + "inPredicate")
        @SyntaxSubnode(type = LikePredicate.class, xpath = predicatePath + "likePredicate")
        @SyntaxSubnode(type = NullPredicate.class, xpath = predicatePath + "nullPredicate")
        @SyntaxSubnode(type = QuantifiedComparisonPredicate.class, xpath = predicatePath + "quantifiedComparisonPredicate")
        @SyntaxSubnode(type = ExistsPredicate.class, xpath = predicatePath + "existsPredicate")
        @SyntaxSubnode(type = MatchPredicate.class, xpath = predicatePath + "matchPredicate")
        @SyntaxSubnode(type = OverlapsPredicate.class, xpath = predicatePath + "overlapsPredicate")
        @SyntaxSubnode(type = SubconditionExpression.class, xpath = "./booleanFactor/booleanTest/booleanPrimary[count(./searchCondition) > 0]")
        public List<BooleanExpression> children;
    }


    public static abstract class PredicateExpression extends BooleanExpression {
        @SyntaxTerm(xpath = "boolean(../../text()[1]='NOT' or ../../../../text()[1] = 'NOT')")
        public boolean isInverted;

        @SyntaxTerm(xpath = "../truthValue | ../../../truthValue")
        public PredicateClarificationKind clarification;
    }

    @SyntaxLiteral(name = "truthValue", xstring = "x:sourceText('_', \"boolean(local-name()!='booleanPrimary')\", ..)")
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

        @SyntaxSubnodesSpecification(type = SearchCondition.class, xpath = "./searchCondition")
        public BooleanExpression child;
    }

    @SyntaxNode(name = "comparisonPredicate")
    public final static class ComparisonPredicate extends PredicateExpression {

        @SyntaxSubnodesSpecification(type = Expression.RowValueExpressionSpecification.class, xpath = "./rowValueConstructor[1]")
        public Expression.RowValue left;

        @SyntaxSubnodesSpecification(type = Expression.RowValueExpressionSpecification.class, xpath = "./rowValueConstructor[2]")
        public Expression.RowValue right;
    }

    @SyntaxNode(name = "betweenPredicate")
    public final static class BetweenPredicate extends PredicateExpression {

    }

    @SyntaxNode(name = "inPredicate")
    public final static class InPredicate extends PredicateExpression {

    }

    @SyntaxNode(name = "likePredicate")
    public final static class LikePredicate extends PredicateExpression {

    }

    @SyntaxNode(name = "nullPredicate")
    public final static class NullPredicate extends PredicateExpression {

    }

    @SyntaxNode(name = "quantifiedComparisonPredicate")
    public final static class QuantifiedComparisonPredicate extends PredicateExpression {

    }

    @SyntaxNode(name = "existsPredicate")
    public final static class ExistsPredicate extends PredicateExpression {

    }

    @SyntaxNode(name = "matchPredicate")
    public final static class MatchPredicate extends PredicateExpression {

    }

    @SyntaxNode(name = "overlapsPredicate")
    public final static class OverlapsPredicate extends PredicateExpression {

    }
}
