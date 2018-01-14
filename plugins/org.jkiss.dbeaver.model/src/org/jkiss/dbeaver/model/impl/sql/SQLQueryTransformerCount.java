/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLQueryTransformerCount.
 * Transforms SQL query into SELECT COUNT(*) query
*/
public class SQLQueryTransformerCount implements SQLQueryTransformer {

    static protected final Log log = Log.getLog(SQLQueryTransformerCount.class);

    private static final String COUNT_WRAP_PREFIX = "SELECT COUNT(*) FROM (";
    private static final String COUNT_WRAP_POSTFIX = ") dbvrcnt";

    @Override
    public SQLQuery transformQuery(SQLDataSource dataSource, SQLSyntaxManager syntaxManager, SQLQuery query) throws DBException {
        try {
            if (!dataSource.getSQLDialect().supportsSubqueries()) {
                return tryInjectCount(dataSource, query);
            }
        } catch (DBException e) {
            log.debug("Error injecting count: " + e.getMessage());
            // Inject failed (most likely parser error)
        }
        return wrapSourceQuery(dataSource, syntaxManager, query);
    }

    private SQLQuery wrapSourceQuery(SQLDataSource dataSource, SQLSyntaxManager syntaxManager, SQLQuery query) {
        // Trim query delimiters (#2541)
        String srcQuery = SQLUtils.trimQueryStatement(syntaxManager, query.getText(), true);
        String countQuery = COUNT_WRAP_PREFIX + srcQuery + COUNT_WRAP_POSTFIX;
        return new SQLQuery(dataSource, countQuery, query, false);
    }

    private SQLQuery tryInjectCount(SQLDataSource dataSource, SQLQuery query) throws DBException {
        try {
            Statement statement = CCJSqlParserUtil.parse(query.getText());
            if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
                PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
                if (select.getHaving() != null) {
                    throw new DBException("Can't inject COUNT into query with HAVING clause");
                }
                if (!CommonUtils.isEmpty(select.getGroupByColumnReferences())) {
                    throw new DBException("Can't inject COUNT into query with GROUP BY clause");
                }

                Distinct selectDistinct = select.getDistinct();
                if (selectDistinct != null) {
                    // Remove distinct
                    select.setDistinct(null);
                }

                Function countFunc = new Function();
                countFunc.setName("count");
                if (selectDistinct != null) {
                    countFunc.setDistinct(true);
                    List<Expression> exprs = new ArrayList<>();
                    for (SelectItem item : select.getSelectItems()) {
                        if (item instanceof SelectExpressionItem) {
                            exprs.add(((SelectExpressionItem)item).getExpression());
                        }
                    }
                    if (!exprs.isEmpty()) {
                        countFunc.setParameters(new ExpressionList(exprs));
                    }
                }
                countFunc.setAllColumns(true);

                List<SelectItem> selectItems = new ArrayList<>();
                selectItems.add(new SelectExpressionItem(countFunc));
                select.setSelectItems(selectItems);
                select.setOrderByElements(null);
                return new SQLQuery(dataSource, select.toString(), query, false);
            } else {
                throw new DBException("Query [" + query.getText() + "] can't be modified");
            }
        } catch (JSQLParserException e) {
            throw new DBException("Can't transform query to SELECT count(*)", e);
        }
    }
}
