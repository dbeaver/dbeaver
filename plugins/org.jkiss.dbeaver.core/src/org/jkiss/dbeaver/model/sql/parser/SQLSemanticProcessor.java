/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.sql.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.util.List;

/**
 * Semantic SQL processor
 */
public class SQLSemanticProcessor {

    private static final String NESTED_QUERY_AlIAS = "z_q";

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

    public static String patchQuery(final DBPDataSource dataSource, String sql, final DBDDataFilter filter) throws DBException {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                ((Select) statement).getSelectBody().accept(new SelectVisitor() {
                    @Override
                    public void visit(PlainSelect plainSelect) {
                        try {
                            patchSelectQuery(dataSource, plainSelect, filter);
                        } catch (JSQLParserException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void visit(SetOperationList setOpList) {

                    }

                    @Override
                    public void visit(WithItem withItem) {

                    }
                });
            }
            return statement.toString();
        } catch (Exception e) {
            throw new DBException("SQL parse error", e);
        }
    }

    private static void patchSelectQuery(DBPDataSource dataSource, PlainSelect select, DBDDataFilter filter) throws JSQLParserException {
        // WHERE
        FromItem fromItem = select.getFromItem();
        String tableAlias = fromItem.getAlias() == null ? null : fromItem.getAlias().getName();
        StringBuilder whereString = new StringBuilder();
        SQLUtils.appendConditionString(filter, dataSource, tableAlias, whereString, true);
        Expression filterWhere = CCJSqlParserUtil.parseCondExpression(whereString.toString());
        Expression sourceWhere = select.getWhere();
        if (sourceWhere == null) {
            select.setWhere(filterWhere);
        } else {
            select.setWhere(new AndExpression(select.getWhere(), filterWhere));
        }
        // ORDER
        StringBuilder orderString = new StringBuilder();
        SQLUtils.appendOrderString(filter, dataSource, tableAlias, orderString);
        List<OrderByElement> orderByElements = select.getOrderByElements();

    }

}
