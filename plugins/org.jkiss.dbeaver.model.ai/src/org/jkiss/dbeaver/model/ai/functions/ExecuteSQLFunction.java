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

import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.ASTNodeAccess;
import net.sf.jsqlparser.parser.Node;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
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
import org.jkiss.dbeaver.model.ai.functions.heuristics.SQLScopeHeuristics;
import org.jkiss.dbeaver.model.ai.utils.AIDataExportUtils;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.exec.output.DBCOutputSeverity;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.sql.exec.SQLScriptProcessor;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticUtils;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public abstract class ExecuteSQLFunction implements AIFunction {
    private static final Log log = Log.getLog(ExecuteSQLFunction.class);

    public static final String SQL_PARAM = "sqlQuery";

    @NotNull
    @Override
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull Map<String, Object> parameters
    ) throws DBException {
        AIDatabaseContext aiDatabaseContext = context.getContext();
        if (aiDatabaseContext == null) {
            return new AIFunctionResult(
                AIFunctionType.INFORMATION,
                ""
            );
        }

        DBPDataSource dataSource = aiDatabaseContext.getExecutionContext().getDataSource();
        String catalog = CommonUtils.toString(parameters.get(AIFunctionParams.CATALOG_PARAM), null);
        String sqlQuery = CommonUtils.toString(parameters.get(SQL_PARAM));
        DataTransferProcessorDescriptor processor = getResultProcessor(parameters);
        int maxRows = processor == null ? 0 : getMaxRows(parameters);
        if (processor != null && maxRows <= 0) {
            throw new DBException("Maximum row count must be greater than zero");
        }

        if (CommonUtils.isEmptyTrimmed(sqlQuery)) {
            throw new DBException("Parameter '" + SQL_PARAM + "' not specified");
        }

        DBSInstance dbsInstance = resolveInstance(dataSource, catalog);

        DBCExecutionContext executionContext = DBUtils.getOrOpenDefaultContext(dbsInstance, false);

        if (!AIUtils.isObjectInScope(aiDatabaseContext, executionContext, dbsInstance)) {
            return new AIFunctionResult(
                AIFunctionType.INFORMATION,
                "Database instance '" + dbsInstance.getName() + "' is not in scope. Access restricted."
            );
        }

        List<SQLQuery> scriptElements = SQLScriptParser
            .parseScript(dataSource, sqlQuery)
            .stream()
            .filter(it -> it instanceof SQLQuery)
            .map(SQLQuery.class::cast)
            .toList();
        if (processor != null && scriptElements.size() > 1 && !processor.isAppendable()) {
            throw new DBException("Format '" + processor.getAIFormatId() + "' supports only one SQL statement per call");
        }
        if (scriptElements.isEmpty()) {
            throw new DBException("No SQL statements to execute");
        }

        SQLScriptContext scriptContext = new SQLScriptContext(
            null,
            () -> executionContext,
            null,
            new LogOutputWriter(log),
            null
        );

        for (SQLQuery query : scriptElements) {
            AIFunctionResult invalidObjectsMessage = verifyObjectsInScope(query, executionContext, aiDatabaseContext);
            if (invalidObjectsMessage != null) {
                return invalidObjectsMessage;
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DBCStatistics statistics = new DBCStatistics();
        boolean modifiedRowCountKnown = true;
        for (SQLQuery query : scriptElements) {
            StreamTransferConsumer consumer = null;
            if (processor != null) {
                query.setResultSetLimit(0, maxRows);
                consumer = new StreamTransferConsumer();
                SQLQueryDataContainer sqlQueryDataContainer = new SQLQueryDataContainer(
                    new DataSourceContextProvider(dataSource), query, scriptContext, log
                );
                IStreamDataExporter streamDataExporter = AIDataExportUtils.createExporter(processor);
                consumer.initTransfer(
                    sqlQueryDataContainer,
                    prepareSettings(),
                    new IDataTransferConsumer.TransferParameters(false, false, outputStream),
                    streamDataExporter,
                    AIDataExportUtils.getProperties(processor),
                    dataSource.getContainer().getProject()
                );
            }

            SQLScriptProcessor scriptProcessor = new SQLScriptProcessor(
                executionContext,
                List.of(query),
                scriptContext,
                consumer,
                log
            );
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
        return new AIFunctionResult(
            getResultType(),
            getResultValue(outputStream.toString(StandardCharsets.UTF_8), statistics)
        );
    }

    @NotNull
    protected abstract SQLQueryCategory getQueryCategory();

    @NotNull
    protected final AIFunctionType getResultType() {
        return AIFunctionType.INFORMATION;
    }

    @NotNull
    protected Object getResultValue(@NotNull String output, @NotNull DBCStatistics statistics) {
        return "Executed " + statistics.getStatementsCount() + " statement(s)";
    }

    @NotNull
    protected abstract SQLScopeHeuristics getScopeHeuristics();

    protected int getMaxRows(@NotNull Map<String, Object> parameters) {
        return 0;
    }

    @Nullable
    protected DataTransferProcessorDescriptor getResultProcessor(@NotNull Map<String, Object> parameters) throws DBException {
        return null;
    }

    final void validateQuery(@NotNull SQLQuery query) throws DBException {
        SQLQueryCategory category = SQLScopeHeuristics.getQueryCategory(query.getText());
        SQLQueryCategory parsedCategory = SQLQueryCategory.UNKNOWN;
        if (query.getStatement() != null) {
            parsedCategory = query.getType().getCategory();
        }
        if (query.getStatement() instanceof Select && query.isMutatingStatement()) {
            category = query.getStatement() instanceof PlainSelect select
                && (!CommonUtils.isEmpty(select.getIntoTables()) || select.getIntoTempTable() != null)
                ? SQLQueryCategory.DDL : SQLQueryCategory.DML;
            parsedCategory = category;
        }
        if (category != getQueryCategory()
            || parsedCategory != SQLQueryCategory.UNKNOWN && parsedCategory != category
            || category == SQLQueryCategory.DQL && (query.isModifying() || SQLScopeHeuristics.hasWriteKeywords(query.getText()))) {
            throw new DBException("This function only accepts " + getQueryCategory() + " statements");
        }
    }

    @Nullable
    final AIFunctionResult verifyObjectsInScope(
        @NotNull SQLQuery query,
        @NotNull DBCExecutionContext executionContext,
        @NotNull AIDatabaseContext aiDatabaseContext
    ) throws DBException {
        validateQuery(query);
        QueryTablesResolutionResult resolutionResult = this.resolveQueryTables(executionContext, query, aiDatabaseContext.getScope());
        List<String> messages = new LinkedList<>();
        if (resolutionResult.datasourceWideRequired && aiDatabaseContext.getScope() != AIDatabaseScope.CURRENT_DATASOURCE) {
            messages.add("This statement requires datasource-wide scope");
        }
        for (String tableName : resolutionResult.unresolvedObjects) {
            messages.add(tableName + " is not found");
        }
        for (DBSObject object : resolutionResult.resolvedTables) {
            if (object instanceof DBPDataSource && aiDatabaseContext.getScope() == AIDatabaseScope.CUSTOM
                || !AIUtils.isObjectInScope(aiDatabaseContext, executionContext, object)) {
                messages.add(DBUtils.getFullQualifiedName(executionContext.getDataSource(), object) + " is out of allowed scope");
            }
        }
        if (!messages.isEmpty()) {
            return new AIFunctionResult(
                AIFunctionType.INFORMATION,
                "SQL query operates with restricted database objects: " + String.join(", ", messages)
            );
        }
        return null;
    }

    record QueryTablesResolutionResult(
        @NotNull Set<DBSObject> resolvedTables,
        @NotNull Set<String> unresolvedObjects,
        boolean datasourceWideRequired
    ) {
    }

    @NotNull
    private QueryTablesResolutionResult resolveQueryTables(
        @NotNull DBCExecutionContext executionContext,
        @NotNull SQLQuery query,
        @NotNull AIDatabaseScope scope
    ) throws DBException {
        SQLIdentifierDetector identifierDetector = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect());

        Set<DBSObject> resolvedObjects = new HashSet<>();
        Set<String> unresolvedObjects = new HashSet<>();

        if (!(query.getStatement() instanceof Select) || query.isMutatingStatement()) {
            if (!(executionContext.getDataSource() instanceof DBSObjectContainer root)) {
                throw new DBException("Cannot resolve database objects in an unparsed SQL statement");
            }
            Map<String, SQLScopeHeuristics.ObjectReference> references = new LinkedHashMap<>();
            for (SQLScopeHeuristics.ObjectReference reference : getScopeHeuristics().extractReferences(query.getText())) {
                references.put(reference.name(), reference);
            }
            if (query.getStatement() != null) {
                try {
                    for (String name : new TablesNamesFinder<Void>().getTables(query.getStatement())) {
                        references.putIfAbsent(name, new SQLScopeHeuristics.ObjectReference(name, "OBJECT", false));
                    }
                } catch (UnsupportedOperationException | ClassCastException e) {
                    // TablesNamesFinder does not support every statement, including data-modifying CTEs.
                    log.debug("Using heuristic object resolution: " + e.getMessage());
                }
            }
            boolean global = references.isEmpty()
                && (getQueryCategory() == SQLQueryCategory.DCL || getQueryCategory() == SQLQueryCategory.DDL);
            if (references.isEmpty() && !global) {
                throw new DBException("Cannot determine database objects in an unparsed SQL statement");
            }
            for (SQLScopeHeuristics.ObjectReference reference : references.values()) {
                String[] parts = identifierDetector.splitIdentifier(reference.name());
                if (reference.type().equals("COLUMN") && parts.length > 1) {
                    parts = Arrays.copyOf(parts, parts.length - 1);
                }
                if (Set.of("USER", "ROLE", "TABLESPACE", "SYSTEM", "EXTENSION").contains(reference.type())) {
                    global = true;
                    continue;
                }
                if (reference.creation() && Set.of("SCHEMA", "DATABASE", "CATALOG").contains(reference.type())) {
                    global |= !reference.type().equals("SCHEMA") || scope == AIDatabaseScope.CURRENT_SCHEMA;
                }
                DBSObject object = Set.of("SCHEMA", "DATABASE", "CATALOG").contains(reference.type())
                    ? resolveNamespace(executionContext, root, parts, reference.type(), reference.creation())
                    : AIUtils.findObjectByFQN(new VoidProgressMonitor(), executionContext, root, parts);
                if (object == null && Set.of("PROCEDURE", "FUNCTION").contains(reference.type()) && !reference.creation()) {
                    DBSObject namespace = resolveNamespace(executionContext, root, parts, reference.type(), false);
                    DBSProcedureContainer procedures = DBUtils.getAdapter(DBSProcedureContainer.class, namespace);
                    if (procedures != null) {
                        object = procedures.getProcedure(new VoidProgressMonitor(), DBUtils.getUnQuotedNormalizedIdentifier(
                            executionContext.getDataSource().getSQLDialect(), parts[parts.length - 1]));
                    }
                }
                if (object == null && (getQueryCategory() == SQLQueryCategory.DDL || getQueryCategory() == SQLQueryCategory.DCL)) {
                    object = resolveNamespace(executionContext, root, parts, reference.type(), reference.creation());
                }
                if (object == null) {
                    unresolvedObjects.add(reference.name());
                } else {
                    resolvedObjects.add(object);
                }
            }
            return new QueryTablesResolutionResult(resolvedObjects, unresolvedObjects, global);
        }

        traverseQueryTables(query.getStatement(), tableNode -> {
            DBSObject object = SQLSemanticUtils.findObjectForTableNode(
                new VoidProgressMonitor(),
                executionContext,
                executionContext.getDataSource(),
                identifierDetector,
                tableNode
            );
            if (object == null) {
                unresolvedObjects.add(tableNode.getFullyQualifiedName().toString());
            } else {
                resolvedObjects.add(object);
            }
        });

        return new QueryTablesResolutionResult(resolvedObjects, unresolvedObjects, false);
    }

    @Nullable
    private static DBSObject resolveNamespace(
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer root,
        @NotNull String[] parts,
        @NotNull String type,
        boolean creation
    ) throws DBException {
        parts = Arrays.stream(parts)
            .map(part -> DBUtils.getUnQuotedNormalizedIdentifier(executionContext.getDataSource().getSQLDialect(), part))
            .toArray(String[]::new);
        DBCExecutionContextDefaults<?, ?> defaults = executionContext.getContextDefaults();
        Class<? extends DBSObject> childType = root.getPrimaryChildType(new VoidProgressMonitor());
        boolean schemas = DBSSchema.class.isAssignableFrom(childType)
            || defaults != null && (defaults.getDefaultSchema() != null || defaults.supportsSchemaChange());
        if (type.equals("SCHEMA")) {
            if (!creation) {
                return DBUtils.getObjectByPath(new VoidProgressMonitor(), executionContext, root,
                    parts.length > 1 ? parts[parts.length - 2] : null, parts[parts.length - 1], null, true);
            }
            return parts.length > 1 ? root.getChild(new VoidProgressMonitor(), parts[0])
                : defaults != null && defaults.getDefaultCatalog() != null ? defaults.getDefaultCatalog() : root;
        }
        if (type.equals("DATABASE") || type.equals("CATALOG")) {
            return creation ? root : root.getChild(new VoidProgressMonitor(), parts[parts.length - 1]);
        }
        if (parts.length == 1) {
            if (defaults != null && defaults.getDefaultSchema() != null) {
                return defaults.getDefaultSchema();
            }
            if (defaults != null && defaults.getDefaultCatalog() != null) {
                return defaults.getDefaultCatalog();
            }
            // A schema/catalog cannot be guessed when the datasource exposes more than one namespace.
            return DBSCatalog.class.isAssignableFrom(childType) || DBSSchema.class.isAssignableFrom(childType) ? null : root;
        }
        String catalog = parts.length > 2 ? parts[parts.length - 3] : schemas ? null : parts[0];
        String schema = schemas || parts.length > 2 ? parts[parts.length - 2] : null;
        return DBUtils.getObjectByPath(new VoidProgressMonitor(), executionContext, root, catalog, schema, null, true);
    }

    @NotNull
    static DBSInstance resolveInstance(@NotNull DBPDataSource dataSource, @Nullable String catalog) throws DBException {
        if (CommonUtils.isEmpty(catalog)) {
            return dataSource.getDefaultInstance();
        }
        return dataSource.getAvailableInstances().stream()
            .filter(instance -> instance.getName().equals(catalog))
            .findFirst()
            .orElseThrow(() -> new DBException("Catalog '" + catalog + "' not found"));
    }

    private record TableScope(@NotNull Node node, @NotNull Set<String> aliases) {
    }

    static void traverseQueryTables(@NotNull Statement statement, @NotNull Consumer<Table> onTable) {
        Deque<TableScope> stack = new ArrayDeque<>();
        stack.push(new TableScope(((ASTNodeAccess) statement).getASTNode(), Set.of()));
        Set<Select> handledSelects = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Table> handledTables = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Function> handledFunctions = Collections.newSetFromMap(new IdentityHashMap<>());
        while (!stack.isEmpty()) {
            TableScope scope = stack.pop();
            Node node = scope.node();
            Set<String> aliases = scope.aliases();
            Object value = node instanceof SimpleNode simpleNode ? simpleNode.jjtGetValue() : null;
            if (value instanceof Select select && handledSelects.add(select)) {
                Set<String> visibleAliases = new HashSet<>(aliases);
                for (WithItem<?> item : CommonUtils.safeCollection(select.getWithItemsList())) {
                    Set<String> definitionAliases = new HashSet<>(visibleAliases);
                    if (item.isRecursive()) {
                        definitionAliases.add(identifierKey(item.getAliasName()));
                    }
                    // CTE definitions have separate AST roots. Check their physical tables as well as the main query.
                    if (item.getParenthesedStatement() instanceof ASTNodeAccess definition) {
                        stack.push(new TableScope(definition.getASTNode(), definitionAliases));
                    }
                    visibleAliases.add(identifierKey(item.getAliasName()));
                }
                aliases = visibleAliases;
            }
            if (value instanceof Table table && handledTables.add(table)) {
                boolean qualified = !CommonUtils.isEmpty(table.getSchemaName())
                    || table.getDatabase() != null && !CommonUtils.isEmpty(table.getDatabase().getDatabaseName());
                if (qualified || !aliases.contains(identifierKey(table.getName()))) {
                    onTable.accept(table);
                }
            }
            if (value instanceof Function function && handledFunctions.add(function) && function.getMultipartName().size() > 1) {
                // Qualified routines can read data just like tables and must be checked against the same scope.
                onTable.accept(new Table(function.getMultipartName()));
            }
            for (int i = node.jjtGetNumChildren() - 1; i >= 0; i--) {
                stack.push(new TableScope(node.jjtGetChild(i), aliases));
            }
        }
    }

    @NotNull
    private static String identifierKey(@NotNull String name) {
        return name.startsWith("\"") || name.startsWith("`") || name.startsWith("[") ? name : name.toUpperCase(Locale.ROOT);
    }

    @NotNull
    private StreamConsumerSettings prepareSettings() {
        StreamConsumerSettings settings = new StreamConsumerSettings();
        settings.setOutputClipboard(false);
        settings.setOutputEncodingBOM(false);
        settings.setOutputEncoding(StandardCharsets.UTF_8.name());
        return settings;
    }

    private static class LogOutputWriter implements DBCOutputWriter {
        private final Log log;

        private LogOutputWriter(@NotNull Log log) {
            this.log = log;
        }

        @Override
        public void println(@Nullable DBCOutputSeverity severity, @Nullable String message) {
            if (message != null) {
                log.debug("MCP sql execution log: " + message);
            }
        }

        @Override
        public void flush() {

        }
    }
}
