/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.transformers;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
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
    private static final String COUNT_WRAP_POSTFIX = "\n) dbvrcnt";

    @NotNull
    @Override
    public SQLQuery transformQuery(
        @NotNull DBPDataSource dataSource,
        @NotNull SQLSyntaxManager syntaxManager,
        @NotNull SQLQuery query
    ) throws DBException {
        try {
            SQLDialect sqlDialect = dataSource.getSQLDialect();
            if (!sqlDialect.supportsSubqueries() || (sqlDialect instanceof SQLDialectRelational sdr && sdr.isAmbiguousCountBroken())) {
                return tryInjectCount(dataSource, query);
            }
        } catch (Throwable e) {
            log.debug("Error injecting count: " + e.getMessage());
            // Inject failed (most likely parser error)
        }
        return wrapSourceQuery(dataSource, syntaxManager, query);
    }

    @NotNull
    private SQLQuery wrapSourceQuery(
        @NotNull DBPDataSource dataSource,
        @NotNull SQLSyntaxManager syntaxManager,
        @NotNull SQLQuery query
    ) throws DBException {
        String queryText = null;

        // Remove orderings (#4652)
        Statement statement = SQLSemanticProcessor.parseQuery(query.getText());
        if (statement instanceof PlainSelect plainSelect) {
            if (!CommonUtils.isEmpty(plainSelect.getOrderByElements())) {
                plainSelect.setOrderByElements(null);
                queryText = statement.toString();
            }
        } else {
            throw new DBCException("Cannot calculate row count for non-SELECT statement '" + query.getText() + "'");
        }
        // Trim query delimiters (#2541)
        if (queryText == null) {
            queryText = query.getText();
        }
        String srcQuery = SQLUtils.trimQueryStatement(syntaxManager, queryText, true);
        String countQuery = COUNT_WRAP_PREFIX + srcQuery + COUNT_WRAP_POSTFIX;
        return new SQLQuery(dataSource, countQuery, query, false);
    }

    @NotNull
    private SQLQuery tryInjectCount(@NotNull DBPDataSource dataSource, @NotNull SQLQuery query) throws DBException {
        try {
            Statement statement = SQLSemanticProcessor.parseQuery(query.getText());
            if (statement instanceof PlainSelect select) {
                if (select.getHaving() != null) {
                    throw new DBException("Can't inject COUNT into query with HAVING clause");
                }
                if (select.getGroupBy() != null && !CommonUtils.isEmpty(select.getGroupBy().getGroupByExpressionList())) {
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
                    for (SelectItem<?> item : select.getSelectItems()) {
                        exprs.add(item.getExpression());
                    }
                    if (!exprs.isEmpty()) {
                        countFunc.setParameters(new ExpressionList<>(exprs));
                    }
                } else {
                    //countFunc.setAllColumns(true); // We can't use setAllColumns now (since JSQLParser 4.2), it will return COUNT(ALL). Replaced by AllColumns Expression
                    countFunc.setParameters(new ExpressionList<>(new AllColumns()));
                }

                List<SelectItem<?>> selectItems = new ArrayList<>();
                selectItems.add(new SelectItem(countFunc));
                select.setSelectItems(selectItems);
                select.setOrderByElements(null);
                return new SQLQuery(dataSource, select.toString(), query, false);
            } else {
                throw new DBException("Query [" + query.getText() + "] can't be modified");
            }
        } catch (DBException e) {
            throw new DBException("Can't transform query to SELECT count(*)", e);
        }
    }
}
