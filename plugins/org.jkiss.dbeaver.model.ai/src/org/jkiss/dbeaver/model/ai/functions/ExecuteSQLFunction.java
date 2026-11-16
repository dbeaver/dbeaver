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

import net.sf.jsqlparser.parser.ASTNodeAccess;
import net.sf.jsqlparser.parser.Node;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.utils.AIDataExportUtils;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.output.DBCOutputSeverity;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.sql.exec.SQLScriptProcessor;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticUtils;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
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

public class ExecuteSQLFunction implements AIFunction {
    private static final Log log = Log.getLog(ExecuteSQLFunction.class);

    public static final String SQL_PARAM = "sqlQuery";
    public static final String MAX_ROWS_PARAM = "maxRows";
    public static final String FORMAT_PARAM = "format";

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
        int maxRows = CommonUtils.toInt(parameters.get(MAX_ROWS_PARAM), 10);
        if (maxRows <= 0) {
            throw new DBException("Maximum row count must be greater than zero");
        }

        if (CommonUtils.isEmptyTrimmed(sqlQuery)) {
            throw new DBException("Parameter '" + SQL_PARAM + "' not specified");
        }
        DataTransferProcessorDescriptor processor = AIDataExportUtils.getProcessor(
            CommonUtils.toString(parameters.get(FORMAT_PARAM), "csv"));

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
        if (scriptElements.size() > 1 && !processor.isAppendable()) {
            throw new DBException("Format '" + processor.getAIFormatId() + "' supports only one SQL statement per call");
        }

        SQLScriptContext scriptContext = new SQLScriptContext(
            null,
            () -> executionContext,
            null,
            new LogOutputWriter(log),
            null
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataSourceContextProvider dataSourceContextProvider = new DataSourceContextProvider(dataSource);
        for (SQLQuery query : scriptElements) {
            AIFunctionResult invalidObjectsMessage = verifyObjectsInScope(query, executionContext, aiDatabaseContext);
            if (invalidObjectsMessage != null) {
                return invalidObjectsMessage;
            }

            query.setResultSetLimit(0, maxRows);
            StreamTransferConsumer consumer = new StreamTransferConsumer();
            SQLQueryDataContainer sqlQueryDataContainer = new SQLQueryDataContainer(
                dataSourceContextProvider, query, scriptContext, log
            );
            IStreamDataExporter streamDataExporter = AIDataExportUtils.createExporter(processor);
            consumer.initTransfer(
                sqlQueryDataContainer,
                prepareSettings(),
                new IDataTransferConsumer.TransferParameters(
                    false,
                    false,
                    outputStream
                ),
                streamDataExporter,
                AIDataExportUtils.getProperties(processor),
                dataSource.getContainer().getProject()
            );

            SQLScriptProcessor scriptProcessor = new SQLScriptProcessor(
                executionContext,
                List.of(query),
                scriptContext,
                consumer,
                log
            );
            scriptProcessor.setMaxRows(maxRows);

            scriptProcessor.runScript(context.getMonitor());
            consumer.finishTransfer(context.getMonitor(), false);
        }

        return new AIFunctionResult(
            AIFunctionType.INFORMATION,
            outputStream.toString(StandardCharsets.UTF_8)
        );
    }

    @Nullable
    AIFunctionResult verifyObjectsInScope(
        @NotNull SQLQuery query,
        @NotNull DBCExecutionContext executionContext,
        @NotNull AIDatabaseContext aiDatabaseContext
    ) throws DBException {
        QueryTablesResolutionResult resolutionResult = this.resolveQueryTables(executionContext, query);
        List<String> messages = new LinkedList<>();
        for (String tableName : resolutionResult.unresolvedObjects) {
            messages.add(tableName + " is not found");
        }
        for (DBSObject object : resolutionResult.resolvedTables) {
            if (!AIUtils.isObjectInScope(aiDatabaseContext, executionContext, object)) {
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
        Set<DBSObject> resolvedTables,
        Set<String> unresolvedObjects
    ) {
    }

    @NotNull
    private QueryTablesResolutionResult resolveQueryTables(
        @NotNull DBCExecutionContext executionContext,
        @NotNull SQLQuery query
    ) throws DBException {
        SQLIdentifierDetector identifierDetector = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect());

        Set<DBSObject> resolvedObjects = new HashSet<>();
        Set<String> unresolvedObjects = new HashSet<>();

        if (query.getStatement() == null) {
            if (!(executionContext.getDataSource() instanceof DBSObjectContainer root)) {
                throw new DBException("Cannot resolve database objects in an unparsed SQL statement");
            }
            Set<String> tableNames = SQLScopeHeuristics.extractTableNames(query.getText());
            if (tableNames.isEmpty()) {
                throw new DBException("Cannot determine database objects in an unparsed SQL statement");
            }
            for (String tableName : tableNames) {
                DBSObject object = AIUtils.findObjectByFQN(
                    new VoidProgressMonitor(), executionContext, root, identifierDetector.splitIdentifier(tableName));
                if (object == null) {
                    unresolvedObjects.add(tableName);
                } else {
                    resolvedObjects.add(object);
                }
            }
            return new QueryTablesResolutionResult(resolvedObjects, unresolvedObjects);
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

        return new QueryTablesResolutionResult(resolvedObjects, unresolvedObjects);
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
