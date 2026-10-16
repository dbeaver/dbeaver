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
package org.jkiss.dbeaver.model.sql.parser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.*;
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
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQueryGenerator;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Semantic SQL processor
 */
public class SQLSemanticProcessor {

    private static final Log log = Log.getLog(SQLSemanticProcessor.class);

    private static final boolean ALLOW_COMPLEX_PARSING = false;
    private static final int PARSE_FUTURE_TIMEOUT_MS = 1000; // if we can't parse fast, we don't want to

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void shutdownExecutor() {
        if (!executor.shutdownNow().isEmpty()) {
            log.warn("Unexpected awaiting tasks found while terminating JSqlParser executor.");
        }
    }

    @NotNull
    private static CCJSqlParser buildParser(@NotNull SQLDialect dialect, @NotNull String sql) throws DBCException {
        final String sqlWithoutComments = SQLUtils.stripComments(dialect, SQLUtils.maskDialectLineComments(dialect, sql));
        try {
            CCJSqlParser parser = new CCJSqlParser(sqlWithoutComments)
                .withAllowComplexParsing(ALLOW_COMPLEX_PARSING);

            for (String[] qs : ArrayUtils.safeArray(dialect.getIdentifierQuoteStrings())) {
                if (qs.length == 2 && "[".equals(qs[0]) && "]".equals(qs[1])) {
                    parser.withSquareBracketQuotation(true);
                    break;
                }
            }
            // Match MySQL-style backslash escaping so the parser agrees with SQLUtils.maskDialectLineComments
            // on where string literals end (e.g. 'O\'Brien'); otherwise the parser sees a stray closing quote.
            if (dialect.getStringEscapeCharacter() == '\\') {
                parser.withBackslashEscapeCharacter(true);
            }
            return parser;
        } catch (ParseException e) {
            throw new DBCException("Error initializing SQL parser: " + e.getMessage(), e);
        }
    }

    @NotNull
    private static <T> T callWithTimeout(@NotNull CCJSqlParser parser, @NotNull Callable<T> task) throws DBCException {
        Future<T> future = executor.submit(task);
        try {
            return future.get(PARSE_FUTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException e) {
            parser.interrupted = true;
            future.cancel(true);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DBCException("Failed to parse SQL query within reasonable time", e);
        } catch (ExecutionException e) {
            throw new DBCException("Error parsing SQL query: " + e.getMessage(), e);
        }
    }

    @NotNull
    public static List<Statement> parseQueries(@NotNull SQLDialect dialect, @NotNull String sql) throws DBCException {
        CCJSqlParser parser = buildParser(dialect, sql);
        List<Statement> statements = callWithTimeout(parser, parser::Statements);
        statements.forEach(statement -> normalizeQuotedIdentifiers(dialect, ((ASTNodeAccess) statement).getASTNode()));
        return statements;
    }

    @NotNull
    public static Statement parseQuery(@NotNull SQLDialect dialect, @NotNull String sql) throws DBCException {
        CCJSqlParser parser = buildParser(dialect, sql);
        Statement statement = callWithTimeout(parser, parser::Statement);
        normalizeQuotedIdentifiers(dialect, ((ASTNodeAccess) statement).getASTNode());
        return statement;
    }

    @Nullable
    public static Expression parseExpression(@NotNull SQLDialect dialect, @NotNull String expression) throws DBCException {
        return parseExpression(dialect, expression, true);
    }

    @Nullable
    public static Expression parseExpression(
        @NotNull SQLDialect dialect,
        @NotNull String expression,
        boolean allowPartialParse
    ) throws DBCException {
        return parseExpression(dialect, expression, allowPartialParse, "Error parsing conditional SQL expression");
    }
    @Nullable
    public static Expression parseCondExpression(@NotNull SQLDialect dialect, @NotNull String expression) throws DBCException {
        return parseCondExpression(dialect, expression, true);
    }

    @Nullable
    public static Expression parseCondExpression(
        @NotNull SQLDialect dialect,
        @NotNull String expression,
        boolean allowPartialParse
    ) throws DBCException {
        return parseExpression(dialect, expression, allowPartialParse, "Error parsing SQL expression");
    }

    @Nullable
    private static Expression parseExpression(
        @NotNull SQLDialect dialect,
        @NotNull String expression,
        boolean allowPartialParse,
        @NotNull String errorMessage
    ) throws DBCException {
        if (expression.isEmpty()) {
            return null;
        }
        CCJSqlParser parser = buildParser(dialect, expression);
        Expression result = callWithTimeout(parser, () -> {
            Expression parsedExpression = parser.Expression();
            if (!allowPartialParse && parser.getNextToken().kind != CCJSqlParserConstants.EOF) {
                throw new ParseException("Unexpected token after SQL expression");
            }
            return parsedExpression;
        });
        try {
            normalizeQuotedIdentifiers(dialect, ((ASTNodeAccess) result).getASTNode());
            return result;
        } catch (RuntimeException e) {
            throw new DBCException(errorMessage, e);
        }
    }

    private static void normalizeQuotedIdentifiers(@NotNull SQLDialect dialect, @NotNull Node root) {
        // JSqlParser routes quoted names through Table.setName, splitting a single dotted token into multiple parts.
        Set<Object> normalized = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Node> nodes = new ArrayDeque<>();
        nodes.push(root);
        while (!nodes.isEmpty()) {
            Node node = nodes.pop();
            if (node instanceof SimpleNode simpleNode) {
                Object value = simpleNode.jjtGetValue();
                if (value instanceof Table table) {
                    Token token = simpleNode.jjtGetFirstToken();
                    if (token != null && token == simpleNode.jjtGetLastToken() && normalized.add(table)) {
                        restoreQuotedTableName(dialect, table, token.image);
                    }
                } else if (value instanceof Column column && normalized.add(column)) {
                    Token first = simpleNode.jjtGetFirstToken();
                    Token delimiter = first == null ? null : first.next;
                    Token last = simpleNode.jjtGetLastToken();
                    if (delimiter != null && last != null && delimiter.next == last && ".".equals(delimiter.image)) {
                        restoreQuotedTableName(dialect, column.getTable(), first.image);
                    }
                }
            }
            for (int i = node.jjtGetNumChildren() - 1; i >= 0; i--) {
                nodes.push(node.jjtGetChild(i));
            }
        }
    }

    private static void restoreQuotedTableName(
        @NotNull SQLDialect dialect,
        @Nullable Table table,
        @NotNull String token
    ) {
        if (table == null || dialect.treatsQuotedIdentifierAsQualifiedName(token) || !isQuotedDottedIdentifier(dialect, token)) {
            return;
        }
        table.getNameParts().clear();
        table.getNameParts().add(token);
        table.getNamePartDelimiters().clear();
    }

    private static boolean isQuotedDottedIdentifier(@NotNull SQLDialect dialect, @NotNull String identifier) {
        for (String[] quotes : ArrayUtils.safeArray(dialect.getIdentifierQuoteStrings())) {
            if (quotes.length == 2 && !quotes[0].isEmpty() && !quotes[1].isEmpty() &&
                identifier.length() >= quotes[0].length() + quotes[1].length() &&
                identifier.startsWith(quotes[0]) && identifier.endsWith(quotes[1]) &&
                identifier.substring(quotes[0].length(), identifier.length() - quotes[1].length()).contains(".")) {
                return true;
            }
        }
        return false;
    }
    public static boolean isSelectQuery(@NotNull SQLDialect dialect, @NotNull String query) {
        try {
            Statement statement = parseQuery(dialect, query);
            return statement instanceof PlainSelect plainSelect && CommonUtils.isEmpty(plainSelect.getIntoTables());
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
     * @deprecated Use {@link SQLQueryGenerator#getQueryWithAppliedFilters(DBRProgressMonitor, DBPDataSource, String, DBDDataFilter)}
     * instead
     */
    @NotNull
    @Deprecated
    public static String addFiltersToQuery(
        @Nullable DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull String sqlQuery,
        @NotNull DBDDataFilter dataFilter
    ) throws DBException {
        return dataSource.getSQLDialect().getQueryGenerator().getQueryWithAppliedFilters(
            monitor,
            dataSource,
            sqlQuery,
            dataFilter
        );
    }

    public static boolean isForceFilterSubQuery(@NotNull DBPDataSource dataSource) {
        return dataSource.getSQLDialect().supportsSubqueries() && dataSource.getContainer().getPreferenceStore()
            .getBoolean(ModelPreferences.SQL_FILTER_FORCE_SUBSELECT);
    }

    @NotNull
    public static String injectFiltersToQuery(
        @Nullable DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull String sqlQuery,
        @NotNull DBDDataFilter dataFilter
    ) throws DBException {
        try {
            Statement statement = parseQuery(dataSource.getSQLDialect(), sqlQuery);
            if (statement instanceof PlainSelect plainSelect) {
                if (patchSelectQuery(monitor, dataSource, plainSelect, dataFilter)) {
                    return statement.toString();
                } else if (plainSelect.getWithItemsList() != null && !plainSelect.getWithItemsList().isEmpty()) {
                    addWhereCondition(dataSource, plainSelect, dataFilter);
                    if (dataFilter.hasOrdering()) {
                        addOrderByClause(monitor, dataSource, plainSelect, dataFilter);
                    }
                    return statement.toString();
                }
            }
        } catch (Throwable e) {
            throw new DBException("Error parsing SQL query", e);
        }
        throw new DBException("Can't inject filters to a query that is not a plain SELECT statement");
    }

    private static boolean patchSelectQuery(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull PlainSelect select,
        @NotNull DBDDataFilter filter
    ) throws DBException {
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
            addWhereCondition(dataSource, select, filter);
        }
        // ORDER
        if (filter.hasOrdering()) {
            addOrderByClause(monitor, dataSource, select, filter);
        }
        return true;
    }

    private static void addWhereCondition(
        @NotNull DBPDataSource dataSource,
        @NotNull PlainSelect select,
        @NotNull DBDDataFilter filter
    ) throws DBException {
        StringBuilder whereString = new StringBuilder();
        SQLUtils.appendConditionString(filter, dataSource, null, whereString, true);
        String condString = whereString.toString();
        addWhereToSelect(dataSource.getSQLDialect(), select, condString);
    }

    private static void addOrderByClause(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull PlainSelect select,
        @NotNull DBDDataFilter filter
    ) throws DBException {
        List<OrderByElement> orderByElements = select.getOrderByElements();
        if (orderByElements == null) {
            orderByElements = new ArrayList<>();
            select.setOrderByElements(orderByElements);
        }
        List<DBDAttributeConstraint> orderConstraints = filter.getOrderConstraints();
        if (!CommonUtils.isEmpty(orderConstraints)) {
            for (DBDAttributeConstraint co : orderConstraints) {
                Expression orderExpr = getOrderConstraintExpression(monitor, dataSource, select, filter, co);
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

    private static boolean isDynamicAttribute(@Nullable DBSAttributeBase attribute) {
        if (!(attribute instanceof DBDAttributeBinding attributeBinding)) {
            return DBUtils.isDynamicAttribute(attribute);
        }
        return DBUtils.isDynamicAttribute(attributeBinding.getAttribute());
    }

    private static boolean isValidTableColumn(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @Nullable Table table,
        @NotNull DBDAttributeConstraint co
    ) throws DBException {
        DBSAttributeBase attribute = co.getAttribute();

        if (isDynamicAttribute(attribute)) {
            return true;
        }

        if (attribute instanceof DBDAttributeBinding attributeBinding) {
            attribute = attributeBinding.getMetaAttribute();
        }

        if (table != null && attribute instanceof DBCAttributeMetaData attributeMetaData) {
            DBSEntityAttribute entityAttribute = null;
            DBCEntityMetaData entityMetaData = attributeMetaData.getEntityMetaData();
            var defaultContext = DBUtils.getDefaultContext(dataSource, true);
            if (entityMetaData != null && defaultContext != null) {
                DBSEntity entity = DBUtils.getEntityFromMetaData(monitor, defaultContext, entityMetaData);
                if (entity != null) {
                    entityAttribute = entity.getAttribute(monitor, co.getAttributeName());
                }
            }
            // No such attribute in entity. Do not use table prefix (#6927)
            return entityAttribute != null;
        }
        return true;
    }

    @Nullable
    private static Expression getOrderConstraintExpression(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull PlainSelect select,
        @NotNull DBDDataFilter filter,
        @NotNull DBDAttributeConstraint co
    ) throws DBException {
        String columnName = co.getAttributeName();
        boolean supportsIndexOrdering = dataSource.getSQLDialect().supportsColumnIndexOrdering();
        boolean forceNumeric = filter.hasNameDuplicates(columnName) || !SQLUtils.PATTERN_SIMPLE_NAME.matcher(columnName).matches();
        Expression orderExpr;
        String attrName = DBUtils.getQuotedIdentifier(dataSource, co.getAttributeName());
        if (supportsIndexOrdering && (forceNumeric || attrName.isEmpty())) {
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
            orderExpr = SQLSemanticProcessor.parseExpression(dataSource.getSQLDialect(), attrName);
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
    public static Table getConstraintTable(
        @NotNull DBPDataSource dataSource,
        @NotNull PlainSelect select,
        @NotNull DBDAttributeConstraint constraint
    ) {
        String constrTable;
        DBSAttributeBase ca = constraint.getAttribute();
        if (ca instanceof DBDAttributeBinding binding) {
            DBCAttributeMetaData metaAttribute = binding.getMetaAttribute();
            constrTable = metaAttribute == null ? null : metaAttribute.getEntityName();
        } else if (ca instanceof DBSEntityAttribute entityAttribute) {
            constrTable = entityAttribute.getParentObject().getName();
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
    public static Table getTableFromSelect(@Nullable Select select) {
        if (select instanceof PlainSelect plainSelect) {
            FromItem fromItem = plainSelect.getFromItem();
            if (fromItem instanceof Table table) {
                return table;
            }
        }
        return null;
    }

    @Nullable
    private static Table findTableInFrom(@NotNull DBPDataSource dataSource, @Nullable FromItem fromItem, @NotNull String tableName) {
        if (fromItem instanceof Table table && DBUtils.getUnQuotedIdentifier(dataSource, tableName)
            .equals(DBUtils.getUnQuotedIdentifier(dataSource, table.getName()))) {
            return table;
        }
        return null;
    }

    @Nullable
    public static Table findTableByNameOrAlias(@Nullable Select select, @Nullable String tableName) {
        if (select instanceof PlainSelect plainSelect) {
            FromItem fromItem = plainSelect.getFromItem();
            if (fromItem instanceof Table table && equalTables(table, tableName)) {
                return table;
            }
            for (Join join : CommonUtils.safeCollection(plainSelect.getJoins())) {
                if (join.getRightItem() instanceof Table table && equalTables(table, tableName)) {
                    return table;
                }
            }
        }
        return null;
    }

    public static boolean equalTables(@Nullable Table t1, @Nullable String name) {
        if (t1 == null || name == null) {
            return true;
        }
        if (t1.getAlias() != null) {
            return CommonUtils.equalObjects(t1.getAlias().getName(), name);
        } else {
            return CommonUtils.equalObjects(t1.getName(), name);
        }
    }

    public static void addWhereToSelect(@NotNull PlainSelect select, @Nullable String condString) throws DBCException {
        addWhereToSelect(BasicSQLDialect.INSTANCE, select, condString);
    }

    public static void addWhereToSelect(
        @NotNull SQLDialect dialect,
        @NotNull PlainSelect select,
        @Nullable String condString
    ) throws DBCException {
        Expression filterWhere = SQLSemanticProcessor.parseCondExpression(dialect, condString);
        addWhereToSelect(select, filterWhere);
    }

    public static void addWhereToSelect(@NotNull PlainSelect select, @Nullable Expression conditionExpr) {
        Expression sourceWhere = select.getWhere();
        if (sourceWhere == null) {
            select.setWhere(conditionExpr);
        } else {
            select.setWhere(new AndExpression(
                new ParenthesedExpressionList<>(sourceWhere),
                new ParenthesedExpressionList<>(conditionExpr)
            ));
        }
    }


    /**
     * Returns a simple table name in the form of schema.table with proper quoting rules.
     *
     * Examples of transformations:
     * <ul>
     *   <li>{@code a.b    -> a.b}</li>
     *   <li>{@code "a.b" -> "a.b"}</li>
     *   <li>{@code "a".b -> "a".b}</li>
     *   <li>{@code a."b" -> a."b"}</li>
     *   <li>{@code "a"."b" -> "a.b"}</li>
     * </ul>
     *
     * @param select  SQL SELECT statement
     * @param dialect SQL dialect used to check and quote identifiers
     * @return string representation of the table name
     * @throws DBException if the table cannot be determined from the SELECT
     */
    @NotNull
    public static String getSimpleTableName(@NotNull PlainSelect select, @NotNull SQLDialect dialect) throws DBException {
        if (!(select.getFromItem() instanceof Table table)) {
            throw new DBException("Cannot determine table name: FROM is " +
                select.getFromItem().getClass().getSimpleName());
        }
        final String name   = table.getName();
        final String schema = table.getSchemaName();

        if (schema == null || schema.isEmpty()) {
            return name;
        }

        final boolean schemaQuoted = dialect.isQuotedIdentifier(schema);
        final boolean nameQuoted   = dialect.isQuotedIdentifier(name);

        if (schemaQuoted && nameQuoted) {
            final String merged = DBUtils.getUnQuotedIdentifier(schema, "\"")
                + '.'
                + DBUtils.getUnQuotedIdentifier(name, "\"");
            return dialect.getQuotedIdentifier(merged, true, true);
        }

        return schema + '.' + name;
    }

    @NotNull
    public static Token[] parseSqlTextForTokens(@NotNull SQLDialect dialect, @NotNull String sqlText) throws DBCException {
        Statement statement = SQLSemanticProcessor.parseQuery(dialect, sqlText);
        LinkedList<Token> tokensList = new LinkedList<>();
        for (Token t = ((ASTNodeAccess) statement).getASTNode().jjtGetFirstToken(); t != null; t = t.next) {
            if (!t.image.isEmpty()) {
                tokensList.add(t);
            }
        }
        return tokensList.toArray(Token[]::new);
    }
}
