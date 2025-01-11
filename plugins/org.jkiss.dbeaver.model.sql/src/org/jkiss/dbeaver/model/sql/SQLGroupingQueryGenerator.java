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
package org.jkiss.dbeaver.model.sql;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.utils.CommonUtils;

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
    private final List<SQLGroupingAttribute> groupAttributes;
    @NotNull
    private final List<String> groupFunctions;

    private final boolean showDuplicatesOnly;
    private String[] funcAliases = new String[0];

    public SQLGroupingQueryGenerator(
        @NotNull DBPDataSource dataSource,
        @NotNull DBSDataContainer container,
        @NotNull SQLDialect dialect,
        @NotNull SQLSyntaxManager syntaxManager,
        @NotNull List<SQLGroupingAttribute> groupAttributes,
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
        String subqueryAlias;
        Statement statement = null;
        try {
            statement = SQLSemanticProcessor.parseQuery(dataSource.getSQLDialect(), queryText);
        } catch (Throwable e) {
            log.debug("SQL parse error", e);
        }
        boolean isCTE = statement instanceof Select select && !select.getWithItemsList().isEmpty();
        if (!(container instanceof DBSEntity) && dialect.supportsSubqueries() && !isCTE) {
            subqueryAlias = "src";
            sql.append("SELECT ");
            for (int i = 0; i < groupAttributes.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(groupAttributes.get(i).prepareSqlString(subqueryAlias));
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
            sql.append("\n) ").append(subqueryAlias);
        } else {
            subqueryAlias = null;
            if (statement instanceof Select && ((Select) statement).getSelectBody() instanceof PlainSelect select) {
                select.setOrderByElements(null);
                SQLDialect sqlDialect = dataSource.getSQLDialect();
                if (select.getFromItem() instanceof Table table) {
                    FormattedTable formattedTable = new FormattedTable(table, sqlDialect);
                    // implicitly parsed where-conditions might have use table alias if presented,
                    // so don't forget it while replacing the table reference
                    formattedTable.setAlias(table.getAlias());
                    select.setFromItem(formattedTable);
                }

                List<SelectItem> selectItems = new ArrayList<>();
                select.setSelectItems(selectItems);
                for (SQLGroupingAttribute groupAttribute : groupAttributes) {
                    selectItems.add(new SelectExpressionItem(groupAttribute.prepareExpression()));
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
            if (statement != null) {
                queryText = statement.toString();
                sql.append(queryText);
            }
        }

        sql.append("\nGROUP BY ");
        for (int i = 0; i < groupAttributes.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(groupAttributes.get(i).prepareSqlString(subqueryAlias));
        }
        boolean isDefaultGrouping = groupFunctions.size() == 1 && groupFunctions.get(0).equalsIgnoreCase(DEFAULT_FUNCTION);

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

    public String[] getFuncAliases() {
        return funcAliases;
    }

    /**
     * Represents a formatted table in an SQL query.
     * This class is used to format the table name according to the specified SQL dialect.
     * It is necessary because `net.sf.jsqlparser.schema.Table` only supports a dot as a separator.
     */
    // TODO: Remove this class when https://github.com/dbeaver/pro/issues/3140 will be resolved
    private static class FormattedTable extends Table {
        private final SQLDialect sqlDialect;

        public FormattedTable(Table table, SQLDialect sqlDialect) {
            super(table.getDatabase(), table.getSchemaName(), table.getName());
            this.sqlDialect = sqlDialect;
        }

        @Override
        public String getFullyQualifiedName() {
            String databaseName = !CommonUtils.isEmpty(getDatabase().getDatabaseName())
                    ? getDatabase().getDatabaseName() + sqlDialect.getCatalogSeparator()
                    : "";
            String schemaName = getSchemaName() != null ? getSchemaName() + sqlDialect.getStructSeparator() : "";
            return databaseName + schemaName + getName();
        }
    }
}
