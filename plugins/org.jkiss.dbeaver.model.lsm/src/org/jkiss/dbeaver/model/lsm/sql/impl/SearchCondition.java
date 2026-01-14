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

public abstract class SearchCondition {
    // public abstract BooleanExpression member();


    public abstract static class BooleanExpression {

    }

    public static class OrExpression extends BooleanExpression {
        public List<BooleanExpression> children;
    }

    public static class AndExpression extends BooleanExpression {
        public List<BooleanExpression> children;
    }


    public abstract static class PredicateExpression extends BooleanExpression {
        public boolean isInverted;
        public PredicateClarificationKind clarification;
    }

    public enum PredicateClarificationKind {
        IS_TRUE,
        IS_FALSE,
        IS_UNKNOWN,
        IS_NOT_TRUE,
        IS_NOT_FALSE,
        IS_NOT_UNKNOWN,
    }

    public static final class SubconditionExpression extends PredicateExpression {

        public BooleanExpression child;
    }

    public static final class ComparisonPredicate extends PredicateExpression {
        public Expression.RowValue left;
        public Expression.RowValue right;
    }

    public static final class BetweenPredicate extends PredicateExpression {

    }

    public static final class InPredicate extends PredicateExpression {

    }

    public static final class LikePredicate extends PredicateExpression {

    }

    public static final class NullPredicate extends PredicateExpression {

    }

    public static final class QuantifiedComparisonPredicate extends PredicateExpression {

    }

    public static final class ExistsPredicate extends PredicateExpression {

    }

    public static final class MatchPredicate extends PredicateExpression {

    }

    public static final class OverlapsPredicate extends PredicateExpression {

    }
}