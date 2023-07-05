/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SQLGroupingQueryGenerator {

    private static final Log log = Log.getLog(SQLGroupingQueryGenerator.class);

    public static final String FUNCTION_COUNT = "COUNT";

    public static final String DEFAULT_FUNCTION = FUNCTION_COUNT + "(*)";

    @NotNull
    private final DBPDataSource dataSource;
    @NotNull
    private final DBSDataContainer container;
    @NotNull
    private final SQLDialect dialect;
    @NotNull
    private final SQLSyntaxManager syntaxManager;
    @NotNull
    private final List<String> groupAttributes;
    @NotNull
    private final List<String> groupFunctions;

    private final boolean showDuplicatesOnly;
    private String[] funcAliases = new String[0];

    public SQLGroupingQueryGenerator(
        @NotNull DBPDataSource dataSource,
        @NotNull DBSDataContainer container,
        @NotNull SQLDialect dialect,
        @NotNull SQLSyntaxManager syntaxManager,
        @NotNull List<String> groupAttributes,
        @NotNull List<String> groupFunctions,
        boolean showDuplicatesOnly
    ) {
        this.dataSource = dataSource;
        this.container = container;
        this.dialect = dialect;
        this.syntaxManager = syntaxManager;
        this.groupAttributes = groupAttributes;
        this.groupFunctions = groupFunctions;
        this.showDuplicatesOnly = showDuplicatesOnly;
    }


    public String generateGroupingQuery(String queryText) throws DBException {

        if (queryText == null || queryText.isEmpty()) {
            if (container != null) {
                queryText = container.getName();
            } else {
                throw new DBException("Empty data container");
            }
        }
        for (String delimiter : syntaxManager.getStatementDelimiters()) {
            while (queryText.endsWith(delimiter)) {
                queryText = queryText.substring(0, queryText.length() - delimiter.length());
            }
        }

        boolean useAliasForColumns = dataSource.getSQLDialect().supportsAliasInConditions();
        StringBuilder sql = new StringBuilder();
        funcAliases = new String[groupFunctions.size()];
        for (int i = 0; i < groupFunctions.size(); i++) {
            if (useAliasForColumns) {
                funcAliases[i] = makeGroupFunctionAlias(groupFunctions, i);
            } else {
                funcAliases[i] = groupFunctions.get(i);
            }
        }
        if (!(container instanceof DBSEntity) && dialect.supportsSubqueries()) {
            sql.append("SELECT ");
            for (int i = 0; i < groupAttributes.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(quotedGroupingString(dataSource, groupAttributes.get(i)));
            }
            for (int i = 0; i < groupFunctions.size(); i++) {
                String func = groupFunctions.get(i);
                sql.append(", ").append(func);
                if (useAliasForColumns) {
                    sql.append(" as ").append(funcAliases[i]);
                }
            }
            sql.append(" FROM (\n");
            sql.append(queryText);
            sql.append("\n) src");
        } else {
            try {
                Statement statement = SQLSemanticProcessor.parseQuery(dataSource.getSQLDialect(), queryText);
                if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect) {
                    PlainSelect select = (PlainSelect) ((Select) statement).getSelectBody();
                    select.setOrderByElements(null);

                    List<SelectItem> selectItems = new ArrayList<>();
                    select.setSelectItems(selectItems);
                    for (String groupAttribute : groupAttributes) {
                        selectItems.add(
                            new SelectExpressionItem(
                                new Column(
                                    quotedGroupingString(dataSource, groupAttribute))));
                    }
                    for (int i = 0; i < groupFunctions.size(); i++) {
                        String func = groupFunctions.get(i);
                        Expression expression = SQLSemanticProcessor.parseExpression(func);
                        SelectExpressionItem sei = new SelectExpressionItem(expression);
                        if (useAliasForColumns) {
                            sei.setAlias(new Alias(funcAliases[i]));
                        }
                        selectItems.add(sei);
                    }
                }
                queryText = statement.toString();
            } catch (Throwable e) {
                log.debug("SQL parse error", e);
            }
            sql.append(queryText);
        }

        sql.append("\nGROUP BY ");
        for (int i = 0; i < groupAttributes.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(quotedGroupingString(dataSource, groupAttributes.get(i)));
        }
        boolean isDefaultGrouping = groupFunctions.size() == 1 && groupFunctions.get(0).equals(DEFAULT_FUNCTION);

        if (isDefaultGrouping && showDuplicatesOnly) {
            sql.append("\nHAVING ");
            if (dataSource.getSQLDialect().supportsAliasInHaving()) {
                sql.append(funcAliases[0]);
            } else {
                // very special case
                sql.append(DEFAULT_FUNCTION);
            }
            sql.append(" > 1");
        }
        return sql.toString();
    }

    private String makeGroupFunctionAlias(List<String> groupFunctions, int funcIndex) {
        String function = groupFunctions.get(funcIndex);
        StringBuilder alias = new StringBuilder();
        for (int i = 0; i < function.length(); i++) {
            char c = function.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                alias.append(c);
            }
        }
        if (alias.length() > 0) {
            alias.append('_');
            return alias.toString().toLowerCase(Locale.ENGLISH);
        }
        return "i_" + funcIndex;
    }

    private String quotedGroupingString(DBPDataSource dataSource, String string) {
        try {
            Expression expression = SQLSemanticProcessor.parseExpression(string);
            if (!(expression instanceof Column)) {
                return string;
            }
        } catch (DBException e) {
            log.debug("Can't parse expression " + string, e);
        }
        return DBUtils.getQuotedIdentifier(dataSource, string);
    }

    public String[] getFuncAliases() {
        return funcAliases;
    }

}
