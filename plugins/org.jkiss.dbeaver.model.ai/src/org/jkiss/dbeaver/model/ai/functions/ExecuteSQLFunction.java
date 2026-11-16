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
package org.jkiss.dbeaver.model.ai.functions;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.ASTNodeAccess;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserConstants;
import net.sf.jsqlparser.parser.Node;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.parser.Token;
import net.sf.jsqlparser.parser.TokenMgrException;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.UnsupportedStatement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.utils.AIDataExportUtils;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.exec.output.DBCOutputSeverity;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.sql.exec.SQLScriptProcessor;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class ExecuteSQLFunction implements AIFunction {
    private static final Log log = Log.getLog(ExecuteSQLFunction.class);

    public static final String SQL_PARAM = "sqlQuery";

    protected record ExecutionResult(@NotNull String output, @NotNull DBCStatistics statistics) {
    }

    @NotNull
    protected final AIFunctionType getResultType() {
        return AIFunctionType.INFORMATION;
    }

    /** A semantic rejection is final; text fallback is only for statements the parser cannot represent. */
    protected final void verifyQuery(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull SQLQuery query
    ) throws DBException {
        // Some dialects execute special comments that JSQLParser otherwise ignores.
        if (query.getText().contains("/*!") || query.getText().contains("/*M!") || query.getText().contains("/*m!")) {
            tokenize(query.getText());
        }
        if (query.getStatement() == null || query.getStatement() instanceof UnsupportedStatement) {
            verifyQueryText(context, executionContext, query.getText());
        } else {
            verifyStatement(context, executionContext, query);
        }
    }

    protected abstract void verifyStatement(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull SQLQuery query
    ) throws DBException;

    protected abstract void verifyQueryText(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull String queryText
    ) throws DBException;

    @NotNull
    protected final ExecutionResult executeQueries(
        @NotNull AIFunctionContext context,
        @NotNull Map<String, Object> parameters,
        @Nullable DataTransferProcessorDescriptor processor,
        int maxRows
    ) throws DBException {
        AIDatabaseContext databaseContext = requireDatabaseContext(context);
        String sql = CommonUtils.toString(parameters.get(SQL_PARAM));
        if (CommonUtils.isEmptyTrimmed(sql)) {
            throw new DBException("Parameter '" + SQL_PARAM + "' not specified");
        }
        if (processor != null && maxRows <= 0) {
            throw new DBException("Maximum row count must be greater than zero");
        }
        DBCExecutionContext originalContext = databaseContext.getExecutionContext();
        DBCExecutionContext executionContext = openExecutionContext(context,
            CommonUtils.toString(parameters.get(AIFunctionParams.CATALOG_PARAM), null));
        try {
            return executeVerifiedQueries(context, executionContext, sql, processor, maxRows);
        } finally {
            if (executionContext != originalContext) {
                executionContext.close();
            }
        }
    }

    @NotNull
    private ExecutionResult executeVerifiedQueries(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull String sql,
        @Nullable DataTransferProcessorDescriptor processor,
        int maxRows
    ) throws DBException {
        DBPDataSource dataSource = executionContext.getDataSource();
        List<SQLQuery> queries = SQLScriptParser.parseScript(dataSource, sql).stream()
            .filter(SQLQuery.class::isInstance).map(SQLQuery.class::cast).toList();
        if (queries.isEmpty()) {
            throw new DBException("No SQL statements to execute");
        }
        if (processor != null && queries.size() > 1 && !processor.isAppendable()) {
            throw new DBException("Format '" + processor.getAIFormatId() + "' supports only one statement per call");
        }
        for (SQLQuery query : queries) {
            verifyQuery(context, executionContext, query);
        }
        SQLScriptContext scriptContext = new SQLScriptContext(null, () -> executionContext, null, new LogOutputWriter(), null);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DBCStatistics statistics = new DBCStatistics();
        boolean modifiedRowCountKnown = true;
        for (SQLQuery query : queries) {
            StreamTransferConsumer consumer = null;
            if (processor != null) {
                query.setResultSetLimit(0, maxRows);
                consumer = new StreamTransferConsumer();
                StreamConsumerSettings settings = new StreamConsumerSettings();
                settings.setOutputClipboard(false);
                settings.setOutputEncodingBOM(false);
                settings.setOutputEncoding(StandardCharsets.UTF_8.name());
                consumer.initTransfer(
                    new SQLQueryDataContainer(new DataSourceContextProvider(dataSource), query, scriptContext, log),
                    settings,
                    new IDataTransferConsumer.TransferParameters(false, false, output),
                    AIDataExportUtils.createExporter(processor),
                    AIDataExportUtils.getProperties(processor),
                    dataSource.getContainer().getProject()
                );
            }
            SQLScriptProcessor scriptProcessor = new SQLScriptProcessor(executionContext, List.of(query), scriptContext, consumer, log);
            if (processor != null) {
                scriptProcessor.setMaxRows(maxRows);
            }
            scriptProcessor.runScript(context.getMonitor());
            statistics.accumulate(scriptProcessor.getTotalStatistics());
            modifiedRowCountKnown &= scriptProcessor.getTotalStatistics().getRowsUpdated() >= 0;
            if (consumer != null) {
                consumer.finishTransfer(context.getMonitor(), false);
            }
        }
        if (!modifiedRowCountKnown) {
            statistics.setRowsUpdated(-1);
        }
        return new ExecutionResult(output.toString(StandardCharsets.UTF_8), statistics);
    }

    @NotNull
    protected final AIFunctionResult completionResult(@NotNull ExecutionResult result) {
        return new AIFunctionResult(getResultType(), "Executed " + result.statistics().getStatementsCount() + " statement(s)");
    }

    protected static void requireStatement(boolean accepted, @NotNull SQLQueryCategory category) throws DBException {
        if (!accepted) {
            throw new DBException("This function only accepts " + category + " statements");
        }
    }

    @NotNull
    private static AIDatabaseContext requireDatabaseContext(@NotNull AIFunctionContext context) throws DBException {
        AIDatabaseContext databaseContext = context.getContext();
        if (databaseContext == null) {
            throw new DBException("No database connection");
        }
        return databaseContext;
    }

    protected final void checkObjectScope(
        @NotNull AIFunctionContext context,
        @NotNull DBSObject object
    ) throws DBException {
        AIDatabaseContext databaseContext = requireDatabaseContext(context);
        if (!AIUtils.isObjectInScope(databaseContext, databaseContext.getExecutionContext(), object)) {
            throw new DBException("Database object '" + object.getName() + "' is outside the allowed scope");
        }
    }

    protected final void verifyDatasourceScope(@NotNull AIFunctionContext context) throws DBException {
        if (requireDatabaseContext(context).getScope() != AIDatabaseScope.CURRENT_DATASOURCE) {
            throw new DBException("This statement requires datasource-wide scope");
        }
    }

    protected final void verifyObject(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String name
    ) throws DBException {
        DBSObject object = findObject(context, executionContext, name);
        if (object == null) {
            throw new DBException("Database object '" + name + "' not found");
        }
        checkObjectScope(context, object);
    }

    protected final void verifyDefinitionObject(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull String name,
        @NotNull String type,
        boolean creation
    ) throws DBException {
        if (Set.of("USER", "ROLE", "SYSTEM", "TABLESPACE", "EXTENSION").contains(type)
            || creation && Set.of("DATABASE", "CATALOG").contains(type)) {
            verifyDatasourceScope(context);
            return;
        }
        if (creation && type.equals("SCHEMA") && requireDatabaseContext(context).getScope() == AIDatabaseScope.CURRENT_SCHEMA) {
            throw new DBException("Creating a schema is outside the current-schema scope");
        }
        String[] parts = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect()).splitIdentifier(name);
        if (type.equals("COLUMN") && parts.length > 1) {
            parts = Arrays.copyOf(parts, parts.length - 1);
            name = String.join(".", parts);
        }
        DBSObject object = !creation && Set.of("OBJECT", "OTHER", "TABLE", "VIEW").contains(type)
            ? findObject(context, executionContext, name) : null;
        if (object == null) {
            object = resolveNamespace(context, executionContext, parts, type, creation);
        }
        if (object == null || object instanceof DBPDataSource && requireDatabaseContext(context).getScope() == AIDatabaseScope.CUSTOM) {
            throw new DBException("Cannot determine an allowed namespace for '" + name + "'");
        }
        checkObjectScope(context, object);
    }

    @Nullable
    private DBSObject findObject(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String name
    ) throws DBException {
        DBSObjectContainer root = DBUtils.getAdapter(DBSObjectContainer.class, executionContext.getDataSource());
        if (root == null) {
            throw new DBException("Datasource does not expose database objects");
        }
        String[] parts = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect()).splitIdentifier(name);
        return AIUtils.findObjectByFQN(context.getMonitor(), executionContext, root, parts);
    }

    protected final void verifyRoutine(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String name
    ) throws DBException {
        String[] parts = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect()).splitIdentifier(name);
        DBSObject namespace = resolveNamespace(context, executionContext, parts, "FUNCTION", false);
        DBSProcedureContainer procedures = DBUtils.getAdapter(DBSProcedureContainer.class, namespace);
        String routineName = DBUtils.getUnQuotedNormalizedIdentifier(executionContext.getDataSource().getSQLDialect(), parts[parts.length - 1]);
        DBSObject procedure = procedures == null ? null : procedures.getProcedure(context.getMonitor(), routineName);
        if (procedure == null && procedures != null) {
            // Overloaded routines may use a signature rather than the display name as their unique cache key.
            procedure = DBUtils.findObject(procedures.getProcedures(context.getMonitor()), routineName, true);
        }
        if (procedure == null) {
            throw new DBException("Database routine '" + name + "' not found");
        }
        checkObjectScope(context, procedure);
    }

    @Nullable
    private static DBSObject resolveNamespace(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull String[] parts,
        @NotNull String type,
        boolean creation
    ) throws DBException {
        DBSObjectContainer root = DBUtils.getAdapter(DBSObjectContainer.class, executionContext.getDataSource());
        if (root == null || parts.length == 0) {
            return null;
        }
        parts = Arrays.stream(parts).map(part -> DBUtils.getUnQuotedNormalizedIdentifier(
            executionContext.getDataSource().getSQLDialect(), part)).toArray(String[]::new);
        DBCExecutionContextDefaults<?, ?> defaults = executionContext.getContextDefaults();
        Class<? extends DBSObject> childType = root.getPrimaryChildType(context.getMonitor());
        if (type.equals("SCHEMA")) {
            if (!creation) {
                return DBUtils.getObjectByPath(context.getMonitor(), executionContext, root,
                    parts.length > 1 ? parts[parts.length - 2] : null, parts[parts.length - 1], null, true);
            }
            return parts.length > 1 ? root.getChild(context.getMonitor(), parts[0])
                : defaults != null && defaults.getDefaultCatalog() != null ? defaults.getDefaultCatalog() : root;
        }
        if (type.equals("DATABASE") || type.equals("CATALOG")) {
            return creation ? root : root.getChild(context.getMonitor(), parts[parts.length - 1]);
        }
        if (parts.length == 1) {
            if (defaults != null && defaults.getDefaultSchema() != null) {
                return defaults.getDefaultSchema();
            }
            if (defaults != null && defaults.getDefaultCatalog() != null) {
                return defaults.getDefaultCatalog();
            }
            return DBSCatalog.class.isAssignableFrom(childType) || DBSSchema.class.isAssignableFrom(childType) ? null : root;
        }
        boolean schemas = DBSSchema.class.isAssignableFrom(childType)
            || defaults != null && (defaults.getDefaultSchema() != null || defaults.supportsSchemaChange());
        return DBUtils.getObjectByPath(context.getMonitor(), executionContext, root,
            parts.length > 2 ? parts[parts.length - 3] : schemas ? null : parts[0],
            schemas || parts.length > 2 ? parts[parts.length - 2] : null, null, true);
    }

    protected final void verifyStatementObjects(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull Statement statement
    ) throws DBException {
        Set<String> names = new LinkedHashSet<>();
        Set<String> routines = new LinkedHashSet<>();
        if (statement instanceof Select select && select.getASTNode() != null) {
            collectSelectObjects(select, names, routines);
        } else {
            try {
                names.addAll(objectFinder(routines).getTables(statement));
            } catch (UnsupportedOperationException | ClassCastException e) {
                throw new DBException("Cannot verify objects in this parsed statement", e);
            }
        }
        for (String name : names) {
            verifyObject(context, executionContext, name);
        }
        for (String name : routines) {
            verifyRoutine(context, executionContext, name);
        }
    }

    protected final void verifyExpressionObjects(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull Expression expression
    ) throws DBException {
        Set<String> routines = new LinkedHashSet<>();
        Set<String> names;
        try {
            names = objectFinder(routines).getTables(expression);
        } catch (UnsupportedOperationException | ClassCastException e) {
            throw new DBException("Cannot verify objects in this expression", e);
        }
        for (String name : names) {
            verifyObject(context, executionContext, name);
        }
        for (String name : routines) {
            verifyRoutine(context, executionContext, name);
        }
    }

    @NotNull
    private static TablesNamesFinder<Void> objectFinder(@NotNull Set<String> routines) {
        return new TablesNamesFinder<>() {
            @Override
            public <S> Void visit(@NotNull Function function, @Nullable S unused) {
                if (function.getMultipartName().size() > 1) {
                    routines.add(function.getName());
                }
                return super.visit(function, unused);
            }
        };
    }

    private record SelectScope(@NotNull Node node, @NotNull Set<String> aliases) {
    }

    private static void collectSelectObjects(
        @NotNull Select select, @NotNull Set<String> names, @NotNull Set<String> routines
    ) throws DBException {
        Deque<SelectScope> stack = new ArrayDeque<>();
        stack.push(new SelectScope(select.getASTNode(), Set.of()));
        Set<Select> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (!stack.isEmpty()) {
            SelectScope scope = stack.pop();
            Node node = scope.node();
            Set<String> aliases = scope.aliases();
            Object value = node instanceof SimpleNode simple ? simple.jjtGetValue() : null;
            if (value instanceof Select nested && visited.add(nested)) {
                Set<String> visible = new HashSet<>(aliases);
                for (WithItem<?> item : CommonUtils.safeCollection(nested.getWithItemsList())) {
                    // Definitions have separate AST roots, and their aliases must not escape the enclosing SELECT.
                    Set<String> definitionAliases = new HashSet<>(visible);
                    if (item.isRecursive()) {
                        definitionAliases.add(identifierKey(item.getAliasName()));
                    }
                    if (item.getParenthesedStatement() instanceof ASTNodeAccess definition) {
                        stack.push(new SelectScope(definition.getASTNode(), definitionAliases));
                    } else {
                        throw new DBException("Cannot verify a non-SELECT common-table definition here");
                    }
                    visible.add(identifierKey(item.getAliasName()));
                }
                aliases = visible;
            }
            if (value instanceof Table table && (!CommonUtils.isEmpty(table.getSchemaName())
                || table.getDatabase() != null && !CommonUtils.isEmpty(table.getDatabase().getDatabaseName())
                || !aliases.contains(identifierKey(table.getName())))) {
                names.add(table.getFullyQualifiedName());
            }
            if (value instanceof Function function && function.getMultipartName().size() > 1) {
                routines.add(function.getName());
            }
            for (int i = node.jjtGetNumChildren() - 1; i >= 0; i--) {
                stack.push(new SelectScope(node.jjtGetChild(i), aliases));
            }
        }
    }

    @NotNull
    private static String identifierKey(@NotNull String name) {
        return name.startsWith("\"") || name.startsWith("`") || name.startsWith("[") ? name : name.toUpperCase(Locale.ROOT);
    }

    @NotNull
    protected static List<String> tokenize(@NotNull String sql) throws DBException {
        try {
            CCJSqlParser lexer = new CCJSqlParser(sql).withSquareBracketQuotation(true).withBackslashEscapeCharacter(true);
            List<String> tokens = new ArrayList<>();
            while (true) {
                Token token = lexer.getNextToken();
                for (Token comment = token.specialToken; comment != null; comment = comment.specialToken) {
                    String text = comment.image.toUpperCase(Locale.ROOT);
                    if (text.startsWith("/*!") || text.startsWith("/*M!")) {
                        throw new DBException("Cannot verify executable SQL comments");
                    }
                }
                if (token.kind == CCJSqlParserConstants.EOF) {
                    return tokens;
                }
                tokens.add(token.image);
            }
        } catch (ParseException | TokenMgrException e) {
            throw new DBException("Cannot tokenize SQL statement", e);
        }
    }

    @NotNull
    protected static String firstKeyword(@NotNull List<String> tokens) {
        return tokens.isEmpty() ? "" : tokens.getFirst().toUpperCase(Locale.ROOT);
    }

    protected static boolean containsKeyword(@NotNull List<String> tokens, @NotNull Set<String> words) {
        for (int i = 0; i < tokens.size(); i++) {
            if ((i == 0 || !tokens.get(i - 1).equals(".")) && words.contains(tokens.get(i).toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    protected static int skipParentheses(@NotNull List<String> tokens, int start) {
        int depth = 0;
        for (int i = start; i < tokens.size(); i++) {
            if (tokens.get(i).equals("(")) {
                depth++;
            } else if (tokens.get(i).equals(")") && --depth == 0) {
                return i + 1;
            }
        }
        return tokens.size();
    }

    protected record Identifier(@NotNull String name, int end) {
    }

    @Nullable
    protected static Identifier readIdentifier(@NotNull List<String> tokens, int start) {
        if (start >= tokens.size() || !isIdentifier(tokens.get(start))) {
            return null;
        }
        StringBuilder name = new StringBuilder(tokens.get(start++));
        while (start + 1 < tokens.size() && tokens.get(start).equals(".") && isIdentifier(tokens.get(start + 1))) {
            name.append('.').append(tokens.get(start + 1));
            start += 2;
        }
        return new Identifier(name.toString(), start);
    }

    private static boolean isIdentifier(@NotNull String token) {
        return !token.isEmpty() && (token.startsWith("\"") || token.startsWith("`") || token.startsWith("[")
            || (Character.isJavaIdentifierStart(token.charAt(0)) || token.charAt(0) == '#')
                && !Set.of("SELECT", "FROM", "JOIN", "WHERE", "ON", "AS", "TO", "SET", "VALUES", "USING", "ONLY")
                    .contains(token.toUpperCase(Locale.ROOT)));
    }

    @NotNull
    protected static Set<String> findTextObjects(@NotNull List<String> tokens, @NotNull Set<String> prefixes) {
        Set<String> names = new LinkedHashSet<>();
        Set<Integer> fromDepths = new HashSet<>();
        int depth = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).toUpperCase(Locale.ROOT);
            if (token.equals("(")) {
                depth++;
            } else if (token.equals(")")) {
                fromDepths.remove(depth--);
            } else if (Set.of("WHERE", "SET", "GROUP", "ORDER", "HAVING", "LIMIT", "RETURNING", "VALUES", "UNION", "EXCEPT", "INTERSECT")
                .contains(token)) {
                fromDepths.remove(depth);
            }
            if ((prefixes.contains(token) && (i == 0 || !tokens.get(i - 1).equals(".")))
                || token.equals(",") && fromDepths.contains(depth)) {
                int start = i + 1;
                while (start < tokens.size() && Set.of("ONLY", "LATERAL", "LOW_PRIORITY", "IGNORE", "OVERWRITE")
                    .contains(tokens.get(start).toUpperCase(Locale.ROOT))) {
                    start++;
                }
                Identifier name = readIdentifier(tokens, start);
                if (name != null) {
                    names.add(name.name());
                    i = name.end() - 1;
                }
                if (token.equals("FROM") || token.equals("UPDATE")) {
                    fromDepths.add(depth);
                }
            }
        }
        return names;
    }

    protected final void verifyTextObjects(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<String> tokens,
        @NotNull Set<String> prefixes
    ) throws DBException {
        Set<String> names = findTextObjects(tokens, prefixes);
        if (names.isEmpty()) {
            throw new DBException("Cannot determine database objects in this SQL statement");
        }
        for (String name : names) {
            verifyObject(context, executionContext, name);
        }
        verifyTextRoutines(context, executionContext, tokens, Set.of());
    }

    protected final void verifyTextRoutines(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<String> tokens,
        @NotNull Set<String> ignoredNames
    ) throws DBException {
        for (int i = 0; i < tokens.size(); i++) {
            Identifier name = readIdentifier(tokens, i);
            if (name != null && name.end() < tokens.size() && tokens.get(name.end()).equals("(")
                && name.name().contains(".") && !ignoredNames.contains(name.name())) {
                verifyRoutine(context, executionContext, name.name());
                i = name.end() - 1;
            }
        }
    }

    @NotNull
    protected final DBCExecutionContext openExecutionContext(
        @NotNull AIFunctionContext context, @Nullable String catalogName
    ) throws DBException {
        DBCExecutionContext original = requireDatabaseContext(context).getExecutionContext();
        if (CommonUtils.isEmpty(catalogName)) {
            return original;
        }
        DBPDataSource dataSource = original.getDataSource();
        DBSObjectContainer root = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        DBSObject object = root == null ? null : root.getChild(context.getMonitor(), catalogName);
        DBSInstance instance = dataSource.getAvailableInstances().stream()
            .filter(candidate -> candidate.getName().equals(catalogName)).findFirst().orElse(null);
        if (instance == null && !(object instanceof DBSCatalog)) {
            throw new DBException("Catalog '" + catalogName + "' not found");
        }
        checkObjectScope(context, object instanceof DBSCatalog ? object : instance);
        if (instance == null && object instanceof DBSInstance catalogInstance) {
            instance = catalogInstance;
        }
        if (instance != null) {
            return instance.openIsolatedContext(context.getMonitor(), "MCP SQL execution", original);
        }
        DBCExecutionContext isolated = original.getOwnerInstance().openIsolatedContext(context.getMonitor(), "MCP SQL execution", original);
        if (isolated == original) {
            throw new DBException("Isolated catalog selection is not supported by this datasource");
        }
        try {
            var defaults = isolated.getContextDefaults();
            if (defaults == null || !defaults.supportsCatalogChange()) {
                throw new DBException("Catalog selection is not supported by this execution context");
            }
            // Structural catalogs (for example MySQL databases) need not be DBSInstance objects.
            defaults.setDefaultCatalog(context.getMonitor(), (DBSCatalog) object, null);
            return isolated;
        } catch (DBException | RuntimeException e) {
            isolated.close();
            throw e;
        }
    }

    private static class LogOutputWriter implements DBCOutputWriter {
        @Override
        public void println(@Nullable DBCOutputSeverity severity, @Nullable String message) {
            if (message != null) {
                log.debug("MCP SQL execution: " + message);
            }
        }

        @Override
        public void flush() {
        }
    }
}
