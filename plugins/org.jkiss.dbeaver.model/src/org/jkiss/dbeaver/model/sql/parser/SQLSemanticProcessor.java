/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.sql.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic SQL processor
 */
public class SQLSemanticProcessor {

    private static final Log log = Log.getLog(SQLSemanticProcessor.class);

    private static final String NESTED_QUERY_AlIAS = "z_q";

    public static boolean isSelectQuery(String query)
    {
        try {
            Statement statement = CCJSqlParserUtil.parse(query);
            return
                statement instanceof Select &&
                ((Select) statement).getSelectBody() instanceof PlainSelect &&
                CommonUtils.isEmpty(((PlainSelect) ((Select) statement).getSelectBody()).getIntoTables());
        } catch (Throwable e) {
            //log.debug(e);
            return false;
        }
    }

    // FIXME: Applying filters changes query formatting (thus it changes column names in expressions)
    // FIXME: Solution - always wrap query in subselect + add patched WHERE and ORDER
    public static String addFiltersToQuery(final DBPDataSource dataSource, String sqlQuery, final DBDDataFilter dataFilter) {
        boolean supportSubqueries = dataSource instanceof SQLDataSource && ((SQLDataSource) dataSource).getSQLDialect().supportsSubqueries();
        try {
            Statement statement = CCJSqlParserUtil.parse(sqlQuery);
            if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
                PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
                if (patchSelectQuery(dataSource, select, dataFilter)) {
                    return statement.toString();
                }
            }
        } catch (Throwable e) {
            log.debug("SQL parse error", e);
        }
        return wrapQuery(dataSource, sqlQuery, dataFilter);
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

    private static boolean patchSelectQuery(DBPDataSource dataSource, PlainSelect select, DBDDataFilter filter) throws JSQLParserException {
        // WHERE
        if (filter.hasConditions()) {
            for (DBDAttributeConstraint co : filter.getConstraints()) {
                if (co.hasCondition()) {
                    Table table = getConstraintTable(select, co);
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
                Expression orderExpr = getConstraintExpression(select, co);
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

    private static Expression getConstraintExpression(PlainSelect select, DBDAttributeConstraint co) throws JSQLParserException {
        Expression orderExpr;
        String attrName = co.getAttribute().getName();
        if (attrName.isEmpty()) {
            orderExpr = new LongValue(co.getAttribute().getOrdinalPosition() + 1);
        } else if (CommonUtils.isJavaIdentifier(attrName)) {
            // Use column table only if there are multiple source tables (joins)
            Table orderTable = CommonUtils.isEmpty(select.getJoins()) ? null : getConstraintTable(select, co);
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
