/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.StringProvider;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic SQL processor
 */
public class SQLSemanticProcessor {

    private static final Log log = Log.getLog(SQLSemanticProcessor.class);

    private static final String NESTED_QUERY_AlIAS = "z_q";


    public static Statement parseQuery(@Nullable SQLDialect dialect, @NotNull String sql) throws DBCException {
        CCJSqlParser parser = new CCJSqlParser(new StringProvider(sql));
        try {
            if (dialect != null) {
                // Enable square brackets
                for (String[] qs : ArrayUtils.safeArray(dialect.getIdentifierQuoteStrings())) {
                    if (qs.length == 2 && "[".equals(qs[0]) && "]".equals(qs[1])) {
                        parser.withSquareBracketQuotation(true);
                        break;
                    }
                }
            }
            return parser.Statement();
        } catch (Exception e) {
            throw new DBCException("Error parsing SQL query", e);
        }
    }

    public static boolean isSelectQuery(SQLDialect dialect, String query)
    {
        try {
            Statement statement = parseQuery(dialect, query);
            return
                statement instanceof Select &&
                ((Select) statement).getSelectBody() instanceof PlainSelect &&
                CommonUtils.isEmpty(((PlainSelect) ((Select) statement).getSelectBody()).getIntoTables());
        } catch (Throwable e) {
            //log.debug(e);
            return false;
        }
    }

    // Applying filters changes query formatting (thus it changes column names in expressions)
    // Solution - always wrap query in subselect + add patched WHERE and ORDER
    // It is configurable
    public static String addFiltersToQuery(DBRProgressMonitor monitor, final DBPDataSource dataSource, String sqlQuery, final DBDDataFilter dataFilter) {
        boolean supportSubqueries = dataSource.getSQLDialect().supportsSubqueries();
        if (supportSubqueries && dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.SQL_FILTER_FORCE_SUBSELECT)) {
            return wrapQuery(dataSource, sqlQuery, dataFilter);
        }
        String newQuery = injectFiltersToQuery(monitor, dataSource, sqlQuery, dataFilter);
        if (newQuery == null) {
            // Let's try subquery though.
            return wrapQuery(dataSource, sqlQuery, dataFilter);
        }
        return newQuery;
    }

    public static String injectFiltersToQuery(DBRProgressMonitor monitor, final DBPDataSource dataSource, String sqlQuery, final DBDDataFilter dataFilter) {
        try {
            Statement statement = parseQuery(dataSource.getSQLDialect(), sqlQuery);
            if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
                PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
                if (patchSelectQuery(monitor, dataSource, select, dataFilter)) {
                    return statement.toString();
                }
            }
        } catch (Throwable e) {
            log.debug("SQL parse error", e);
        }
        return null;
    }

    public static String wrapQuery(final DBPDataSource dataSource, String sqlQuery, final DBDDataFilter dataFilter) {
        // Append filter conditions to query
        StringBuilder modifiedQuery = new StringBuilder(sqlQuery.length() + 100);
        modifiedQuery.append("SELECT * FROM (\n");
        modifiedQuery.append(sqlQuery);
        modifiedQuery.append("\n) ").append(NESTED_QUERY_AlIAS);
        if (dataFilter.hasConditions()) {
            modifiedQuery.append(" WHERE ");
            SQLUtils.appendConditionString(dataFilter, dataSource, NESTED_QUERY_AlIAS, modifiedQuery, true);
        }
        if (dataFilter.hasOrdering()) {
            modifiedQuery.append(" ORDER BY "); //$NON-NLS-1$
            SQLUtils.appendOrderString(dataFilter, dataSource, NESTED_QUERY_AlIAS, modifiedQuery);
        }
        return modifiedQuery.toString();
    }

    private static boolean patchSelectQuery(DBRProgressMonitor monitor, DBPDataSource dataSource, PlainSelect select, DBDDataFilter filter) throws JSQLParserException, DBException {
        // WHERE
        if (filter.hasConditions()) {
            for (DBDAttributeConstraint co : filter.getConstraints()) {
                if (co.hasCondition()) {
                    Table table = getConstraintTable(select, co);
                    if (!isValidTableColumn(monitor, dataSource, table, co)) {
                        table = null;
                    }
                    if (table != null) {
                        if (table.getAlias() != null) {
                            co.setEntityAlias(table.getAlias().getName());
                        } else {
                            co.setEntityAlias(table.getName());
                        }
                    } else {
                        co.setEntityAlias(null);
                    }
                }
            }
            StringBuilder whereString = new StringBuilder();
            SQLUtils.appendConditionString(filter, dataSource, null, whereString, true);
            String condString = whereString.toString();
            addWhereToSelect(select, condString);
        }
        // ORDER
        if (filter.hasOrdering()) {
            List<OrderByElement> orderByElements = select.getOrderByElements();
            if (orderByElements == null) {
                orderByElements = new ArrayList<>();
                select.setOrderByElements(orderByElements);
            }
            for (DBDAttributeConstraint co : filter.getOrderConstraints()) {
                String columnName = co.getAttributeName();
                boolean forceNumeric = filter.hasNameDuplicates(columnName) || !SQLUtils.PATTERN_SIMPLE_NAME.matcher(columnName).matches();
                Expression orderExpr = getOrderConstraintExpression(monitor, dataSource, select, co, forceNumeric);
                OrderByElement element = new OrderByElement();
                element.setExpression(orderExpr);
                if (co.isOrderDescending()) {
                    element.setAsc(false);
                    element.setAscDescPresent(true);
                }
                orderByElements.add(element);
            }

        }
        return true;
    }

    private static boolean isValidTableColumn(DBRProgressMonitor monitor, DBPDataSource dataSource, Table table, DBDAttributeConstraint co) throws DBException {
        DBSAttributeBase attribute = co.getAttribute();
        if (attribute instanceof DBDAttributeBinding) {
            attribute = ((DBDAttributeBinding) attribute).getMetaAttribute();
        }
        if (table != null && attribute instanceof DBCAttributeMetaData) {
            DBSEntityAttribute entityAttribute = null;
            DBCEntityMetaData entityMetaData = ((DBCAttributeMetaData) attribute).getEntityMetaData();
            if (entityMetaData != null) {
                DBSEntity entity = DBUtils.getEntityFromMetaData(monitor, DBUtils.getDefaultContext(dataSource, true), entityMetaData);
                if (entity != null) {
                    entityAttribute = entity.getAttribute(monitor, co.getAttributeName());
                }
            }
            // No such attribute in entity. Do not use table prefix (#6927)
            return entityAttribute != null;
        }
        return true;
    }

    private static Expression getOrderConstraintExpression(DBRProgressMonitor monitor, DBPDataSource dataSource, PlainSelect select, DBDAttributeConstraint co, boolean forceNumeric) throws JSQLParserException, DBException {
        Expression orderExpr;
        String attrName = DBUtils.getQuotedIdentifier(dataSource, co.getAttributeName());
        if (forceNumeric || attrName.isEmpty()) {
            orderExpr = new LongValue(co.getOrderPosition());
        } else if (CommonUtils.isJavaIdentifier(attrName)) {
            // Use column table only if there are multiple source tables (joins)
            Table orderTable = CommonUtils.isEmpty(select.getJoins()) ? null : getConstraintTable(select, co);

            if (!isValidTableColumn(monitor, dataSource, orderTable, co)) {
                orderTable = null;
            }

            orderExpr = new Column(orderTable, attrName);
        } else {
            // TODO: set tableAlias for all column references in expression
            orderExpr = CCJSqlParserUtil.parseExpression(attrName);
            //orderExpr = new CustomExpression(attrName);
            //orderExpr = new LongValue(co.getAttribute().getOrdinalPosition() + 1);
        }
        return orderExpr;
    }

    /**
     * Extract alias (or source table name) for specified constraint from SQL select.
     * Searches in FROM and JOIN
     */
    @Nullable
    public static Table getConstraintTable(PlainSelect select, DBDAttributeConstraint constraint) {
        String constrTable;
        DBSAttributeBase ca = constraint.getAttribute();
        if (ca instanceof DBDAttributeBinding) {
            constrTable = ((DBDAttributeBinding) ca).getMetaAttribute().getEntityName();
        } else if (ca instanceof DBSEntityAttribute) {
            constrTable = ((DBSEntityAttribute) ca).getParentObject().getName();
        } else {
            return null;
        }
        if (constrTable == null) {
            return null;
        }
        FromItem fromItem = select.getFromItem();
        Table table = findTableInFrom(fromItem, constrTable);
        if (table == null) {
            // Maybe it is a join
            if (!CommonUtils.isEmpty(select.getJoins())) {
                for (Join join : select.getJoins()) {
                    table = findTableInFrom(join.getRightItem(), constrTable);
                    if (table != null) {
                        break;
                    }
                }
            }
        }
        return table;
    }

    @Nullable
    private static Table findTableInFrom(FromItem fromItem, String tableName) {
        if (fromItem instanceof Table && tableName.equals(((Table) fromItem).getName())) {
            return (Table) fromItem;
        }
        return null;
    }

    public static void addWhereToSelect(PlainSelect select, String condString) throws JSQLParserException {
        Expression filterWhere;
        try {
            filterWhere = CCJSqlParserUtil.parseCondExpression(condString);
        } catch (JSQLParserException e) {
            throw new JSQLParserException("Bad query condition: [" + condString + "]", e);
        }
        addWhereToSelect(select, filterWhere);
    }

    public static void addWhereToSelect(PlainSelect select, Expression conditionExpr) throws JSQLParserException {
        Expression sourceWhere = select.getWhere();
        if (sourceWhere == null) {
            select.setWhere(conditionExpr);
        } else {
            select.setWhere(new AndExpression(new Parenthesis(sourceWhere), conditionExpr));
        }
    }

}
