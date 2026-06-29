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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.internal.AIMessages;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AIUtils {
    private static final Log log = Log.getLog(AIUtils.class);
    public static final double DEFAULT_TEMPERATURE = 0.0;

    @Nullable
    public static AIEngineDescriptor getActiveEngineDescriptor() {
        return AIEngineRegistry.getInstance().getEngineDescriptor(
            AISettingsManager.getInstance().getSettings().activeEngine()
        );
    }

    public static boolean hasValidConfiguration() throws DBException {
        AISettings aiSettings = AISettingsManager.getInstance().getSettings();
        AIEngineProperties configuration = aiSettings.getEngineConfiguration(aiSettings.activeEngine());
        return configuration.isValidConfiguration();
    }

    /**
     * Retrieves a secret value from the global secret controller.
     * If the secret value is empty, it returns the provided default value.
     */
    public static String getSecretValueOrDefault(
        @NotNull String secretId,
        @Nullable String defaultValue
    ) throws DBException {
        String secretValue = DBSSecretController.getGlobalSecretController().getPrivateSecretValue(secretId);
        if (CommonUtils.isEmpty(secretValue)) {
            return defaultValue;
        }

        return secretValue;
    }

    /**
     * Checks if the given DBPObject is eligible for AI description.
     *
     * @param dbpObject the object to check
     * @return true if the object can be described by AI, false otherwise
     */
    public static boolean isEligible(@Nullable DBPObject dbpObject) {
        if (dbpObject instanceof DataSourceDescriptor descriptor) {
            return descriptor.getDriver().isEmbedded();
        }
        return dbpObject instanceof DBSEntity
            || dbpObject instanceof DBSSchema
            || dbpObject instanceof DBSTableColumn
            || dbpObject instanceof DBSProcedure
            || dbpObject instanceof DBSTrigger
            || dbpObject instanceof DBSEntityConstraint;
    }

    /**
     * Retrieves the DDL for the given DBSObject if applicable.
     *
     * @param object  the DBSObject from which to retrieve the DDL
     * @param monitor the progress monitor
     */
    @Nullable
    public static String getObjectDDL(@Nullable DBSObject object, @NotNull DBRProgressMonitor monitor) {
        if (object instanceof DBSProcedure
            || object instanceof DBSTrigger
            || object instanceof DBSEntityConstraint
            || object instanceof DBSView
        ) {
            if (object instanceof DBPScriptObject scriptObject) {
                try {
                    return scriptObject.getObjectDefinitionText(
                        monitor, Map.of(
                            DBPScriptObject.OPTION_INCLUDE_COMMENTS, false,
                            DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS, false,
                            DBPScriptObject.OPTION_SKIP_INDEXES, true, // Exclude indexes
                            DBPScriptObject.OPTION_SKIP_DROPS, true // Exclude --DROP
                        )
                    );
                } catch (DBException e) {
                    log.debug(e);
                }
            }
        }
        return null;
    }

    public static boolean confirmExecutionIfNeeded(
        @NotNull DBPDataSource dataSource,
        @NotNull List<SQLScriptElement> scriptElements,
        boolean isCommand
    ) {
        if (DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            // TODO: change behavior in multiuser mode
            return true;
        }
        Set<SQLQueryCategory> queryCategories = SQLQueryCategory.categorizeScript(scriptElements);
        if (queryCategories.contains(SQLQueryCategory.UNKNOWN) && isConfirmationNeeded(AIConstants.AI_CONFIRM_OTHER)) {
            String message = isCommand ? AIMessages.ai_execute_command_confirm_other_message :
                AIMessages.ai_execute_query_confirm_other_message;
            return confirmExecute(AIMessages.ai_execute_query_title, message, dataSource, scriptElements);
        }

        if (queryCategories.contains(SQLQueryCategory.DDL) && isConfirmationNeeded(AIConstants.AI_CONFIRM_DDL)) {
            String message = isCommand ? AIMessages.ai_execute_command_confirm_ddl_message :
                AIMessages.ai_execute_query_confirm_ddl_message;
            return confirmExecute(AIMessages.ai_execute_query_title, message, dataSource, scriptElements);
        }
        if (queryCategories.contains(SQLQueryCategory.DML) && isConfirmationNeeded(AIConstants.AI_CONFIRM_DML)) {
            String message = isCommand ? AIMessages.ai_execute_command_confirm_dml_message :
                AIMessages.ai_execute_query_confirm_dml_message;
            return confirmExecute(AIMessages.ai_execute_query_title, message, dataSource, scriptElements);
        }
        if (queryCategories.contains(SQLQueryCategory.SQL) && isConfirmationNeeded(AIConstants.AI_CONFIRM_SQL)) {
            String message = isCommand ? AIMessages.ai_execute_command_confirm_sql_message :
                AIMessages.ai_execute_query_confirm_sql_message;
            return confirmExecute(AIMessages.ai_execute_query_title, message, dataSource, scriptElements);
        }
        return true;
    }

    public static void disableAutoCommitIfNeeded(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<SQLScriptElement> scriptElements,
        @Nullable DBCExecutionContext context
    ) throws DBException {
        if (!SQLQueryCategory.categorizeScript(scriptElements).contains(SQLQueryCategory.DML)) {
            return;
        }

        AIQueryConfirmationRule dmlRule = CommonUtils.valueOf(
            AIQueryConfirmationRule.class,
            DBWorkbench.getPlatform().getPreferenceStore().getString(AIConstants.AI_CONFIRM_DML),
            AIQueryConfirmationRule.CONFIRM
        );
        if (dmlRule == AIQueryConfirmationRule.DISABLE_AUTOCOMMIT) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
            if (txnManager != null && txnManager.isAutoCommit()) {
                txnManager.setAutoCommit(monitor, false);
                showAutoCommitDisabledNotification();
            }
        }
    }

    private static void showAutoCommitDisabledNotification() {
        DBWorkbench.getPlatformUI().showWarningNotification(
            AIMessages.ai_execute_query_auto_commit_disabled_title,
            AIMessages.ai_execute_query_auto_commit_disabled_message
        );
    }

    private static boolean isConfirmationNeeded(@NotNull String actionName) {
        return CommonUtils.valueOf(
            AIQueryConfirmationRule.class,
            DBWorkbench.getPlatform().getPreferenceStore().getString(actionName),
            AIQueryConfirmationRule.CONFIRM
        ) == AIQueryConfirmationRule.CONFIRM;
    }

    private static boolean confirmExecute(
        @NotNull String title,
        @NotNull String message,
        @NotNull DBPDataSource dataSource,
        @NotNull List<SQLScriptElement> scriptElements
    ) {
        String delimiter = SQLUtils.getDefaultScriptDelimiter(dataSource.getSQLDialect());
        String scriptText = scriptElements.stream()
            .map(Object::toString)
            .collect(Collectors.joining(delimiter + "\n"));
        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        return serviceSQL != null ?
            serviceSQL.confirmQueryExecution(title, message, scriptText, getContextProvider(scriptElements), true) :
            DBWorkbench.getPlatformUI().confirmAction(title, message, true);
    }

    @NotNull
    private static DBPContextProvider getContextProvider(@NotNull List<SQLScriptElement> script) {
        DBPDataSource dataSource = script.stream().findFirst()
            .map(SQLScriptElement::getDataSource)
            .orElse(null);
        return new DataSourceContextProvider(dataSource);
    }

    public static void updateScopeSettingsIfNeeded(
        @NotNull AIContextSettings settings,
        @NotNull DBPDataSourceContainer container,
        @Nullable DBCExecutionContext executionContext
    ) {
        if (settings.getScope() != null || !container.isConnected()) {
            return;
        }
        if (executionContext == null || executionContext.getContextDefaults() == null) {
            // default scope
            settings.setScope(AIDatabaseScope.CURRENT_DATABASE);
            return;
        }
        DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
        if (contextDefaults.getDefaultCatalog() != null || contextDefaults.supportsCatalogChange()) {
            settings.setScope(AIDatabaseScope.CURRENT_DATABASE);
        } else {
            settings.setScope(AIDatabaseScope.CURRENT_DATASOURCE);
        }
    }

    public static boolean isExcludableObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject obj
    ) {
        return DBUtils.isSystemObject(obj)
            || DBUtils.isHiddenObject(obj)
            || obj instanceof DBSTablePartition
            || DBNUtils.getNodeByObject(monitor, obj, false) == null;
    }

    @NotNull
    public static Map<String, AIModel> modelMap(@NotNull AIModel ... models) {
        return Stream.of(models).collect(Collectors.toMap(
            AIModel::name,
            Function.identity()
        ));
    }

    @NotNull
    public static Optional<AIModel> getModelByName(@NotNull Map<String, AIModel> models, @Nullable String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return Optional.empty();
        }
        modelName = modelName.toLowerCase(Locale.ROOT);

        // Try to find more generic model
        String tmpName = modelName;
        for (;;) {
            AIModel model = models.get(tmpName);
            if (model != null) {
                return Optional.of(model);
            }
            int divPos = tmpName.lastIndexOf('-');
            if (divPos > 0) {
                tmpName = tmpName.substring(0, divPos);
            } else {
                break;
            }
        }

        return Optional.empty();
    }

    public static boolean isCatalogInScope(
        @NotNull AIDatabaseContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSCatalog catalog
    ) {
        switch (context.getScope()) {
            case CURRENT_DATABASE, CURRENT_SCHEMA -> {
                DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
                if (contextDefaults == null) {
                    return false;
                }
                return contextDefaults.getDefaultCatalog() == catalog;
            }
            case CURRENT_DATASOURCE -> {
                return true;
            }
            case CUSTOM -> {
                return context.getCustomCatalogs().contains(catalog);
            }
            default -> {
                return false;
            }
        }
    }

    public static boolean isSchemaInScope(
        @NotNull AIDatabaseContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSSchema schema
    ) {
        switch (context.getScope()) {
            case CURRENT_DATABASE -> {
                if (schema.getParentObject() instanceof DBSCatalog parentCatalog) {
                    DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
                    if (contextDefaults != null) {
                        DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
                        if (defaultCatalog != null) {
                            return parentCatalog == defaultCatalog;
                        }
                    }
                }
                // If there is no default catalog or server doesn't support catalogs
                return true;
            }
            case CURRENT_SCHEMA -> {
                DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
                if (contextDefaults == null) {
                    return false;
                }
                return contextDefaults.getDefaultSchema() == schema;
            }
            case CURRENT_DATASOURCE -> {
                return true;
            }
            case CUSTOM -> {
                List<DBSObject> customEntities = context.getCustomEntities();
                return context.getCustomSchemas().contains(schema) ||
                    (customEntities != null && customEntities.contains(schema.getParentObject()));
            }
            default -> {
                return false;
            }
        }
    }

    public static boolean isObjectInScope(
        @NotNull AIDatabaseContext context,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObject object
    ) {
        if (object instanceof DBPDataSource || object instanceof DBPDataSourceContainer) {
            return true;
        }
        if (object instanceof DBSCatalog catalog) {
            return isCatalogInScope(context, executionContext, catalog);
        } else if (object instanceof DBSSchema schema) {
            return isSchemaInScope(context, executionContext, schema);
        } else {
            if (context.getScope() == AIDatabaseScope.CUSTOM) {
                // Check that this object in the custom entity list
                List<DBSObject> customEntities = context.getCustomEntities();
                if (customEntities != null) {
                    if (customEntities.contains(object)) {
                        return true;
                    }
                    DBSSchema schema = DBStructUtils.getObjectSchema(object);
                    if (schema != null && customEntities.contains(schema)) {
                        return true;
                    }
                    DBSCatalog catalog = DBStructUtils.getObjectCatalog(object);
                    if (catalog != null && customEntities.contains(catalog)) {
                        return true;
                    }
                }
                return false;
            } else {
                // Just check that parent schema/catalog/datasource is in scope
                DBSObject parentObject = object.getParentObject();
                return parentObject != null && isObjectInScope(context, executionContext, parentObject);
            }
        }
    }

    /**
     * Normalizes a temperature value used for AI model inference.
     * If the supplied value is not a finite number (e.g. {@code NaN} or {@code Infinity})
     * it is replaced with {@link #DEFAULT_TEMPERATURE}.
     */
    public static double normalizeTemperature(double temperature) {
        return Double.isFinite(temperature) ? temperature : DEFAULT_TEMPERATURE;
    }

    public static boolean hasInformationFunctions(@NotNull AIToolboxManager toolboxManager, @NotNull List<AIFunctionCall> functionCalls) {
        for (AIFunctionCall fc : functionCalls) {
            AIFunctionDescriptor function = fc.getOrResolveFunction(toolboxManager);
            if (function != null && function.getType() == AIFunctionType.INFORMATION) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a formatted string with the type and full name of the DBSObject.
     *
     * @param dbsObject the DBSObject to format
     */
    @NotNull
    public static String getDatabaseObjectInfo(@NotNull DBSObject dbsObject, boolean isPromptInfo) {
        String objectInfo;
        String objectFullName = DBUtils.getObjectFullName(dbsObject, DBPEvaluationContext.DDL);
        if (dbsObject instanceof DataSourceDescriptor) {
            objectInfo = isPromptInfo ? "database with following tables" : "listed database tables";
        } else if (dbsObject instanceof DBSSchema) {
            objectInfo = "schema '" + objectFullName + "'";
        } else if (dbsObject instanceof DBSEntity) {
            objectInfo = "table '" + objectFullName + "'";
        } else if (dbsObject instanceof DBSEntityAttribute) {
            objectInfo = "column '" + objectFullName + "' in the table '" +
                DBUtils.getObjectFullName(dbsObject.getParentObject(), DBPEvaluationContext.DDL) + "'";
        } else {
            objectInfo = DBUtils.getObjectTypeName(dbsObject) + " '" + objectFullName + "'";
        }
        return objectInfo;
    }

    /**
     * Finds a schema within the given function context and schema name.
     * The method identifies and retrieves the schema from the specified database or schema scope
     * based on the provided context and schema name.
     *
     * @param context the function context containing the database connection and related settings; must not be null
     * @param schemaName the full or partial name of the schema to search; must not be null
     * @return the schema object if found
     * @throws DBException if the schema cannot be found or if there are issues with database access or scope determination
     */
    @NotNull
    public static DBSSchema findSchemaInContext(
        @NotNull AIFunctionContext context,
        @NotNull String schemaName
    ) throws DBException {
        AIDatabaseContext dbContext = context.getContext();
        if (dbContext == null) {
            throw new DBException("Not connected to database");
        }
        DBCExecutionContext executionContext = dbContext.getExecutionContext();
        if (!(executionContext.getDataSource() instanceof DBSObjectContainer dsObjectContainer)) {
            throw new DBException("Cannot determine object container in data source " + executionContext.getDataSource());
        }
        SQLDialect dialect = dsObjectContainer.getDataSource().getSQLDialect();
        var contextDefaults = executionContext.getContextDefaults();
        DBSCatalog defaultCatalog = null;
        if (contextDefaults != null) {
            defaultCatalog = contextDefaults.getDefaultCatalog();
        }

        String[] nameParts = schemaName.split("\\.");
        DBSObjectContainer searchContainer;
        String actualSchemaName;

        if (nameParts.length == 2) {
            String databaseName = nameParts[0];
            actualSchemaName = nameParts[1];

            DBSObject database = dsObjectContainer.getChild(context.getMonitor(), databaseName);
            if (database instanceof DBSObjectContainer databaseContainer) {
                searchContainer = databaseContainer;
            } else {
                throw new DBException("Database '" + databaseName + "' not found");
            }
        } else {
            actualSchemaName = schemaName;
            searchContainer = defaultCatalog != null ? defaultCatalog : dsObjectContainer;
        }

        actualSchemaName = dialect.getUnquotedIdentifier(actualSchemaName);
        DBSObject schema = searchContainer.getChild(context.getMonitor(), actualSchemaName);

        if (schema == null) {
            throw new DBException("Cannot find schema '" + schemaName + "' in current scope");
        } else if (schema instanceof DBSSchema dbsSchema) {
            return dbsSchema;
        } else {
            throw new DBException("Schema '" + schemaName + "' is not a valid schema object");
        }
    }

    /**
     * Finds a database table within the given function context and table name.
     * Searches the table in the current database or schema scope based on the context settings and the provided table name.
     *
     * @param context the function context containing database-related information; must not be null
     * @param tableName the full or partial name of the table to search; must not be null
     * @return the data container representing the table if found
     * @throws DBException if the table cannot be found or other database-related errors occur
     */
    @NotNull
    public static DBSDataContainer findTableInContext(
        @NotNull AIFunctionContext context,
        @NotNull String tableName
    ) throws DBException {
        return findTableInContext(context, null, null, tableName);
    }

    @NotNull
    public static DBSDataContainer findTableInContext(
        @NotNull AIFunctionContext context,
        @Nullable String catalogName,
        @Nullable String schemaName,
        @NotNull String tableName
    ) throws DBException {
        AIDatabaseContext dbContext = context.getContext();
        if (dbContext == null) {
            throw new DBException("Not connected to database");
        }
        DBCExecutionContext executionContext = dbContext.getExecutionContext();
        DBPDataSource dataSource = executionContext.getDataSource();
        if (!(dataSource instanceof DBSObjectContainer dsObjectContainer)) {
            throw new DBException("Cannot determine object container in data source " + dataSource);
        }
        SQLDialect dialect = dataSource.getSQLDialect();

        SQLIdentifierDetector wordDetector = new SQLIdentifierDetector(dialect);
        String[] nameParts = wordDetector.splitIdentifier(tableName);
        if (nameParts.length == 1) {
            if (!CommonUtils.isEmpty(schemaName)) {
                if (!CommonUtils.isEmpty(catalogName)) {
                    nameParts = new String[] { catalogName, schemaName, nameParts[0] };
                } else {
                    nameParts = new String[] { schemaName, nameParts[0] };
                }
            } else if (!CommonUtils.isEmpty(catalogName)) {
                nameParts = new String[] { catalogName, nameParts[0] };
            }
        }
        DBSObject table = findObjectByFQN(
            context.getMonitor(),
            executionContext,
            dsObjectContainer,
            nameParts
        );

        if (table == null) {
            throw new DBException("Table '" + tableName + "' not found");
        } else if (table instanceof DBSDataContainer entity) {
            return entity;
        } else {
            throw new DBException("Table '" + tableName + "' is not a data container (" + table + ")");
        }
    }

    /**
     * Find object by FQN.
     * This is tricky. We may receive short name (just a table) or partial FQN
     * schema.table or full FQN catalog.schema.table.
     * We need to guess what each part means.
     */
    @Nullable
    public static DBSObject findObjectByFQN(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer rootContainer,
        @NotNull String[] nameParts
    ) throws DBException {
        SQLDialect dialect = executionContext.getDataSource().getSQLDialect();
        DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
        boolean supportsSchemas = contextDefaults != null &&
            (contextDefaults.getDefaultSchema() != null || contextDefaults.supportsSchemaChange());
        String objectName = DBUtils.getUnQuotedNormalizedIdentifier(dialect, nameParts[nameParts.length - 1]);
        String schemaName = null;
        String catalogName = null;

        if (nameParts.length > 2) {
            // Full FQN
            catalogName = DBUtils.getUnQuotedNormalizedIdentifier(dialect, nameParts[nameParts.length - 3]);
            schemaName = DBUtils.getUnQuotedNormalizedIdentifier(dialect, nameParts[nameParts.length - 2]);
        } else if (nameParts.length > 1) {
            // Full or partial
            // First part is schema or catalog
            String parentName = DBUtils.getUnQuotedNormalizedIdentifier(dialect, nameParts[nameParts.length - 2]);
            if (supportsSchemas) {
                schemaName = parentName;
            } else {
                catalogName = parentName;
            }
        }

        DBSObject object = DBUtils.getObjectByPath(
            monitor, executionContext, rootContainer, catalogName, schemaName, objectName, true);
        if (object == null) {
            object = DBUtils.getObjectByPath(
                monitor, executionContext, rootContainer, catalogName, schemaName, objectName, true, false);
        }
        return object;
    }

    public static boolean useStreamMode() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AIConstants.AI_USE_STREAM_MODE);
    }
}
