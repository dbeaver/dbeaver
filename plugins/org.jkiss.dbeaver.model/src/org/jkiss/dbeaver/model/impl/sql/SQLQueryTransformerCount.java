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
package org.jkiss.dbeaver.model.impl.sql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLQueryTransformerCount.
 * Transforms SQL query into SELECT COUNT(*) query
*/
public class SQLQueryTransformerCount implements SQLQueryTransformer {
    @Override
    public SQLQuery transformQuery(SQLDataSource dataSource, SQLQuery query) throws DBException {
        try {
            Statement statement = CCJSqlParserUtil.parse(query.getQuery());
            if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
                PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
                List<SelectItem> selectItems = new ArrayList<>();
                Function countFunc = new Function();
                countFunc.setName("count");
                countFunc.setParameters(new ExpressionList(Collections.<Expression>singletonList(new Column("*"))));
                SelectItem countItem = new SelectExpressionItem(countFunc);
                selectItems.add(countItem);
                select.setSelectItems(selectItems);
                return new SQLQuery(select.toString(), query, false);
            } else {
                throw new DBException("Query [" + query.getQuery() + "] can't be modified");
            }
        } catch (JSQLParserException e) {
            throw new DBException("Can't transform query to SELECT count(*)", e);
        }
    }
}
