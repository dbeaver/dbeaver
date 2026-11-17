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
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.TableStatement;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPKeywordType;
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
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.sql.exec.SQLScriptProcessor;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSEntityElement;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.utils.ArrayUtils;
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
        if (query.getStatement() == null || query.getParseError() != null || query.getStatement() instanceof UnsupportedStatement) {
            if (executionContext.getDataSource().getSQLDialect().getExecutableCommentPrefixes().stream()
                .anyMatch(prefix -> query.getText().contains(prefix))) {
                throw new DBException("Cannot verify database object scope in an unparsed SQL statement containing executable comments");
            }
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

    protected final void verifyQueries(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<SQLQuery> queries,
        @Nullable DataTransferProcessorDescriptor processor
    ) throws DBException {
        if (queries.isEmpty()) {
            throw new DBException("No SQL statements to execute");
        }
        if (processor != null && queries.size() > 1 && !processor.isAppendable()) {
            throw new DBException("Format '" + processor.getAIFormatId() + "' supports only one statement per call");
        }
        for (SQLQuery query : queries) {
            verifyQuery(context, executionContext, query);
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
        verifyQueries(context, executionContext, queries, processor);
        SQLScriptContext scriptContext = new SQLScriptContext(null, () -> executionContext, null, new LogOutputWriter(), null);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DBCStatistics statistics = new DBCStatistics();
        boolean modifiedRowCountKnown = true;
        for (SQLQuery query : queries) {
            StreamTransferConsumer consumer = null;
            if (processor != null) {
                var exporter = AIDataExportUtils.createExporter(processor);
                AIDataExportUtils.writeResultSeparator(output, exporter);
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
                    exporter,
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
        DBSObject owner = object instanceof DBSEntityElement element ? element.getParentObject()
            : object instanceof DBSTrigger trigger ? trigger.getTable() : null;
        boolean allowed = AIUtils.isObjectInScope(databaseContext, databaseContext.getExecutionContext(), object)
            || owner != null && databaseContext.isObjectAllowed(object)
                && AIUtils.isObjectInScope(databaseContext, databaseContext.getExecutionContext(), owner);
        if (!allowed) {
            throw new DBException("Database object '" + object.getName() + "' is outside the allowed scope");
        }
    }

    protected final void verifyDatasourceScope(@NotNull AIFunctionContext context) throws DBException {
        AIDatabaseContext databaseContext = requireDatabaseContext(context);
        if (databaseContext.getScope() != AIDatabaseScope.CURRENT_DATASOURCE || databaseContext.hasDataSourceCustomScope()) {
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
        DBSObjectContainer root = DBUtils.getAdapter(DBSObjectContainer.class, executionContext.getDataSource());
        DBCExecutionContextDefaults<?, ?> defaults = executionContext.getContextDefaults();
        boolean schemas = root != null && DBSSchema.class.isAssignableFrom(root.getPrimaryChildType(context.getMonitor()))
            || defaults != null && (defaults.getDefaultSchema() != null || defaults.supportsSchemaChange());
        if (type.equals("SCHEMA") && !schemas) {
            type = "DATABASE";
        }
        if (!Set.of("OBJECT", "OTHER", "TABLE", "VIEW", "MATERIALIZED VIEW", "INDEX", "SEQUENCE", "FUNCTION", "PROCEDURE",
            "ROUTINE", "TRIGGER", "TYPE", "DOMAIN", "SCHEMA", "DATABASE", "CATALOG", "SYNONYM", "COLUMN", "CONSTRAINT",
            "COLLATION", "CHARACTER SET", "CHARACTER_SET", "TRANSLATION", "TRANSFORM", "ASSERTION").contains(type)
            || creation && Set.of("DATABASE", "CATALOG").contains(type)) {
            verifyDatasourceScope(context);
            return;
        }
        if (creation && type.equals("SCHEMA") && requireDatabaseContext(context).getScope() == AIDatabaseScope.CURRENT_SCHEMA) {
            throw new DBException("Creating a schema is outside the current-schema scope");
        }
        String[] parts = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect()).splitIdentifier(name);
        if (parts.length == 0 || parts.length > (type.equals("COLUMN") ? 4 : 3)) {
            throw new DBException("Cannot verify this qualified object name");
        }
        if (type.equals("COLUMN")) {
            if (parts.length < 2) {
                throw new DBException("Cannot determine the column's owning table");
            }
            parts = Arrays.copyOf(parts, parts.length - 1);
            name = String.join(".", parts);
            type = "TABLE";
        }
        DBSObject namespace = resolveNamespace(context, executionContext, parts, type, creation);
        if (namespace == null) {
            throw new DBException("Cannot determine an allowed namespace for '" + name + "'");
        }
        if (!creation && Set.of("FUNCTION", "PROCEDURE", "ROUTINE").contains(type)) {
            Set<DBSObject> routines = findRoutines(context, executionContext, name);
            if (!routines.isEmpty()) {
                for (DBSObject routine : routines) {
                    checkObjectScope(context, routine);
                }
                return;
            }
        }
        DBSObject object = !creation && !Set.of("SCHEMA", "DATABASE", "CATALOG").contains(type)
            ? findDefinitionObject(context, executionContext, name, type, namespace) : null;
        if (object == null) {
            if (!coversNamespace(context, namespace)) {
                throw new DBException("The whole namespace of '" + name + "' is not in the allowed scope");
            }
            object = namespace;
        }
        checkObjectScope(context, object);
    }

    private boolean coversNamespace(@NotNull AIFunctionContext context, @NotNull DBSObject namespace) throws DBException {
        AIDatabaseContext databaseContext = requireDatabaseContext(context);
        if (databaseContext.hasDataSourceCustomScope()) {
            String namespaceId = DBUtils.getObjectFullId(namespace);
            if (databaseContext.getDataSourceCustomObjectIds().stream()
                .noneMatch(id -> namespaceId.equals(id) || namespaceId.startsWith(id + "/"))) {
                return false;
            }
        }
        if (databaseContext.getScope() == AIDatabaseScope.CURRENT_DATASOURCE) {
            return true;
        }
        if (databaseContext.getScope() == AIDatabaseScope.CUSTOM) {
            // Derived schema/catalog sets also contain ancestors of selected tables, not full namespace grants.
            return CommonUtils.safeCollection(databaseContext.getCustomEntities()).stream()
                .anyMatch(allowed -> isWithin(namespace, allowed));
        }
        DBCExecutionContext original = databaseContext.getExecutionContext();
        DBCExecutionContextDefaults<?, ?> defaults = original.getContextDefaults();
        if (defaults != null) {
            DBSObject authority = databaseContext.getScope() == AIDatabaseScope.CURRENT_SCHEMA
                ? defaults.getDefaultSchema() : defaults.getDefaultCatalog();
            if (authority != null) {
                return isWithin(namespace, authority);
            }
            DBSCatalog catalog = defaults.getDefaultCatalog();
            if (catalog != null && !DBSSchema.class.isAssignableFrom(catalog.getPrimaryChildType(context.getMonitor()))) {
                return isWithin(namespace, catalog);
            }
        }
        DBSObjectContainer root = DBUtils.getAdapter(DBSObjectContainer.class, original.getDataSource());
        if (root == null) {
            return false;
        }
        Class<? extends DBSObject> children = root.getPrimaryChildType(context.getMonitor());
        return !DBSCatalog.class.isAssignableFrom(children)
            && (databaseContext.getScope() != AIDatabaseScope.CURRENT_SCHEMA || !DBSSchema.class.isAssignableFrom(children))
            && isWithin(namespace, root);
    }

    private static boolean isWithin(@NotNull DBSObject object, @NotNull DBSObject parent) {
        for (DBSObject current = object; current != null; current = current.getParentObject()) {
            if (current == parent) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private DBSObject findDefinitionObject(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull String name,
        @NotNull String type,
        @NotNull DBSObject namespace
    ) throws DBException {
        SQLIdentifierDetector identifiers = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect());
        String[] parts = identifiers.splitIdentifier(name);
        String objectName = DBUtils.getUnQuotedNormalizedIdentifier(executionContext.getDataSource().getSQLDialect(), parts[parts.length - 1]);
        boolean exactName = identifiers.isQuoted(parts[parts.length - 1]) || !coversNamespace(context, namespace);
        Class<? extends DBSObject> objectClass = switch (type.replace(' ', '_')) {
            case "TABLE", "VIEW", "MATERIALIZED_VIEW", "OBJECT" -> DBSEntity.class;
            case "CONSTRAINT" -> DBSEntityConstraint.class;
            case "SEQUENCE" -> DBSSequence.class;
            case "FUNCTION", "PROCEDURE", "ROUTINE" -> DBSProcedure.class;
            case "INDEX" -> DBSTableIndex.class;
            case "TRIGGER" -> DBSTrigger.class;
            case "TYPE", "DOMAIN" -> DBSDataType.class;
            case "OTHER" -> DBSObject.class;
            default -> null;
        };
        if (namespace instanceof DBSObjectContainer container) {
            DBSObject child = container.getChild(context.getMonitor(), objectName);
            if (child != null && objectClass != null && objectClass.isInstance(child)) {
                return child;
            }
        }
        if (namespace instanceof DBSEntityContainer container) {
            for (var mapping : ArrayUtils.safeArray(container.getNestedEntityTypes())) {
                if (objectClass != null && objectClass.isAssignableFrom(mapping.getEntityClass())
                    || mapping.getEntityType().getId().equalsIgnoreCase(type)) {
                    DBSObject child = DBUtils.findObject(container.getEntities(context.getMonitor(), mapping.getEntityType()),
                        objectName, !exactName);
                    if (child != null) {
                        return child;
                    }
                }
            }
        }
        DBSStructureAssistant<DBCExecutionContext> assistant = DBUtils.getAdapter(DBSStructureAssistant.class, namespace);
        if (assistant == null) {
            assistant = DBUtils.getAdapter(DBSStructureAssistant.class, executionContext.getDataSource());
        }
        if (assistant != null) {
            DBSObjectType[] types = Arrays.stream(assistant.getSupportedObjectTypes())
                .filter(candidate -> objectClass != null ? objectClass.isAssignableFrom(candidate.getTypeClass())
                    : candidate.getTypeName().equalsIgnoreCase(type)).toArray(DBSObjectType[]::new);
            if (types.length != 0) {
                DBSStructureAssistant.ObjectsSearchParams search = new DBSStructureAssistant.ObjectsSearchParams(types, objectName);
                search.setParentObject(namespace);
                search.setGlobalSearch(false);
                search.setLikeCondition(false);
                search.setCaseSensitive(exactName);
                search.setMaxResults(100);
                for (var reference : assistant.findObjectsByMask(context.getMonitor(), executionContext, search)) {
                    boolean matches = search.isCaseSensitive() ? objectName.equals(reference.getName())
                        : objectName.equalsIgnoreCase(reference.getName());
                    if (matches) {
                        DBSObject child = reference.resolveObject(context.getMonitor());
                        if (isWithin(child, namespace) && (objectClass == null || objectClass.isInstance(child))) {
                            return child;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private DBSObject findObject(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String name
    ) throws DBException {
        String[] parts = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect()).splitIdentifier(name);
        if (parts.length == 0 || parts.length > 3) {
            throw new DBException("Cannot verify this qualified object name");
        }
        DBSObject namespace = resolveNamespace(context, executionContext, parts, "OBJECT", false);
        return namespace instanceof DBSObjectContainer container ? container.getChild(context.getMonitor(),
            DBUtils.getUnQuotedNormalizedIdentifier(executionContext.getDataSource().getSQLDialect(), parts[parts.length - 1])) : null;
    }

    protected final void verifyRoutine(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String name
    ) throws DBException {
        Set<DBSObject> routines = findRoutines(context, executionContext, name);
        if (routines.isEmpty()) {
            throw new DBException("Database routine '" + name + "' not found");
        }
        for (DBSObject routine : routines) {
            checkObjectScope(context, routine);
        }
    }

    @NotNull
    private Set<DBSObject> findRoutines(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String name
    ) throws DBException {
        String[] parts = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect()).splitIdentifier(name);
        if (parts.length == 0 || parts.length > 3) {
            throw new DBException("Cannot verify this qualified routine name");
        }
        DBSObject namespace = resolveNamespace(context, executionContext, parts, "FUNCTION", false);
        DBSProcedureContainer procedures = DBUtils.getAdapter(DBSProcedureContainer.class, namespace);
        String routineName = DBUtils.getUnQuotedNormalizedIdentifier(executionContext.getDataSource().getSQLDialect(), parts[parts.length - 1]);
        Set<DBSObject> routines = new LinkedHashSet<>();
        DBSObject procedure = procedures == null ? null : procedures.getProcedure(context.getMonitor(), routineName);
        if (procedure != null && namespace != null && isWithin(procedure, namespace)) {
            routines.add(procedure);
        }
        if (procedures != null) {
            // Overloaded routines may use a signature rather than the display name as their unique cache key.
            SQLIdentifierDetector identifiers = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect());
            boolean exactName = identifiers.isQuoted(parts[parts.length - 1]) || namespace == null || !coversNamespace(context, namespace);
            for (DBSProcedure candidate : CommonUtils.safeCollection(procedures.getProcedures(context.getMonitor()))) {
                boolean matches = exactName ? candidate.getName().equals(routineName)
                    : candidate.getName().equalsIgnoreCase(routineName);
                if (matches && namespace != null && isWithin(candidate, namespace)) {
                    routines.add(candidate);
                }
            }
        }
        return routines;
    }

    private void verifyFunctionReference(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String name
    ) throws DBException {
        SQLIdentifierDetector identifiers = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect());
        if (identifiers.splitIdentifier(name).length > 1) {
            verifyRoutine(context, executionContext, name);
        } else {
            Set<DBSObject> routines = findRoutines(context, executionContext, name);
            if (!routines.isEmpty()) {
                for (DBSObject routine : routines) {
                    checkObjectScope(context, routine);
                }
            } else if (!identifiers.isQuoted(name) && executionContext.getDataSource().getSQLDialect().getFunctions().stream()
                .anyMatch(builtin -> builtin.equalsIgnoreCase(name))) {
                return;
            } else {
                verifyDatasourceScope(context);
            }
        }
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
        if (root == null || parts.length == 0 || parts.length > 3) {
            return null;
        }
        parts = Arrays.stream(parts).map(part -> DBUtils.getUnQuotedNormalizedIdentifier(
            executionContext.getDataSource().getSQLDialect(), part)).toArray(String[]::new);
        DBCExecutionContextDefaults<?, ?> defaults = executionContext.getContextDefaults();
        Class<? extends DBSObject> childType = root.getPrimaryChildType(context.getMonitor());
        if (type.equals("SCHEMA")) {
            if (!creation) {
                if (parts.length > 1 && !DBSCatalog.class.isAssignableFrom(childType)
                    && (defaults == null || defaults.getDefaultCatalog() == null
                        || !defaults.getDefaultCatalog().getName().equals(parts[parts.length - 2]))) {
                    return null;
                }
                DBSObject object = DBUtils.getObjectByPath(context.getMonitor(), executionContext, root,
                    parts.length > 1 ? parts[parts.length - 2] : null, parts[parts.length - 1], null, true);
                return object instanceof DBSSchema ? object : null;
            }
            if (parts.length > 1) {
                DBSObject catalog = root.getChild(context.getMonitor(), parts[0]);
                return catalog instanceof DBSCatalog ? catalog
                    : defaults != null && defaults.getDefaultCatalog() != null
                        && defaults.getDefaultCatalog().getName().equals(parts[0]) ? defaults.getDefaultCatalog() : null;
            }
            return defaults != null && defaults.getDefaultCatalog() != null ? defaults.getDefaultCatalog() : root;
        }
        if (type.equals("DATABASE") || type.equals("CATALOG")) {
            if (creation) {
                return root;
            }
            DBSObject catalog = root.getChild(context.getMonitor(), parts[parts.length - 1]);
            if (catalog instanceof DBSCatalog) {
                return catalog;
            }
            return defaults != null && defaults.getDefaultCatalog() != null
                && defaults.getDefaultCatalog().getName().equals(parts[parts.length - 1]) ? defaults.getDefaultCatalog() : null;
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
        String catalogName = parts.length > 2 ? parts[parts.length - 3] : schemas ? null : parts[0];
        String schemaName = schemas || parts.length > 2 ? parts[parts.length - 2] : null;
        if (catalogName != null && !DBSCatalog.class.isAssignableFrom(childType)) {
            if (defaults == null || defaults.getDefaultCatalog() == null || !defaults.getDefaultCatalog().getName().equals(catalogName)) {
                return null;
            }
            if (schemaName == null) {
                return defaults.getDefaultCatalog();
            }
        }
        DBSObject namespace = DBUtils.getObjectByPath(context.getMonitor(), executionContext, root, catalogName, schemaName, null, true);
        return schemaName != null ? namespace instanceof DBSSchema ? namespace : null
            : namespace instanceof DBSCatalog ? namespace : null;
    }

    protected final void verifyStatementObjects(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull Statement statement
    ) throws DBException {
        verifyStatementObjects(context, executionContext, statement, Set.of());
    }

    private void verifyStatementObjects(
        @NotNull AIFunctionContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull Statement statement,
        @NotNull Set<String> inheritedCtes
    ) throws DBException {
        Set<String> names = new LinkedHashSet<>();
        Set<String> routines = new LinkedHashSet<>();
        try {
            if (statement instanceof Select select && select.getASTNode() != null) {
                collectSelectObjects(select, names, routines, inheritedCtes);
            } else {
                Set<String> ctes = new HashSet<>(inheritedCtes);
                List<WithItem<?>> definitions = statement instanceof Insert insert ? insert.getWithItemsList()
                    : statement instanceof Update update ? update.getWithItemsList()
                    : statement instanceof Delete delete ? delete.getWithItemsList()
                    : statement instanceof Merge merge ? merge.getWithItemsList() : List.of();
                for (WithItem<?> definition : CommonUtils.safeCollection(definitions)) {
                    Set<String> visible = new HashSet<>(ctes);
                    if (definition.isRecursive()) {
                        visible.add(identifierKey(definition.getAliasName()));
                    }
                    verifyStatementObjects(context, executionContext, definition.getParenthesedStatement(), visible);
                    ctes.add(identifierKey(definition.getAliasName()));
                }
                objectFinder(names, routines, ctes).getTables(statement);
            }
        } catch (UnsupportedOperationException | ClassCastException | IllegalArgumentException e) {
            throw new DBException("Cannot verify objects in this parsed statement", e);
        }
        for (String name : names) {
            verifyObject(context, executionContext, name);
        }
        for (String name : routines) {
            verifyFunctionReference(context, executionContext, name);
        }
    }

    protected final void verifyExpressionObjects(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull Expression expression
    ) throws DBException {
        Set<String> routines = new LinkedHashSet<>();
        Set<String> names = new LinkedHashSet<>();
        try {
            objectFinder(names, routines, Set.of()).getTables(expression);
        } catch (UnsupportedOperationException | ClassCastException | IllegalArgumentException e) {
            throw new DBException("Cannot verify objects in this expression", e);
        }
        for (String name : names) {
            verifyObject(context, executionContext, name);
        }
        for (String name : routines) {
            verifyFunctionReference(context, executionContext, name);
        }
    }

    @NotNull
    private static TablesNamesFinder<Void> objectFinder(
        @NotNull Set<String> names, @NotNull Set<String> routines, @NotNull Set<String> ctes
    ) {
        return new TablesNamesFinder<>() {
            @Override
            public <S> Void visit(@NotNull Table table, @Nullable S unused) {
                if (!CommonUtils.isEmpty(table.getSchemaName())
                    || table.getDatabase() != null && !CommonUtils.isEmpty(table.getDatabase().getDatabaseName())
                    || !ctes.contains(identifierKey(table.getName()))) {
                    names.add(table.getFullyQualifiedName());
                }
                return null;
            }

            @Override
            public <S> Void visit(@NotNull WithItem<?> item, @Nullable S unused) {
                // Definitions are verified separately with only the aliases visible at their declaration.
                return null;
            }

            @Override
            public <S> Void visit(@NotNull Select select, @Nullable S unused) {
                collectSelectObjects(select, names, routines, ctes);
                return null;
            }

            @Override
            public <S> Void visit(@NotNull PlainSelect select, @Nullable S unused) {
                collectSelectObjects(select, names, routines, ctes);
                return null;
            }

            @Override
            public <S> Void visit(@NotNull ParenthesedSelect select, @Nullable S unused) {
                collectSelectObjects(select, names, routines, ctes);
                return null;
            }

            @Override
            public <S> Void visit(@NotNull TableStatement select, @Nullable S unused) {
                return visit(select.getTable(), unused);
            }

            @Override
            public <S> Void visit(@NotNull Function function, @Nullable S unused) {
                routines.add(function.getName());
                return super.visit(function, unused);
            }
        };
    }

    private record SelectScope(@NotNull Node node, @NotNull Set<String> aliases) {
    }

    private static void collectSelectObjects(
        @NotNull Select select, @NotNull Set<String> names, @NotNull Set<String> routines, @NotNull Set<String> inheritedCtes
    ) {
        if (select.getASTNode() == null) {
            if (select instanceof TableStatement table) {
                names.add(table.getTable().getFullyQualifiedName());
                return;
            }
            throw new IllegalArgumentException("Cannot resolve this SELECT representation");
        }
        Deque<SelectScope> stack = new ArrayDeque<>();
        stack.push(new SelectScope(select.getASTNode(), inheritedCtes));
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
                        throw new IllegalArgumentException("Cannot verify a non-SELECT common-table definition here");
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
            if (value instanceof Function function) {
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
    protected static List<String> tokenize(@NotNull SQLDialect dialect, @NotNull String sql) throws DBException {
        if (sql.isBlank()) {
            return List.of();
        }
        try {
            boolean bracketIdentifiers = false;
            for (String[] quotes : ArrayUtils.safeArray(dialect.getIdentifierQuoteStrings())) {
                bracketIdentifiers |= quotes.length == 2 && quotes[0].equals("[") && quotes[1].equals("]");
            }
            String expandedSql = SQLUtils.expandExecutableComments(dialect, sql);
            CCJSqlParser lexer = new CCJSqlParser(SQLUtils.maskComments(dialect, expandedSql))
                .withSquareBracketQuotation(bracketIdentifiers)
                .withBackslashEscapeCharacter(dialect.getStringEscapeCharacter() == '\\');
            List<String> tokens = new ArrayList<>();
            while (true) {
                Token token = lexer.getNextToken();
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
        SQLIdentifierDetector identifiers = new SQLIdentifierDetector(executionContext.getDataSource().getSQLDialect());
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0 && tokens.get(i - 1).equals(".")) {
                continue;
            }
            Identifier name = readIdentifier(tokens, i);
            if (name != null && name.end() < tokens.size() && tokens.get(name.end()).equals("(")
                && !ignoredNames.contains(name.name())) {
                boolean qualified = identifiers.splitIdentifier(name.name()).length > 1;
                DBPKeywordType keyword = executionContext.getDataSource().getSQLDialect().getKeywordType(name.name());
                if (qualified || identifiers.isQuoted(name.name()) || keyword != DBPKeywordType.KEYWORD && keyword != DBPKeywordType.TYPE) {
                    verifyFunctionReference(context, executionContext, name.name());
                }
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
        if (!(object instanceof DBSCatalog)) {
            DBCExecutionContextDefaults<?, ?> defaults = original.getContextDefaults();
            if (defaults != null && defaults.getDefaultCatalog() != null && defaults.getDefaultCatalog().getName().equals(catalogName)) {
                object = defaults.getDefaultCatalog();
            }
        }
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
