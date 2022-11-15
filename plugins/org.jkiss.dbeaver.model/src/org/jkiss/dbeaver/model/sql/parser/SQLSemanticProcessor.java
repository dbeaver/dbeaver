/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.impl.sql.SQLDialectQueryGenerator;
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

    private static final boolean ALLOW_COMPLEX_PARSING = false;

    public static Statement parseQuery(@Nullable SQLDialect dialect, @NotNull String sql) throws DBCException {
        CCJSqlParser parser = new CCJSqlParser(new StringProvider(sql));
        try {
            parser.withAllowComplexParsing(ALLOW_COMPLEX_PARSING);
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
            throw new DBCException("Error parsing SQL query: " + e.getMessage(), e);
        }
    }

    public static Statement parseQuery(@NotNull String sql) throws DBCException {
        return parseQuery(null, sql);
    }

    public static Expression parseExpression(String expression) throws DBCException {
        return parseExpression(expression, true);
    }

    public static Expression parseExpression(String expression, boolean allowPartialParse) throws DBCException {
        try {
            return CCJSqlParserUtil.parseExpression(expression, allowPartialParse);
        } catch (JSQLParserException e) {
            throw new DBCException("Error parsing conditional SQL expression", e);
        }
    }

    public static Expression parseCondExpression(String expression) throws DBCException {
        return parseCondExpression(expression, true);
    }

    public static Expression parseCondExpression(String expression, boolean allowPartialParse) throws DBCException {
        try {
            return CCJSqlParserUtil.parseCondExpression(expression, allowPartialParse);
        } catch (JSQLParserException e) {
            throw new DBCException("Error parsing SQL expression", e);
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

    /**
     *  Applying filters changes query formatting (thus it changes column names in expressions)
     *  Solution - always wrap query in subselect + add patched WHERE and ORDER
     *  It is configurable
     *
     * @deprecated Use {@link SQLDialectQueryGenerator#getQueryWithAppliedFilters(DBRProgressMonitor, DBPDataSource, String, DBDDataFilter)} instead
     */
    @Deprecated
    public static String addFiltersToQuery(DBRProgressMonitor monitor, final DBPDataSource dataSource, String sqlQuery, final DBDDataFilter dataFilter) {
        return dataSource.getSQLDialect().getQueryGenerator().getQueryWithAppliedFilters(monitor, dataSource, sqlQuery,
            dataFilter);
    }

    public static boolean isForceFilterSubQuery(DBPDataSource dataSource) {
        return dataSource.getSQLDialect().supportsSubqueries() && dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.SQL_FILTER_FORCE_SUBSELECT);
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


    /**
     *
     * @deprecated Use {@link SQLDialectQueryGenerator#getWrappedFilterQuery(DBPDataSource, String, DBDDataFilter)} instead
     */
    public static String wrapQuery(final DBPDataSource dataSource, String sqlQuery, final DBDDataFilter dataFilter) {
        return dataSource.getSQLDialect().getQueryGenerator().getWrappedFilterQuery(dataSource, sqlQuery, dataFilter);
    }

    private static boolean patchSelectQuery(DBRProgressMonitor monitor, DBPDataSource dataSource, PlainSelect select, DBDDataFilter filter) throws JSQLParserException, DBException {
        // WHERE
        if (filter.hasConditions()) {
            for (DBDAttributeConstraint co : filter.getConstraints()) {
                if (co.hasCondition() && !isDynamicAttribute(co.getAttribute())) {
                    Table table = getConstraintTable(dataSource, select, co);
                    if (!isValidTableColumn(monitor, dataSource, table, co)) {
                        return false;
                    }
                    if (table != null) {
                        if (table.getAlias() != null) {
                            co.setEntityAlias(table.getAlias().getName());
                        } else {
                            co.setEntityAlias(table.getName());
                        }
                    } else {
                        return false;
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
            List<DBDAttributeConstraint> orderConstraints = filter.getOrderConstraints();
            if (!CommonUtils.isEmpty(orderConstraints)) {
                for (DBDAttributeConstraint co : orderConstraints) {
                    String columnName = co.getAttributeName();
                    boolean forceNumeric = filter.hasNameDuplicates(columnName) || !SQLUtils.PATTERN_SIMPLE_NAME.matcher(columnName).matches();
                    Expression orderExpr = getOrderConstraintExpression(monitor, dataSource, select, filter, co, forceNumeric);
                    OrderByElement element = new OrderByElement();
                    element.setExpression(orderExpr);
                    if (co.isOrderDescending()) {
                        element.setAsc(false);
                        element.setAscDescPresent(true);
                    }
                    orderByElements.add(element);
                }
            }
            String filterOrder = filter.getOrder();
            if (!CommonUtils.isEmpty(filterOrder)) {
                // expression = CCJSqlParserUtil.parseExpression(filterOrder);
                // It's good place to use parseExpression, but it parse fine just one column name, not "column1,column2" or "column1 DESC"
                Expression expression = new CustomExpression(filterOrder);
                OrderByElement element = new OrderByElement();
                element.setExpression(expression);
                orderByElements.add(element);
            }

        }
        return true;
    }

    private static boolean isDynamicAttribute(@Nullable DBSAttributeBase attribute) {
        if (!(attribute instanceof DBDAttributeBinding)) {
            return DBUtils.isDynamicAttribute(attribute);
        }
        DBDAttributeBinding attributeBinding = ((DBDAttributeBinding) attribute);
        return DBUtils.isDynamicAttribute(attributeBinding.getAttribute());
    }

    private static boolean isValidTableColumn(DBRProgressMonitor monitor, DBPDataSource dataSource, Table table, DBDAttributeConstraint co) throws DBException {
        DBSAttributeBase attribute = co.getAttribute();

        if (isDynamicAttribute(attribute)) {
            return true;
        }

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

    private static Expression getOrderConstraintExpression(DBRProgressMonitor monitor, DBPDataSource dataSource, PlainSelect select, DBDDataFilter filter, DBDAttributeConstraint co, boolean forceNumeric) throws DBException {
        Expression orderExpr;
        String attrName = DBUtils.getQuotedIdentifier(dataSource, co.getAttributeName());
        if (forceNumeric || attrName.isEmpty()) {
            int orderColumnIndex = SQLUtils.getConstraintOrderIndex(filter, co);
            if (orderColumnIndex == -1) {
                throw new DBException("Can't generate column order: no position found");
            }
            orderExpr = new LongValue(orderColumnIndex);
        } else if (CommonUtils.isJavaIdentifier(attrName)) {
            // Use column table only if there are multiple source tables (joins)
            Table orderTable = CommonUtils.isEmpty(select.getJoins()) ? null : getConstraintTable(dataSource, select, co);

            if (!isValidTableColumn(monitor, dataSource, orderTable, co)) {
                orderTable = null;
            }

            orderExpr = new Column(orderTable, attrName);
        } else {
            // TODO: set tableAlias for all column references in expression
            orderExpr = SQLSemanticProcessor.parseExpression(attrName);
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
    public static Table getConstraintTable(DBPDataSource dataSource, PlainSelect select, DBDAttributeConstraint constraint) {
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
        Table table = findTableInFrom(dataSource, fromItem, constrTable);
        if (table == null) {
            // Maybe it is a join
            if (!CommonUtils.isEmpty(select.getJoins())) {
                for (Join join : select.getJoins()) {
                    table = findTableInFrom(dataSource, join.getRightItem(), constrTable);
                    if (table != null) {
                        break;
                    }
                }
            }
        }
        return table;
    }

    @Nullable
    public static Table getTableFromSelect(Select select) {
        if (select.getSelectBody() instanceof PlainSelect) {
            FromItem fromItem = ((PlainSelect) select.getSelectBody()).getFromItem();
            if (fromItem instanceof Table) {
                return (Table) fromItem;
            }
        }
        return null;
    }

    @Nullable
    private static Table findTableInFrom(DBPDataSource dataSource, FromItem fromItem, String tableName) {
        if (fromItem instanceof Table && 
            DBUtils.getUnQuotedIdentifier(dataSource, tableName).equals(DBUtils.getUnQuotedIdentifier(dataSource, ((Table) fromItem).getName()))) {
            return (Table) fromItem;
        }
        return null;
    }

    @Nullable
    public static Table findTableByNameOrAlias(Select select, String tableName) {
        SelectBody selectBody = select.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            FromItem fromItem = ((PlainSelect) selectBody).getFromItem();
            if (fromItem instanceof Table && equalTables((Table) fromItem, tableName)) {
                return (Table) fromItem;
            }
            for (Join join : CommonUtils.safeCollection(((PlainSelect) selectBody).getJoins())) {
                if (join.getRightItem() instanceof Table && equalTables((Table) join.getRightItem(), tableName)) {
                    return (Table) join.getRightItem();
                }
            }
        }
        return null;
    }

    public static boolean equalTables(Table t1, String name) {
        if (t1 == null || name == null) {
            return true;
        }
        if (t1.getAlias() != null) {
            return CommonUtils.equalObjects(t1.getAlias().getName(), name);
        } else {
            return CommonUtils.equalObjects(t1.getName(), name);
        }
    }

    public static void addWhereToSelect(PlainSelect select, String condString) throws DBCException {
        Expression filterWhere = SQLSemanticProcessor.parseCondExpression(condString);
        addWhereToSelect(select, filterWhere);
    }

    public static void addWhereToSelect(PlainSelect select, Expression conditionExpr) {
        Expression sourceWhere = select.getWhere();
        if (sourceWhere == null) {
            select.setWhere(conditionExpr);
        } else {
            select.setWhere(new AndExpression(new Parenthesis(sourceWhere), conditionExpr));
        }
    }

}
