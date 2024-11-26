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
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPAttributeReferencePurpose;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQueryGenerator;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSContextBoundAttribute;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.lang.reflect.Array;
import java.util.List;
import java.util.regex.Pattern;

public class StandardSQLDialectQueryGenerator implements SQLQueryGenerator {
    private static final Log log = Log.getLog(StandardSQLDialectQueryGenerator.class);
    private static final String NESTED_QUERY_AlIAS = "z_q";

    public static StandardSQLDialectQueryGenerator INSTANCE = new StandardSQLDialectQueryGenerator();

    public static final Pattern PATTERN_COLUMN_NAME = Pattern.compile(
        "(([a-z_][a-z0-9_]*)|(\\\"([a-z_][a-z0-9_]*)\\\"))(\\.(([a-z_][a-z0-9_]*)|(\\\"([a-z_][a-z0-9_]*)\\\")))*",
        Pattern.CASE_INSENSITIVE
    );


    @Override
    public void appendQueryConditions(
        DBPDataSource dataSource,
        @NotNull StringBuilder query,
        @Nullable String tableAlias,
        @Nullable DBDDataFilter dataFilter
    ) {
        if (dataFilter != null && dataFilter.hasConditions()) {
            query.append("\nWHERE "); //$NON-NLS-1$
            appendConditionString(dataFilter, dataSource, tableAlias, query, true);
        }
    }

    @Override
    public void appendQueryOrder(
        DBPDataSource dataSource,
        @NotNull StringBuilder query,
        @Nullable String tableAlias,
        @Nullable DBDDataFilter dataFilter
    ) {
        if (dataFilter != null) {
            // Construct ORDER BY
            if (dataFilter.hasOrdering()) {
                query.append("\nORDER BY "); //$NON-NLS-1$
                appendOrderString(dataFilter, dataSource, tableAlias, false, query);
            }
        }
    }

    @Override
    public void appendConditionString(
        @NotNull DBDDataFilter filter,
        @NotNull List<DBDAttributeConstraint> constraints,
        @NotNull DBPDataSource dataSource,
        @Nullable String conditionTable,
        @NotNull StringBuilder query,
        boolean inlineCriteria,
        boolean subQuery
    ) {
        if (filter.isUseDisjunctiveNormalForm() && !constraints.isEmpty()) {
            // TODO: Would be nice to have some asserts here

            var names = constraints.stream()
                .map(constraint -> getConstraintAttributeName(dataSource, conditionTable, constraint, subQuery))
                .toList();

            var values = constraints.stream()
                .map(DBDAttributeConstraintBase::getValue)
                .map(Object[].class::cast)
                .toList();

            var count = values.get(0).length;
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    query.append(" OR ");
                }
                query.append('(');
                for (int j = 0; j < constraints.size(); j++) {
                    if (j > 0) {
                        query.append(" AND ");
                    }
                    query.append(names.get(j)).append(" = ");
                    query.append(getStringValue(dataSource, constraints.get(j), inlineCriteria, values.get(j)[i]));
                }
                query.append(')');
            }
            if (count == 0) {
                // Special care for cases when we have no values. Reflects behavior in the else branch
                for (int i = 0; i < constraints.size(); i++) {
                    if (i > 0) {
                        query.append(" AND ");
                    }
                    query.append(names.get(i)).append(" IS NULL");
                }
            }
        } else {
            final String operator = filter.isAnyConstraint() ? " OR " : " AND ";  //$NON-NLS-1$ $NON-NLS-2$

            for (int index = 0; index < constraints.size(); index++) {
                final DBDAttributeConstraint constraint = constraints.get(index);
                if (index > 0) {
                    query.append(operator);
                }
                if (constraints.size() > 1) {
                    // Add parenthesis for the sake of sanity
                    // Constraint may consist of several conditions and we don't want to break operator precedence
                    query.append('(');
                }

                String attrName = getConstraintAttributeName(dataSource, conditionTable, constraint, subQuery);
                if (constraint.getAttribute() != null) {
                    attrName = dataSource.getSQLDialect().getTypeCastClause(constraint.getAttribute(), attrName, true);
                }
                query
                    .append(attrName)
                    .append(' ')
                    .append(getConstraintCondition(dataSource, constraint, conditionTable, inlineCriteria));
                if (constraints.size() > 1) {
                    query.append(')');
                }
            }

            if (!CommonUtils.isEmpty(filter.getWhere())) {
                if (!constraints.isEmpty()) {
                    query.append(operator).append('(').append(filter.getWhere()).append(')');
                } else {
                    query.append(filter.getWhere());
                }
            }
        }
    }

    @Override
    public @NotNull String getQueryWithAppliedFilters(
        @Nullable DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull String sqlQuery,
        @NotNull DBDDataFilter dataFilter
    ) {
        boolean isForceFilterSubQuery = dataSource.getSQLDialect().supportsSubqueries() && dataSource.getContainer()
            .getPreferenceStore()
            .getBoolean(ModelPreferences.SQL_FILTER_FORCE_SUBSELECT);
        if (isForceFilterSubQuery) {
            return getWrappedFilterQuery(dataSource, sqlQuery, dataFilter);
        }
        String newQuery = SQLSemanticProcessor.injectFiltersToQuery(monitor, dataSource, sqlQuery, dataFilter);
        if (newQuery == null) {
            // Let's try subquery though.
            return getWrappedFilterQuery(dataSource, sqlQuery, dataFilter);
        }
        return newQuery;
    }


    @Override
    @NotNull
    public String getWrappedFilterQuery(
        @NotNull DBPDataSource dataSource,
        @NotNull String sqlQuery,
        @NotNull DBDDataFilter dataFilter
    ) {
        StringBuilder modifiedQuery = new StringBuilder(sqlQuery.length() + 100);
        modifiedQuery.append("SELECT * FROM (\n");
        modifiedQuery.append(sqlQuery);
        modifiedQuery.append("\n) ").append(NESTED_QUERY_AlIAS);
        if (dataFilter.hasConditions()) {
            modifiedQuery.append(" WHERE ");
            SQLUtils.appendConditionString(dataFilter, dataSource, NESTED_QUERY_AlIAS, modifiedQuery, true, true);
        }
        if (dataFilter.hasOrdering()) {
            modifiedQuery.append(" ORDER BY "); //$NON-NLS-1$
            SQLUtils.appendOrderString(dataFilter, dataSource, NESTED_QUERY_AlIAS, true, modifiedQuery);
        }
        return modifiedQuery.toString();
    }

    @Override
    public void appendOrderString(
        @NotNull DBDDataFilter filter,
        @NotNull DBPDataSource dataSource,
        @Nullable String conditionTable,
        boolean subQuery,
        @NotNull StringBuilder query
    ) {
        // Construct ORDER BY
        boolean hasOrder = false;
        for (DBDAttributeConstraint co : filter.getOrderConstraints()) {
            if (hasOrder) query.append(',');
            String orderString = null;
            if (co.isPlainNameReference() || co.getAttribute() == null || co.getAttribute() instanceof DBDAttributeBindingMeta
                || co.getAttribute() instanceof DBDAttributeBindingType) {
                String orderColumn = subQuery ? co.getAttributeLabel() : co.getAttributeName();
                if (canOrderByName(dataSource, co, orderColumn) && !filter.hasNameDuplicates(orderColumn)) {
                    DBSAttributeBase attr = co.getAttribute();
                    if (attr instanceof DBDAttributeBinding attrBinding
                        && attrBinding.getEntityAttribute() instanceof DBSContextBoundAttribute entityAttribute
                    ) {
                        orderString = entityAttribute.formatMemberReference(
                            true, conditionTable, DBPAttributeReferencePurpose.DATA_SELECTION
                        );
                    } else {
                        // It is a simple column.
                        orderString = co.getFullAttributeName();
                        if (conditionTable != null) {
                            orderString = conditionTable + '.' + orderString;
                        }
                    }
                }
            }
            if (orderString == null) {
                // Use position number
                int orderIndex = SQLUtils.getConstraintOrderIndex(filter, co);
                if (orderIndex == -1) {
                    log.debug("Can't generate column order: no name and no position found");
                    continue;
                }
                orderString = String.valueOf(orderIndex);
            }
            query.append(orderString);
            if (co.isOrderDescending()) {
                query.append(" DESC"); //$NON-NLS-1$
            }
            hasOrder = true;
        }
        if (!CommonUtils.isEmpty(filter.getOrder())) {
            if (hasOrder) query.append(',');
            query.append(filter.getOrder());
        }
    }

    private static boolean canOrderByName(@NotNull DBPDataSource dataSource,
        @NotNull DBDAttributeConstraint constraint,
        @NotNull String constraintName
    ) {
        if (constraint.getAttribute() == null) {
            return true;
        }
        if (!dataSource.getSQLDialect().supportsOrderByIndex()) {
            return true;
        }
        return PATTERN_COLUMN_NAME // we should assume columns of composite type like comp.x
            .matcher(constraintName)
            .matches();
    }

    @Nullable
    @Override
    public String getConstraintCondition(
        @NotNull DBPDataSource dataSource,
        @NotNull DBDAttributeConstraint constraint,
        @Nullable String conditionTable,
        boolean inlineCriteria
    ) {
        String criteria = constraint.getCriteria();
        if (!CommonUtils.isEmpty(criteria)) {
            final char firstChar = criteria.trim().charAt(0);
            if (!Character.isLetter(firstChar) && firstChar != '=' && firstChar != '>' && firstChar != '<' && firstChar != '!') {
                return '=' + criteria;
            } else {
                return criteria;
            }
        } else if (constraint.getOperator() != null) {
            DBCLogicalOperator operator = constraint.getOperator();
            StringBuilder conString = new StringBuilder();
            Object value = constraint.getValue();
            if (DBUtils.isNullValue(value)) {
                if (operator.getArgumentCount() == 0) {
                    return operator.getExpression();
                }
                if (dataSource.getSQLDialect().useEmptyStringForNulls()) {
                    conString.append("=''");
                } else {
                    conString.append("IS ");
                    if (constraint.isReverseOperator()) {
                        conString.append("NOT ");
                    }
                    conString.append("NULL");
                }
                return conString.toString();
            }
            if (constraint.isReverseOperator()) {
                conString.append("NOT ");
            }
            if (operator.getArgumentCount() > 0) {

                if (operator.equals(DBCLogicalOperator.EQUALS) && value instanceof Object[] array) {
                    // Special case for multiple values for IN
                    // Generate series of ORed conditions
                    for (int i = 0; i < array.length; i++) {
                        if (i > 0) {
                            conString.append(" OR ");
                            conString.append(DBUtils.getQuotedIdentifier(
                                dataSource,
                                CommonUtils.isEmpty(constraint.getAttributeLabel()) ?
                                    constraint.getAttributeName() :
                                    constraint.getAttributeLabel()
                            ));
                            conString.append(' ');
                        }
                        conString.append(operator.getExpression());
                        String strValue = getStringValue(dataSource, constraint, inlineCriteria, array[i]);
                        conString.append(' ').append(strValue);
                    }
                } else {
                    conString.append(operator.getExpression());
                    for (int i = 0; i < operator.getArgumentCount(); i++) {
                        if (i > 0) {
                            conString.append(" AND");
                        }
                        String strValue = getStringValue(dataSource, constraint, inlineCriteria, value);
                        conString.append(' ').append(strValue);
                    }
                }
            } else if (operator.getArgumentCount() < 0) {
                // Multiple arguments
                int valueCount = Array.getLength(value);
                boolean hasNull = false, hasNotNull = false;
                for (int i = 0; i < valueCount; i++) {
                    final boolean isNull = DBUtils.isNullValue(Array.get(value, i));
                    if (isNull && !hasNull) {
                        hasNull = true;
                    }
                    if (!isNull && !hasNotNull) {
                        hasNotNull = true;
                    }
                }
                if (!hasNotNull) {
                    return "IS NULL";
                }
                if (hasNull) {
                    conString.append("IS NULL OR ");
                    DBSAttributeBase attr = constraint.getAttribute();
                    if (attr instanceof DBDAttributeBinding attrBinding
                        && attrBinding.getEntityAttribute() instanceof DBSContextBoundAttribute entityAttribute
                    ) {
                        conString.append(entityAttribute.formatMemberReference(
                            true, conditionTable, DBPAttributeReferencePurpose.DATA_SELECTION
                        ));
                    } else {
                        if (constraint.getEntityAlias() != null) {
                            conString.append(constraint.getEntityAlias()).append('.');
                        } else if (conditionTable != null) {
                            conString.append(conditionTable).append('.');
                        }
                        conString.append(DBUtils.getObjectFullName(
                            dataSource, constraint.getAttribute(), DBPEvaluationContext.DML, DBPAttributeReferencePurpose.DATA_SELECTION
                        ));
                    }
                    conString.append(" ");
                }

                Pair<String, String> brackets = dataSource.getSQLDialect().getInClauseParentheses();
                conString.append(operator.getExpression());
                conString.append(' ').append(brackets.getFirst());
                if (!value.getClass().isArray()) {
                    value = new Object[] {value};
                }
                boolean hasValue = false;
                for (int i = 0; i < valueCount; i++) {
                    Object itemValue = Array.get(value, i);
                    if (DBUtils.isNullValue(itemValue)) {
                        continue;
                    }
                    if (hasValue) {
                        conString.append(",");
                    }
                    hasValue = true;
                    if (inlineCriteria) {
                        conString.append(SQLUtils.convertValueToSQL(dataSource, constraint.getAttribute(), itemValue));
                    } else {
                        conString.append(dataSource.getSQLDialect().getTypeCastClause(constraint.getAttribute(), "?", true));
                    }
                }
                conString.append(brackets.getSecond());
            }
            return conString.toString();
        } else {
            return null;
        }
    }

    private static String getStringValue(
        @NotNull DBPDataSource dataSource,
        @NotNull DBDAttributeConstraint constraint,
        boolean inlineCriteria,
        Object value
    ) {
        String strValue;
        if (constraint.getAttribute() == null) {
            // We have only attribute name
            if (value instanceof CharSequence) {
                strValue = dataSource.getSQLDialect().getQuotedString(value.toString());
            } else {
                strValue = CommonUtils.toString(value);
            }
        } else if (inlineCriteria) {
            DBDValueHandler valueHandler = DBUtils.findValueHandler(dataSource, constraint.getAttribute());
            strValue = SQLUtils.convertValueToSQL(dataSource, constraint.getAttribute(), valueHandler, value, DBDDisplayFormat.NATIVE, true);
        } else {
            strValue = dataSource.getSQLDialect().getTypeCastClause(constraint.getAttribute(), "?", true);
        }
        return strValue;
    }

    @NotNull
    private static String getConstraintAttributeName(
        @NotNull DBPDataSource dataSource,
        @Nullable String conditionTable,
        @NotNull DBDAttributeConstraint constraint,
        boolean subQuery
    ) {
        // Attribute name could be an expression. So check if this is a real attribute
        // and generate full/quoted name for it.
        DBSAttributeBase cAttr = constraint.getAttribute();
        if (cAttr instanceof DBDAttributeBinding binding) {
            if (binding.getEntityAttribute() != null &&
                binding.getMetaAttribute() != null &&
                binding.getEntityAttribute().getName().equals(binding.getMetaAttribute().getName()) ||
                binding instanceof DBDAttributeBindingType) {
                if (binding.getEntityAttribute() instanceof DBSContextBoundAttribute entityAttribute) {
                    return entityAttribute.formatMemberReference(true, conditionTable, DBPAttributeReferencePurpose.DATA_SELECTION);
                } else {
                    return DBUtils.getObjectFullName(
                        dataSource,
                        binding,
                        DBPEvaluationContext.DML,
                        DBPAttributeReferencePurpose.DATA_SELECTION
                    );
                }
            } else {
                if (binding.getMetaAttribute() == null || binding.getEntityAttribute() != null) {
                    // Seems to a reference on a table column.
                    // It is better to use real table column in expressions because aliases may not work
                    return DBUtils.getQuotedIdentifier(
                        dataSource,
                        subQuery ? constraint.getAttributeLabel() : constraint.getAttributeName()
                    );
                } else {
                    // Most likely it is an expression so we don't want to quote it
                    String metaName = binding.getMetaAttribute().getName();
                    String attrName;
                    if (CommonUtils.isNotEmpty(metaName)) {
                        attrName = binding.getMetaAttribute().getName();
                    } else {
                        // Second option for some databases (like Firebird)
                        attrName = binding.getMetaAttribute().getLabel();
                    }
                    // We must quote it because aliases/column names may contain spaces
                    return DBUtils.getQuotedIdentifier(dataSource, attrName);
                }
            }
        } else if (cAttr != null) {
            return DBUtils.getObjectFullName(dataSource, cAttr, DBPEvaluationContext.DML, DBPAttributeReferencePurpose.DATA_SELECTION);
        } else {
            return DBUtils.getQuotedIdentifier(dataSource, constraint.getAttributeName());
        }
    }

    private StandardSQLDialectQueryGenerator() {

    }

}
