/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic SQL processor
 */
public class SQLSemanticProcessor {

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

    public static String addFiltersToQuery(final DBPDataSource dataSource, String sqlQuery, final DBDDataFilter dataFilter) throws DBException {
        boolean supportSubqueries = dataSource instanceof SQLDataSource && ((SQLDataSource) dataSource).getSQLDialect().supportsSubqueries();
        try {
            Statement statement = CCJSqlParserUtil.parse(sqlQuery);
            if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
                PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
                if (!supportSubqueries || CommonUtils.isEmpty(select.getJoins())) {
                    patchSelectQuery(dataSource, select, dataFilter);
                    return statement.toString();
                }
            }
            return wrapQuery(dataSource, sqlQuery, dataFilter);
        } catch (Throwable e) {
            throw new DBException("SQL parse error", e);
        }
    }

    public static String wrapQuery(final DBPDataSource dataSource, String sqlQuery, final DBDDataFilter dataFilter) throws DBException {
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

    private static void patchSelectQuery(DBPDataSource dataSource, PlainSelect select, DBDDataFilter filter) throws JSQLParserException {
        // WHERE
        FromItem fromItem = select.getFromItem();
        String tableAlias = fromItem.getAlias() == null ? null : fromItem.getAlias().getName();
        if (filter.hasConditions()) {
            StringBuilder whereString = new StringBuilder();
            SQLUtils.appendConditionString(filter, dataSource, tableAlias, whereString, true);
            String condString = whereString.toString();
            addWhereToSelect(select, condString);
        }
        // ORDER
        if (filter.hasOrdering()) {
            List<OrderByElement> orderByElements = select.getOrderByElements();
            if (orderByElements == null) {
                orderByElements = new ArrayList<OrderByElement>();
                select.setOrderByElements(orderByElements);
            }
            Table orderTable = tableAlias == null ? null : new Table(tableAlias);
            for (DBDAttributeConstraint co : filter.getOrderConstraints()) {
                Expression orderExpr = new Column(orderTable, DBUtils.getObjectFullName(co.getAttribute()));
                OrderByElement element = new OrderByElement();
                element.setExpression(orderExpr);
                if (co.isOrderDescending()) {
                    element.setAsc(false);
                    element.setAscDescPresent(true);
                }
                orderByElements.add(element);
            }

        }

    }

    public static void addWhereToSelect(PlainSelect select, String condString) throws JSQLParserException {
        Expression filterWhere;
        try {
            filterWhere = CCJSqlParserUtil.parseCondExpression(condString);
        } catch (JSQLParserException e) {
            throw new JSQLParserException("Bad query condition: [" + condString + "]", e);
        }
        Expression sourceWhere = select.getWhere();
        if (sourceWhere == null) {
            select.setWhere(filterWhere);
        } else {
            select.setWhere(new AndExpression(new Parenthesis(sourceWhere), filterWhere));
        }
    }

}
