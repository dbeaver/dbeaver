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
package org.jkiss.dbeaver.model.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.impl.sql.StandardSQLDialectQueryGenerator;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;

import java.util.Objects;

/**
 * A column or an expression used as a grouping key
 */
public abstract class SQLGroupingAttribute {

    private static final Log log = Log.getLog(SQLGroupingAttribute.class);

    /**
     * Returns the grouping attribute display name
     */
    @NotNull
    public abstract String getDisplayName();

    /**
     * Returns the data source of the query to apply grouping to
     */
    @NotNull
    public abstract DBPDataSource getDataSource();

    /**
     * Generates a query expression string for this grouping attribute
     */
    @NotNull
    public abstract String prepareSqlString(@Nullable String subqueryAlias);

    /**
     * Generates query expression tree for this grouping attribute
     */
    @NotNull
    public abstract Expression prepareExpression();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    /**
     * Creates an instance of the grouping attribute describing sql expression or custom column reference
     *
     * @param dataSource - the data source of the query to apply grouping to
     * @param expressionString - a column name or an expression string to be used as a grouping key
     */
    public static SQLGroupingAttribute makeCustom(@NotNull DBPDataSource dataSource, @NotNull String expressionString) {
        return new CustomAttribute(dataSource, expressionString);
    }

    /**
     * Creates an instance of the grouping attribute provided by the result set attribute binding
     */
    public static SQLGroupingAttribute makeBound(@NotNull DBDAttributeBinding attributeBinding) {
        return new BoundAttribute(attributeBinding);
    }

    public static class CustomAttribute extends SQLGroupingAttribute {
        @NotNull
        private final DBPDataSource dataSource;
        @NotNull
        private final String expressionString;

        public CustomAttribute(@NotNull DBPDataSource dataSource, @NotNull String expressionString) {
            this.dataSource = dataSource;
            this.expressionString = expressionString;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return this.expressionString;
        }

        @Override
        @NotNull
        public DBPDataSource getDataSource() {
            return this.dataSource;
        }

        @NotNull
        public String getExpressionString() {
            return this.expressionString;
        }

        @NotNull
        @Override
        public String prepareSqlString(@Nullable String subqueryAlias) {
            try {
                Expression expression = SQLSemanticProcessor.parseExpression(this.expressionString);
                if (!(expression instanceof Column)) {
                    return this.expressionString; // return valid non-column-ref expression as raw string
                }
            } catch (DBException e) {
                log.debug("Can't parse expression " + this.expressionString, e);
            }
            // treat valid column ref or any invalid expression as identifier
            return DBUtils.getQuotedIdentifier(this.dataSource, this.expressionString);
        }

        @NotNull
        @Override
        public Expression prepareExpression() {
            try {
                return SQLSemanticProcessor.parseExpression(this.expressionString); // return any valid expression as its model
            } catch (DBException e) {
                log.debug("Can't parse expression " + this.expressionString, e);
                // treat any invalid expression as identifier
                return new Column(DBUtils.getQuotedIdentifier(this.dataSource, this.expressionString));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomAttribute that)) {
                return false;
            }
            return Objects.equals(this.expressionString, that.expressionString);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.expressionString);
        }
    }

    public static class BoundAttribute extends SQLGroupingAttribute {
        @NotNull
        private final String displayName;
        @NotNull
        private final DBDAttributeBinding binding;

        public BoundAttribute(@NotNull DBDAttributeBinding binding) {
            this.displayName = getAttributeBindingName(binding);
            this.binding = binding;
        }

        private static String getAttributeBindingName(@NotNull DBDAttributeBinding binding) {
            if (binding instanceof DBDAttributeBindingMeta && binding.getMetaAttribute() != null) {
                return DBUtils.getQuotedIdentifier(binding.getDataSource(), binding.getMetaAttribute().getLabel());
            } else {
                return binding.getFullyQualifiedName(DBPEvaluationContext.DML);
            }
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource() {
            return this.binding.getDataSource();
        }

        @NotNull
        public DBDAttributeBinding getBinding() {
            return this.binding;
        }

        @NotNull
        @Override
        public String prepareSqlString(@Nullable String subqueryAlias) {
            return StandardSQLDialectQueryGenerator.getConstraintAttributeName(
                this.getDataSource(),
                subqueryAlias,
                new DBDAttributeConstraint(this.binding),
                subqueryAlias != null,
                subqueryAlias != null
            );
        }

        @NotNull
        @Override
        public Expression prepareExpression() {
            return new Column(this.prepareSqlString(null));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BoundAttribute that)) {
                return false;
            }
            return Objects.equals(this.binding, that.binding);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.displayName);
        }
    }
}
