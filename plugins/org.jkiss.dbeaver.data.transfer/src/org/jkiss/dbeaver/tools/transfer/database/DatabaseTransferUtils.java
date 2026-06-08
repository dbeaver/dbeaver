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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.AbstractCommandContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.tools.transfer.internal.DTActivator;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.lang.reflect.Method;
import java.util.*;

/**
* DatabaseTransferUtils
*/
public class DatabaseTransferUtils {

    private static final Log log = Log.getLog(DatabaseTransferUtils.class);

    private static final boolean USE_STRUCT_DDL = true;

    private static final Pair<DBPDataKind, String> DATA_TYPE_UNKNOWN = new Pair<>(DBPDataKind.UNKNOWN, null);
    private static final Pair<DBPDataKind, String> DATA_TYPE_INTEGER = new Pair<>(DBPDataKind.NUMERIC, "INTEGER");
    private static final Pair<DBPDataKind, String> DATA_TYPE_BIGINT = new Pair<>(DBPDataKind.NUMERIC, "BIGINT");
    private static final Pair<DBPDataKind, String> DATA_TYPE_REAL = new Pair<>(DBPDataKind.NUMERIC, "REAL");
    private static final Pair<DBPDataKind, String> DATA_TYPE_BOOLEAN = new Pair<>(DBPDataKind.BOOLEAN, "BOOLEAN");
    private static final Pair<DBPDataKind, String> DATA_TYPE_STRING = new Pair<>(DBPDataKind.STRING, "VARCHAR");
    private static final Pair<DBPDataKind, String> DATA_TYPE_NATIONAL_STRING = new Pair<>(DBPDataKind.STRING, "NVARCHAR");

    public static void refreshDatabaseModel(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseConsumerSettings consumerSettings,
        @Nullable DatabaseMappingContainer containerMapping
    ) throws DBException {
        monitor.subTask("Refresh database model");
        DBSObjectContainer container = consumerSettings.getContainer();
        if (container != null) {
            DBNModel navigatorModel = DBNUtils.getNavigatorModel(container);
            if (navigatorModel != null) {
                var containerNode = navigatorModel.getNodeByObject(monitor, container, false);
                if (containerNode != null) {
                    containerNode.refreshNode(monitor, containerMapping);
                }
            } else if (container instanceof DBPRefreshableObject refreshableObject) {
                refreshableObject.refreshObject(monitor);
            }
            refreshDatabaseMappings(monitor, consumerSettings, containerMapping, false);
        }
    }

    public static void refreshDatabaseMappings(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseConsumerSettings consumerSettings,
        @Nullable DatabaseMappingContainer containerMapping,
        boolean force
    ) throws DBException {
        DBSObjectContainer container = consumerSettings.getContainer();
        if (container == null) {
            log.debug("Null target container");
            return;
        }
        if (containerMapping == null) {
            log.debug("Null container mapping");
            return;
        }

        // Reflect database changes in mappings
        {
            boolean updateMappingTarget = false;
            boolean updateMappingAttributes = false;

            switch (containerMapping.getMappingType()) {
                case create:
                case recreate:
                    updateMappingTarget = true;
                    updateMappingAttributes = true;
                    break;
                case existing:
                    updateMappingTarget = false;
                    updateMappingAttributes = true;
                    break;
                default:
                    break;
            }

            if (updateMappingTarget || force) {
                monitor.subTask("Refresh database mappings");

                DBSObject newTarget = container.getChild(monitor, DBUtils.getUnQuotedIdentifier(container.getDataSource(), containerMapping.getTargetName()));
                if (newTarget == null) {
                    throw new DBCException("New table " + containerMapping.getTargetName() + " not found in container " + DBUtils.getObjectFullName(container, DBPEvaluationContext.UI));
                } else if (!(newTarget instanceof DBSDataManipulator)) {
                    throw new DBCException("New table " + DBUtils.getObjectFullName(newTarget, DBPEvaluationContext.UI) + " doesn't support data manipulation");
                }
                containerMapping.setTarget((DBSDataManipulator) newTarget, false);
                if (containerMapping.getMappingType() == DatabaseMappingType.create) {
                    containerMapping.setMappingType(DatabaseMappingType.existing, false);
                }
            }

            if (updateMappingAttributes || force) {
                for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                    if (attr.getMappingType() == DatabaseMappingType.create || (attr.getMappingType().isValid() && force)) {
                        attr.updateMappingType(monitor, false, false);
                        if (attr.getTarget() == null) {
                            log.debug("Can't find target attribute '" + attr.getTargetName() + "' in '" + containerMapping.getTargetName() + "'");
                        }
                    }
                }
            }
        }
    }

    /**
     * Method generates array of actions for table creation for containers with correct mapping type.
     * Has old code inside with the simple table creations.
     *
     * @param monitor progress monitor
     * @param executionContext execution context for DDL generation
     * @param schema table container
     * @param containerMapping mapping container can not be null
     * @param changedProperties list of properties what feature table must have
     * @return array of persist actions table creation
     * @throws DBException on any DB error
     */
    @NotNull
    public static DBEPersistAction[] generateTargetTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer schema,
        @NotNull DatabaseMappingContainer containerMapping,
        @Nullable Map<DBPPropertyDescriptor, Object> changedProperties
    ) throws DBException {
        return generateTargetTableDDL(monitor, executionContext, schema, containerMapping, changedProperties, null);
    }

    /**
     * Method generates array of actions for table creation and optional key migration.
     *
     * @param monitor progress monitor
     * @param executionContext execution context for DDL generation
     * @param schema table container
     * @param containerMapping mapping container can not be null
     * @param changedProperties list of properties what feature table must have
     * @param consumerSettings transfer settings; when set, PK/FK migration flags are respected
     * @return array of persist actions table creation
     * @throws DBException on any DB error
     */
    @NotNull
    public static DBEPersistAction[] generateTargetTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer schema,
        @NotNull DatabaseMappingContainer containerMapping,
        @Nullable Map<DBPPropertyDescriptor, Object> changedProperties,
        @Nullable DatabaseConsumerSettings consumerSettings
    ) throws DBException {
        if (containerMapping.getMappingType() == DatabaseMappingType.skip) {
            return new DBEPersistAction[0];
        }
        // Check whether we have any changes in mappings
        if (containerMapping.getMappingType() == DatabaseMappingType.existing) {
            boolean hasChanges = false;
            for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                if (attr.getMappingType() != DatabaseMappingType.existing && attr.getMappingType() != DatabaseMappingType.skip) {
                    hasChanges = true;
                    break;
                }
            }
            for (DatabaseMappingConstraint constraint : containerMapping.getConstraintMappings(monitor)) {
                if (shouldMigrateTableConstraint(constraint, consumerSettings)) {
                    hasChanges = true;
                    break;
                }
            }
            if (!hasChanges) {
                return new DBEPersistAction[0];
            }
        }
        monitor.subTask("Validate table structure table '" + containerMapping.getTargetName() + "'");
        if (USE_STRUCT_DDL) {
            try {
                final List<DBEPersistAction> actions = new ArrayList<>();
                generateStructTableDDL(monitor, executionContext, schema, containerMapping, actions, changedProperties, consumerSettings);
                return actions.toArray(DBEPersistAction[]::new);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Can't create or update target table", null, e);
                if (!DBWorkbench.getPlatformUI().confirmAction(
                    "Generate DDL automatically",
                    "Do you want to create or update target object with auto-generated SQL script?")) {
                    throw new DBException("Target table create or update was canceled");
                }
            }
        }

        // Struct doesn't work (no proper object managers?)
        // Try plain SQL mode

        DBPDataSource dataSource = executionContext.getDataSource();

        String tableName;
        if (containerMapping.getMappingType() == DatabaseMappingType.create) {
            tableName = getTransformedName(dataSource, containerMapping.getTargetName(), false);
        } else {
            tableName = DBObjectNameCaseTransformer.transformName(dataSource, containerMapping.getTargetName());
        }
        containerMapping.setTargetName(tableName);

        if (CommonUtils.isEmpty(tableName)) {
            return new DBEPersistAction[0];
        }

        List<DBEPersistAction> actions = new ArrayList<>();
        StringBuilder sql = new StringBuilder(500);

        if (containerMapping.getMappingType() == DatabaseMappingType.recreate && containerMapping.getTarget() != null) {
            sql.append("DROP TABLE ");
            getTableFullName(schema, dataSource, sql, tableName);
            sql.append(dataSource.getSQLDialect().getScriptDelimiters()[0]);
        }

        if (containerMapping.hasNewTargetObject()) {
            sql.append("CREATE TABLE ");
            getTableFullName(schema, dataSource, sql, tableName);
            sql.append("(\n");
            Map<DBSAttributeBase, DatabaseMappingAttribute> mappedAttrs = new HashMap<>();
            for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                if (attr.getMappingType() != DatabaseMappingType.create) {
                    continue;
                }
                if (!mappedAttrs.isEmpty()) {
                    sql.append(",\n");
                }
                sql.append("\t");
                appendAttributeClause(dataSource, sql, attr);
                mappedAttrs.put(attr.getSource(), attr);
            }
            if (shouldMigratePrimaryKeys(consumerSettings) && containerMapping.getSource() instanceof DBSEntity sourceEntity) {
                // Make primary key
                appendPrimaryKeyClause(monitor, dataSource, sql, containerMapping, sourceEntity, mappedAttrs, consumerSettings);
            }
            sql.append(")");
            actions.add(new SQLDatabasePersistAction("Table DDL", sql.toString()));
        } else {
            for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                if (attr.getMappingType() == DatabaseMappingType.create) {
                    actions.add(generateTargetAttributeDDL(dataSource, attr));
                }
            }
            if (containerMapping.getTarget() instanceof DBSEntity targetEntity) {
                for (DatabaseMappingConstraint constraintMapping : containerMapping.getConstraintMappings(monitor)) {
                    if (!shouldMigrateTableConstraint(constraintMapping, consumerSettings)) {
                        continue;
                    }
                    List<DBSEntityAttribute> targetColumns = new ArrayList<>(constraintMapping.getAttributeMappings().size());
                    for (DatabaseMappingConstraintAttribute attributeMapping : constraintMapping.getAttributeMappings()) {
                        DBSEntityAttribute targetAttribute = attributeMapping.getTargetAttributeMapping().getTarget();
                        if (targetAttribute == null) {
                            targetColumns.clear();
                            break;
                        }
                        targetColumns.add(targetAttribute);
                    }
                    if (targetColumns.isEmpty()) {
                        continue;
                    }
                    String constraintName = DBObjectNameCaseTransformer.transformName(
                        dataSource,
                        CommonUtils.isEmpty(constraintMapping.getTargetName()) ?
                            targetEntity.getName() + "_" + constraintMapping.getConstraintType().getId().toUpperCase(Locale.ROOT) :
                            constraintMapping.getTargetName());
                    StringBuilder constraintSql = new StringBuilder(100);
                    constraintSql.append("ALTER TABLE ")
                        .append(DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.DDL))
                        .append(" ADD CONSTRAINT ")
                        .append(DBUtils.getQuotedIdentifier(dataSource, constraintName));
                    constraintSql.append(" PRIMARY KEY (");
                    for (int i = 0; i < targetColumns.size(); i++) {
                        if (i > 0) {
                            constraintSql.append(",");
                        }
                        constraintSql.append(DBUtils.getQuotedIdentifier(targetColumns.get(i)));
                    }
                    constraintSql.append(")");
                    actions.add(new SQLDatabasePersistAction("Create " + constraintMapping.getConstraintType().getName().toLowerCase(Locale.ROOT), constraintSql.toString()));
                }
            }
        }
        return actions.toArray(new DBEPersistAction[0]);
    }

    private static void appendPrimaryKeyClause(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull StringBuilder sql,
        @NotNull DatabaseMappingContainer containerMapping,
        @NotNull DBSEntity sourceEntity,
        @NotNull Map<DBSAttributeBase, DatabaseMappingAttribute> mappedAttrs,
        @Nullable DatabaseConsumerSettings consumerSettings
    ) throws DBException {
        if (consumerSettings == null) {
            boolean finished = false;
            Collection<? extends DBSEntityAttribute> identifier = DBUtils.getBestTableIdentifier(monitor, sourceEntity);
            if (!CommonUtils.isEmpty(identifier)) {
                List<DatabaseMappingAttribute> targetColumns = new ArrayList<>(identifier.size());
                for (DBSEntityAttribute idAttr : identifier) {
                    DatabaseMappingAttribute mappedAttr = mappedAttrs.get(idAttr);
                    if (mappedAttr == null) {
                        finished = true;
                        break;
                    }
                    targetColumns.add(mappedAttr);
                }
                if (!finished) {
                    if (!targetColumns.isEmpty()) {
                        sql.append(",\n\tPRIMARY KEY (");
                        for (int i = 0; i < targetColumns.size(); i++) {
                            if (i > 0) {
                                sql.append(",");
                            }
                            sql.append(DBUtils.getQuotedIdentifier(dataSource, targetColumns.get(i).getTargetName()));
                        }
                        sql.append(")\n");
                    }
                }
            }
            return;
        }

        DatabaseMappingConstraint primaryKeyMapping = null;
        for (DatabaseMappingConstraint constraintMapping : containerMapping.getConstraintMappings(monitor)) {
            if (constraintMapping.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY &&
                shouldMigrateTableConstraint(constraintMapping, containerMapping.getSettings())) {
                primaryKeyMapping = constraintMapping;
                break;
            }
        }
        if (primaryKeyMapping == null) {
            return;
        }

        List<DatabaseMappingAttribute> targetColumns = new ArrayList<>(primaryKeyMapping.getAttributeMappings().size());
        for (DatabaseMappingConstraintAttribute attributeMapping : primaryKeyMapping.getAttributeMappings()) {
            DBSEntityAttribute sourceAttribute = attributeMapping.getSourceAttribute();
            DatabaseMappingAttribute targetAttribute = sourceAttribute == null ? null : mappedAttrs.get(sourceAttribute);
            if (targetAttribute == null) {
                return;
            }
            targetColumns.add(targetAttribute);
        }
        if (targetColumns.isEmpty()) {
            return;
        }
        sql.append(",\n\tPRIMARY KEY (");
        for (int i = 0; i < targetColumns.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append(DBUtils.getQuotedIdentifier(dataSource, targetColumns.get(i).getTargetName()));
        }
        sql.append(")\n");
    }

    /**
     * Transform target mapping name by mapping rules (if we have them)
     *
     * @param dataSource for preferences and dialect info
     * @param targetName name for transformation
     * @param skipCaseChanging true if we do not want to change name case of the original name
     * @return transformed target name (container or attribute)
     */
    @NotNull
    public static String getTransformedName(@NotNull DBPDataSource dataSource, @NotNull String targetName, boolean skipCaseChanging) {
        String finalName = targetName;
        DBPPreferenceStore dbpPreferenceStore = dataSource.getContainer().getPreferenceStore();
        DBPPreferenceStore store = DTActivator.getDefault().getPreferences();
        MappingNameCase nameCase = MappingNameCase.getCaseFromPreferences(dbpPreferenceStore, store);
        MappingReplaceMechanism mechanism = MappingReplaceMechanism.getCaseFromPreferences(dbpPreferenceStore, store);
        if (nameCase != MappingNameCase.DEFAULT) {
            finalName = nameCase.getIdentifierCase().transform(targetName);
        } else if (!skipCaseChanging && mechanism != MappingReplaceMechanism.CAMELCASE) {
            finalName = DBObjectNameCaseTransformer.transformName(dataSource, targetName);
        }
        if (mechanism != MappingReplaceMechanism.ABSENT && CommonUtils.isNotEmpty(finalName) && finalName.contains(" ")) {
            if (MappingReplaceMechanism.UNDERSCORES == mechanism) {
                finalName = finalName.replaceAll(" ", "_");
            } else if (MappingReplaceMechanism.CAMELCASE == mechanism
                && !(nameCase == MappingNameCase.DEFAULT && dataSource.getSQLDialect().storesUnquotedCase() == DBPIdentifierCase.UPPER)
                && nameCase != MappingNameCase.UPPER // No need to transform upper case names
            ) {
                String camelCaseName = CommonUtils.toCamelCase(finalName);
                if (CommonUtils.isNotEmpty(camelCaseName)) {
                    finalName = camelCaseName.replaceAll(" ", "");
                }
            }
        }
        if (CommonUtils.isNotEmpty(finalName)) {
            // Add quotes for the result name if needed
            return DBUtils.getQuotedIdentifier(dataSource, finalName);
        }
        log.debug("Can't transform target attribute name");
        return targetName;
    }

    private static void getTableFullName(
        @Nullable DBSObjectContainer schema,
        @NotNull DBPDataSource dataSource,
        @NotNull StringBuilder sql,
        @NotNull String tableName
    ) {
        if (schema instanceof DBSSchema || schema instanceof DBSCatalog) {
            sql.append(DBUtils.getFullyQualifiedName(dataSource, schema.getName(), tableName));
        } else {
            sql.append(DBUtils.getQuotedIdentifier(dataSource, tableName));
        }
    }

    @NotNull
    private static SQLObjectEditor<DBSEntity, ?> getTableManager(
        @NotNull DBERegistry editorsRegistry,
        @NotNull Class<? extends DBSObject> tableClass
    ) throws DBException {
        SQLObjectEditor<DBSEntity, ?> tableManager = editorsRegistry.getObjectManager(tableClass, SQLObjectEditor.class);
        if (tableManager == null) {
            throw new DBException("Table manager not found for '" + tableClass.getName() + "'");
        }
        return tableManager;
    }

    @NotNull
    private static Class<? extends DBSObject> getTableClass(@Nullable DBRProgressMonitor monitor, @NotNull DBSObjectContainer schema)
            throws DBException {
        Class<? extends DBSObject> tableClass = schema.getPrimaryChildType(monitor);
        if (!DBSEntity.class.isAssignableFrom(tableClass)) {
            throw new DBException("Wrong table container child type: " + tableClass.getName());
        }
        return tableClass;
    }

    @Nullable
    private static SQLConstraintManager<?, ?> getConstraintManager(
        @NotNull DBERegistry editorsRegistry,
        @Nullable Class<?>[] childTypes
    ) {
        if (childTypes == null) {
            return null;
        }
        Class<? extends DBSEntityConstraint> constraintClass = BeanUtils.findAssignableType(childTypes, DBSEntityConstraint.class);
        return constraintClass == null ? null : editorsRegistry.getObjectManager(constraintClass, SQLConstraintManager.class);
    }

    /**
     * This method returns object of the feature new created table and fill the table creating actions list
     *
     * @param monitor progress monitor
     * @param executionContext not null execution context to get datasource etc.
     * @param schema feature table container
     * @param containerMapping mapping container
     * @param actions will be filled by persist actions
     * @param changedProperties list of properties what feature table must have
     * @param consumerSettings transfer settings; when set, primary key migration flag is respected
     * @return DBSEntity table object that can be used as temporary to work with its properties, for example
     * @throws DBException on any DB error
     */
    @NotNull
    public static DBSEntity generateStructTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer schema,
        @NotNull DatabaseMappingContainer containerMapping,
        @NotNull List<DBEPersistAction> actions,
        @Nullable Map<DBPPropertyDescriptor, Object> changedProperties,
        @Nullable DatabaseConsumerSettings consumerSettings
    ) throws DBException {
        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();

        try {
            Class<? extends DBSObject> tableClass = getTableClass(monitor, schema);
            SQLObjectEditor<DBSEntity, ?> tableManager = getTableManager(editorsRegistry, tableClass);
            if (!tableManager.canCreateObject(schema)) {
                throw new DBException(
                    "Table create is not supported by driver " + schema.getDataSource().getContainer().getDriver().getName());
            }
            Class<? extends DBSEntityAttribute> attrClass;
            SQLObjectEditor<DBSEntityAttribute, ?> attributeManager;
            Class<?>[] childTypes = null;
            if (executionContext.getDataSource().getInfo().isDynamicMetadata()) {
                attrClass = null;
                attributeManager = null;
            } else {
                if (!(tableManager instanceof DBEStructEditor<?> dbeStructEditor)) {
                    throw new DBException(
                        "Table create not supported by " + executionContext.getDataSource().getContainer().getDriver().getName());
                }
                childTypes = dbeStructEditor.getChildTypes();
                attrClass = BeanUtils.findAssignableType(childTypes, DBSEntityAttribute.class);
                if (attrClass == null) {
                    throw new DBException("Column manager not found for '" + tableClass.getName() + "'");
                }
                attributeManager = editorsRegistry.getObjectManager(attrClass, SQLObjectEditor.class);
            }

            Map<String, Object> options = new HashMap<>();
            options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);

            DBECommandContext commandContext = new TargetCommandContext(executionContext);
            Map<DatabaseMappingAttribute, DBSEntityAttribute> targetAttributesByMapping = new LinkedHashMap<>();

            String tableFinalName;

            DBSEntity table;
            DBECommand createCommand = null;
            if (containerMapping.getMappingType() == DatabaseMappingType.create || (
                containerMapping.getMappingType() == DatabaseMappingType.recreate && containerMapping.getTarget() == null)) {
                table = tableManager.createNewObject(monitor, commandContext, schema, null, options);
                applyPropertyChanges(monitor, changedProperties, commandContext, containerMapping, table);
                tableFinalName = getTableFinalName(containerMapping.getTargetName(), tableClass, table, true);
                createCommand = tableManager.makeCreateCommand(table, options);
            } else {
                table = (DBSEntity) containerMapping.getTarget();
                if (table == null) {
                    throw new DBException("Internal error - target table not set");
                }
                if (containerMapping.getMappingType() == DatabaseMappingType.recreate) {
                    tableManager.deleteObject(commandContext, table, options);
                    table = tableManager.createNewObject(
                        monitor,
                        commandContext,
                        table.getParentObject(),
                        null,
                        options);
                    applyPropertyChanges(monitor, changedProperties, commandContext, containerMapping, table);
                    tableFinalName = getTableFinalName(containerMapping.getTargetName(), tableClass, table, false);
                    createCommand = tableManager.makeCreateCommand(table, options);
                } else {
                    tableFinalName = table.getName();
                }
            }

            for (DatabaseMappingAttribute attributeMapping : containerMapping.getAttributeMappings(monitor)) {
                if (attributeMapping.getSource() != null && attributeMapping.getTarget() != null) {
                    targetAttributesByMapping.put(attributeMapping, attributeMapping.getTarget());
                }
            }
            if (attributeManager != null) {
                for (DatabaseMappingAttribute attributeMapping : containerMapping.getAttributeMappings(monitor)) {
                    if (attributeMapping.getMappingType() != DatabaseMappingType.create) {
                        continue;
                    }
                    DBSEntityAttribute newAttribute = attributeManager.createNewObject(monitor, commandContext, table, null, options);
                    if (!(newAttribute instanceof DBPNamedObject2 namedObject2)) {
                        throw new DBException("Table column name cannot be set for " + attrClass.getName());
                    }
                    namedObject2.setName(getTransformedName(newAttribute.getDataSource(), attributeMapping.getTargetName(), false));

                    // Set attribute properties
                    if (newAttribute instanceof DBSTypedObjectExt2 typedAttr) {
                        boolean typeModifiersSet = false;
                        if (typedAttr instanceof DBSTypedObjectExt3 typedObjectExt3) {
                            String fullTargetTypeName = attributeMapping.getTargetType(executionContext.getDataSource(), true);
                            typeModifiersSet = fullTargetTypeName.contains("(");
                            typedObjectExt3.setFullTypeName(fullTargetTypeName);
                        } else {
                            String targetAttrType = attributeMapping.getTargetType(executionContext.getDataSource(), false);
                            typedAttr.setTypeName(targetAttrType);
                        }

                        if (!typeModifiersSet) {
                            DBSAttributeBase sourceAttr = attributeMapping.getSource();
                            if (sourceAttr != null) {
                                typedAttr.setMaxLength(sourceAttr.getMaxLength());
                                typedAttr.setPrecision(sourceAttr.getPrecision());
                                typedAttr.setScale(sourceAttr.getScale());
                                typedAttr.setRequired(sourceAttr.isRequired());
                            }
                        }
                    }

                    SQLObjectEditor.ObjectCreateCommand attrCreateCommand = attributeManager.makeCreateCommand(newAttribute, options);
                    if (createCommand instanceof DBECommandAggregator<?> aggregator) {
                        aggregator.aggregateCommand(attrCreateCommand);
                    }
                    targetAttributesByMapping.put(attributeMapping, newAttribute);
                }
            }

            containerMapping.setTargetName(tableFinalName);
            List<DBEPersistAction> standaloneConstraintActions = migrateStructConstraints(
                monitor,
                childTypes,
                commandContext,
                executionContext,
                options,
                containerMapping,
                table,
                targetAttributesByMapping,
                consumerSettings
            );
            actions.addAll(
                DBExecUtils.getActionsListFromCommandContext(
                    monitor,
                    commandContext,
                    executionContext,
                    options,
                    null));
            actions.addAll(standaloneConstraintActions);
            return table;
        } catch (DBException e) {
            throw new DBException("Can't create or modify target table", e);
        }
    }

    private static boolean shouldMigratePrimaryKeys(@Nullable DatabaseConsumerSettings consumerSettings) {
        return consumerSettings == null || consumerSettings.isMigratePrimaryKeys();
    }

    @NotNull
    private static List<DBEPersistAction> migrateStructConstraints(
        @NotNull DBRProgressMonitor monitor,
        @Nullable Class<?>[] childTypes,
        @NotNull DBECommandContext commandContext,
        @NotNull DBCExecutionContext executionContext,
        @NotNull Map<String, Object> options,
        @NotNull DatabaseMappingContainer containerMapping,
        @NotNull DBSEntity table,
        @NotNull Map<DatabaseMappingAttribute, DBSEntityAttribute> targetAttributesByMapping,
        @Nullable DatabaseConsumerSettings consumerSettings
    ) throws DBException {
        List<DBEPersistAction> result = new ArrayList<>();
        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();
        SQLConstraintManager<?, ?> constraintManager = getConstraintManager(editorsRegistry, childTypes);

        for (DatabaseMappingConstraint constraintMapping : containerMapping.getConstraintMappings(monitor)) {
            if (!shouldMigrateTableConstraint(constraintMapping, consumerSettings)) {
                continue;
            }

            List<DBSTableColumn> targetColumns = new ArrayList<>(constraintMapping.getAttributeMappings().size());
            for (DatabaseMappingConstraintAttribute attributeMapping : constraintMapping.getAttributeMappings()) {
                DatabaseMappingAttribute targetAttributeMapping = attributeMapping.getTargetAttributeMapping();
                if (targetAttributesByMapping.get(targetAttributeMapping) instanceof DBSTableColumn targetColumn) {
                    targetColumns.add(targetColumn);
                }
            }
            if (targetColumns.isEmpty()) {
                continue;
            }

            String constraintName = DBObjectNameCaseTransformer.transformName(
                table.getDataSource(),
                CommonUtils.isEmpty(constraintMapping.getTargetName()) ?
                    table.getName() + "_" + constraintMapping.getConstraintType().getId().toUpperCase(Locale.ROOT) :
                    constraintMapping.getTargetName()
            );
            if (constraintManager != null && constraintManager.canCreateObject(table)) {
                DBSObject targetObject = constraintManager.createNewObject(monitor, commandContext, table, null, options);
                if (!(targetObject instanceof AbstractTableConstraint targetConstraint)) {
                    throw new DBException("Constraint manager returned unsupported object");
                }
                targetConstraint.setConstraintType(constraintMapping.getConstraintType());
                targetConstraint.setName(constraintName);
                for (DBSTableColumn targetColumn : targetColumns) {
                    targetConstraint.addAttributeReference(targetColumn);
                }
                constraintMapping.setTarget(targetConstraint);
            } else {
                StringBuilder sql = new StringBuilder(100);
                sql.append("ALTER TABLE ")
                    .append(DBUtils.getEntityScriptName(table, options))
                    .append(" ADD CONSTRAINT ")
                    .append(DBUtils.getQuotedIdentifier(executionContext.getDataSource(), constraintName))
                    .append(" PRIMARY KEY (");
                for (int i = 0; i < targetColumns.size(); i++) {
                    if (i > 0) {
                        sql.append(",");
                    }
                    sql.append(DBUtils.getQuotedIdentifier(targetColumns.get(i)));
                }
                sql.append(")");
                result.add(new SQLDatabasePersistAction("Create primary key", sql.toString()));
            }
        }
        return result;
    }

    private static boolean shouldMigrateTableConstraint(
        @NotNull DatabaseMappingConstraint constraintMapping,
        @Nullable DatabaseConsumerSettings consumerSettings
    ) {
        return constraintMapping.getMappingType() == DatabaseMappingType.create &&
            constraintMapping.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY &&
            shouldMigratePrimaryKeys(consumerSettings);
    }

    static boolean shouldMigrateForeignKey(
        @NotNull DatabaseMappingConstraint constraintMapping,
        @Nullable DatabaseConsumerSettings consumerSettings
    ) {
        return consumerSettings != null &&
            consumerSettings.isMigrateForeignKeys() &&
            constraintMapping.getMappingType() == DatabaseMappingType.create &&
            constraintMapping.getConstraintType() == DBSEntityConstraintType.FOREIGN_KEY;
    }

    @NotNull
    public static DBEPersistAction[] generateTargetForeignKeysDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DatabaseConsumerSettings consumerSettings
    ) throws DBException {
        if (!consumerSettings.isMigrateForeignKeys()) {
            return new DBEPersistAction[0];
        }

        List<DBEPersistAction> actions = new ArrayList<>();
        for (DatabaseMappingContainer containerMapping : consumerSettings.getDataMappings().values()) {
            DBSEntity targetEntity = getTargetEntityForMapping(monitor, consumerSettings, containerMapping);
            if (containerMapping.getMappingType() == DatabaseMappingType.skip || targetEntity == null) {
                continue;
            }
            for (DatabaseMappingConstraint constraintMapping : containerMapping.getConstraintMappings(monitor)) {
                if (!shouldMigrateForeignKey(constraintMapping, consumerSettings)) {
                    continue;
                }
                ForeignKeyMigrationInfo migrationInfo = resolveForeignKeyMigrationInfo(
                    monitor,
                    consumerSettings,
                    containerMapping,
                    constraintMapping,
                    targetEntity);
                if (migrationInfo == null) {
                    constraintMapping.setMappingType(DatabaseMappingType.skip);
                    continue;
                }

                List<DBEPersistAction> managerActions = generateForeignKeyWithManager(
                    monitor,
                    executionContext,
                    migrationInfo);
                if (managerActions == null) {
                    actions.add(generateForeignKeySQL(executionContext.getDataSource(), migrationInfo));
                } else {
                    actions.addAll(managerActions);
                }
            }
        }
        return actions.toArray(DBEPersistAction[]::new);
    }

    @Nullable
    private static ForeignKeyMigrationInfo resolveForeignKeyMigrationInfo(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseConsumerSettings consumerSettings,
        @NotNull DatabaseMappingContainer containerMapping,
        @NotNull DatabaseMappingConstraint constraintMapping,
        @NotNull DBSEntity targetEntity
    ) throws DBException {
        if (!(constraintMapping.getSource() instanceof DBSEntityAssociation sourceAssociation)) {
            return null;
        }
        DBSEntity referencedSourceEntity = getReferencedSourceEntity(sourceAssociation);
        if (referencedSourceEntity == null) {
            return null;
        }
        DBSEntity referencedTargetEntity = getReferencedTargetEntity(
            monitor,
            consumerSettings,
            containerMapping,
            referencedSourceEntity);
        if (referencedTargetEntity == null) {
            return null;
        }

        List<DBSEntityAttribute> ownTargetAttributes = getOwnTargetAttributes(constraintMapping);
        List<DBSEntityAttribute> referencedSourceAttributes = getSourceReferencedAttributes(monitor, sourceAssociation);
        if (ownTargetAttributes.isEmpty() || referencedSourceAttributes.isEmpty() ||
            ownTargetAttributes.size() != referencedSourceAttributes.size()) {
            return null;
        }

        List<DBSEntityAttribute> referencedTargetAttributes = getReferencedTargetAttributes(
            monitor,
            consumerSettings,
            containerMapping,
            referencedSourceEntity,
            referencedTargetEntity,
            referencedSourceAttributes);
        if (referencedTargetAttributes.size() != referencedSourceAttributes.size()) {
            return null;
        }

        DBSEntityConstraint referencedPrimaryKey = findPrimaryKey(monitor, referencedTargetEntity, referencedTargetAttributes);
        if (referencedPrimaryKey == null) {
            return null;
        }

        String constraintName = getForeignKeyConstraintName(targetEntity, constraintMapping, referencedTargetEntity);
        DBSForeignKeyModifyRule deleteRule = DBSForeignKeyModifyRule.NO_ACTION;
        DBSForeignKeyModifyRule updateRule = DBSForeignKeyModifyRule.NO_ACTION;
        if (sourceAssociation instanceof DBSTableForeignKey foreignKey) {
            deleteRule = foreignKey.getDeleteRule();
            updateRule = foreignKey.getUpdateRule();
        }
        return new ForeignKeyMigrationInfo(
            constraintMapping,
            targetEntity,
            ownTargetAttributes,
            referencedTargetEntity,
            referencedTargetAttributes,
            referencedPrimaryKey,
            constraintName,
            deleteRule,
            updateRule);
    }

    @Nullable
    private static DBSEntity getReferencedSourceEntity(@NotNull DBSEntityAssociation association) {
        DBSEntityConstraint referencedConstraint = association.getReferencedConstraint();
        if (referencedConstraint != null) {
            return referencedConstraint.getParentObject();
        }
        return association.getAssociatedEntity();
    }

    @NotNull
    private static List<DBSEntityAttribute> getOwnTargetAttributes(@NotNull DatabaseMappingConstraint constraintMapping) {
        List<DBSEntityAttribute> result = new ArrayList<>(constraintMapping.getAttributeMappings().size());
        for (DatabaseMappingConstraintAttribute attributeMapping : constraintMapping.getAttributeMappings()) {
            DBSEntityAttribute targetAttribute = attributeMapping.getTargetAttributeMapping().getTarget();
            if (targetAttribute == null) {
                return Collections.emptyList();
            }
            result.add(targetAttribute);
        }
        return result;
    }

    @NotNull
    private static List<DBSEntityAttribute> getSourceReferencedAttributes(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAssociation association
    ) {
        DBSEntityConstraint referencedConstraint = association.getReferencedConstraint();
        if (referencedConstraint instanceof DBSEntityReferrer referrer) {
            List<DBSEntityAttribute> attributes = DBUtils.getEntityAttributes(monitor, referrer);
            if (!attributes.isEmpty()) {
                return attributes;
            }
        }
        if (association instanceof DBSEntityReferrer referrer) {
            List<DBSEntityAttribute> attributes = new ArrayList<>();
            try {
                for (DBSEntityAttributeRef attributeRef : CommonUtils.safeCollection(referrer.getAttributeReferences(monitor))) {
                    if (attributeRef instanceof DBSTableForeignKeyColumn foreignKeyColumn &&
                        foreignKeyColumn.getReferencedColumn() != null) {
                        attributes.add(foreignKeyColumn.getReferencedColumn());
                    }
                }
            } catch (DBException e) {
                log.debug("Error reading referenced foreign key attributes", e);
                return Collections.emptyList();
            }
            if (!attributes.isEmpty()) {
                return attributes;
            }
        }
        return Collections.emptyList();
    }

    @Nullable
    private static DBSEntity getReferencedTargetEntity(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseConsumerSettings consumerSettings,
        @NotNull DatabaseMappingContainer ownerMapping,
        @NotNull DBSEntity referencedSourceEntity
    ) throws DBException {
        DatabaseMappingContainer referencedMapping = findMappingBySourceEntity(consumerSettings, referencedSourceEntity);
        if (referencedMapping != null &&
            referencedMapping.getMappingType().isValid() &&
            referencedMapping.getMappingType() != DatabaseMappingType.skip) {
            return getTargetEntityForMapping(monitor, consumerSettings, referencedMapping);
        }
        if (CommonUtils.equalObjects(ownerMapping.getSource(), referencedSourceEntity) &&
            ownerMapping.getMappingType().isValid() &&
            ownerMapping.getMappingType() != DatabaseMappingType.skip) {
            return getTargetEntityForMapping(monitor, consumerSettings, ownerMapping);
        }
        return findTargetEntityBySourceName(monitor, consumerSettings, referencedSourceEntity);
    }

    @Nullable
    private static DBSEntity getTargetEntityForMapping(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseConsumerSettings consumerSettings,
        @NotNull DatabaseMappingContainer mapping
    ) throws DBException {
        if (mapping.getTarget() instanceof DBSEntity targetEntity) {
            return targetEntity;
        }
        DBSObjectContainer container = consumerSettings.getContainer();
        if (container == null || CommonUtils.isEmpty(mapping.getTargetName())) {
            return null;
        }
        DBPDataSource dataSource = container.getDataSource();
        String targetName = DBUtils.getUnQuotedIdentifier(dataSource, mapping.getTargetName());
        DBSObject targetObject = container.getChild(monitor, targetName);
        if (targetObject == null && !CommonUtils.equalObjects(targetName, mapping.getTargetName())) {
            targetObject = container.getChild(monitor, mapping.getTargetName());
        }
        if (targetObject instanceof DBSEntity targetEntity && targetObject instanceof DBSDataManipulator dataManipulator) {
            mapping.setTarget(dataManipulator, false);
            return targetEntity;
        }
        return null;
    }

    @NotNull
    private static List<DBSEntityAttribute> getReferencedTargetAttributes(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseConsumerSettings consumerSettings,
        @NotNull DatabaseMappingContainer ownerMapping,
        @NotNull DBSEntity referencedSourceEntity,
        @NotNull DBSEntity referencedTargetEntity,
        @NotNull List<DBSEntityAttribute> referencedSourceAttributes
    ) throws DBException {
        DatabaseMappingContainer referencedMapping = findMappingBySourceEntity(consumerSettings, referencedSourceEntity);
        if (referencedMapping == null && CommonUtils.equalObjects(ownerMapping.getSource(), referencedSourceEntity)) {
            referencedMapping = ownerMapping;
        }

        List<DBSEntityAttribute> result = new ArrayList<>(referencedSourceAttributes.size());
        for (DBSEntityAttribute referencedSourceAttribute : referencedSourceAttributes) {
            DBSEntityAttribute referencedTargetAttribute = null;
            if (referencedMapping != null) {
                DatabaseMappingAttribute attributeMapping = referencedMapping.getAttributeMapping(referencedSourceAttribute);
                referencedTargetAttribute = attributeMapping == null ? null : attributeMapping.getTarget();
            }
            if (referencedTargetAttribute == null) {
                referencedTargetAttribute = findTargetAttributeBySourceName(
                    monitor,
                    referencedTargetEntity,
                    referencedSourceAttribute);
            }
            if (referencedTargetAttribute == null) {
                return Collections.emptyList();
            }
            result.add(referencedTargetAttribute);
        }
        return result;
    }

    @Nullable
    private static DBSEntityConstraint findPrimaryKey(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity entity,
        @NotNull List<DBSEntityAttribute> attributes
    ) throws DBException {
        for (DBSEntityConstraint constraint : CommonUtils.safeCollection(entity.getConstraints(monitor))) {
            if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY &&
                constraint instanceof DBSEntityReferrer referrer &&
                DatabaseMappingConstraint.isSameConstraintAttributes(monitor, referrer, attributes)) {
                return constraint;
            }
        }
        return null;
    }

    static boolean canResolveForeignKeyReferencedPrimaryKey(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseConsumerSettings consumerSettings,
        @NotNull DatabaseMappingContainer ownerMapping,
        @NotNull DBSEntityAssociation association
    ) throws DBException {
        DBSEntity referencedSourceEntity = getReferencedSourceEntity(association);
        if (referencedSourceEntity == null) {
            return false;
        }
        List<DBSEntityAttribute> referencedSourceAttributes = getSourceReferencedAttributes(monitor, association);
        if (referencedSourceAttributes.isEmpty()) {
            return false;
        }

        DatabaseMappingContainer referencedMapping = findMappingBySourceEntity(consumerSettings, referencedSourceEntity);
        if (referencedMapping != null &&
            referencedMapping.getMappingType().isValid() &&
            referencedMapping.getMappingType() != DatabaseMappingType.skip) {
            DBSEntity targetEntity = getTargetEntityForMapping(monitor, consumerSettings, referencedMapping);
            if (targetEntity != null) {
                List<DBSEntityAttribute> targetAttributes = getReferencedTargetAttributes(
                    monitor,
                    consumerSettings,
                    ownerMapping,
                    referencedSourceEntity,
                    targetEntity,
                    referencedSourceAttributes);
                if (!targetAttributes.isEmpty() && findPrimaryKey(monitor, targetEntity, targetAttributes) != null) {
                    return true;
                }
            }
            return consumerSettings.isMigratePrimaryKeys() &&
                sourcePrimaryKeyMatches(monitor, referencedSourceEntity, referencedSourceAttributes) &&
                referencedAttributesAreMapped(referencedMapping, referencedSourceAttributes);
        }

        DBSEntity targetEntity = findTargetEntityBySourceName(monitor, consumerSettings, referencedSourceEntity);
        if (targetEntity == null) {
            return false;
        }
        List<DBSEntityAttribute> targetAttributes = getReferencedTargetAttributes(
            monitor,
            consumerSettings,
            ownerMapping,
            referencedSourceEntity,
            targetEntity,
            referencedSourceAttributes);
        return !targetAttributes.isEmpty() && findPrimaryKey(monitor, targetEntity, targetAttributes) != null;
    }

    private static boolean sourcePrimaryKeyMatches(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity referencedSourceEntity,
        @NotNull List<DBSEntityAttribute> referencedSourceAttributes
    ) throws DBException {
        for (DBSEntityConstraint constraint : CommonUtils.safeCollection(referencedSourceEntity.getConstraints(monitor))) {
            if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY &&
                constraint instanceof DBSEntityReferrer referrer &&
                DatabaseMappingConstraint.isSameConstraintAttributes(monitor, referrer, referencedSourceAttributes)) {
                return true;
            }
        }
        return false;
    }

    private static boolean referencedAttributesAreMapped(
        @NotNull DatabaseMappingContainer referencedMapping,
        @NotNull List<DBSEntityAttribute> referencedSourceAttributes
    ) {
        for (DBSEntityAttribute sourceAttribute : referencedSourceAttributes) {
            DatabaseMappingAttribute attributeMapping = referencedMapping.getAttributeMapping(sourceAttribute);
            if (attributeMapping == null || attributeMapping.getMappingType() == DatabaseMappingType.skip) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static DatabaseMappingContainer findMappingBySourceEntity(
        @NotNull DatabaseConsumerSettings consumerSettings,
        @NotNull DBSEntity sourceEntity
    ) {
        for (DatabaseMappingContainer mapping : consumerSettings.getDataMappings().values()) {
            if (CommonUtils.equalObjects(mapping.getSource(), sourceEntity)) {
                return mapping;
            }
        }
        return null;
    }

    @Nullable
    private static List<DBEPersistAction> generateForeignKeyWithManager(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull ForeignKeyMigrationInfo migrationInfo
    ) throws DBException {
        SQLForeignKeyManager foreignKeyManager = getForeignKeyManager(migrationInfo.targetEntity);
        if (foreignKeyManager == null) {
            return null;
        }
        if (!foreignKeyManager.canCreateObject(migrationInfo.targetEntity)) {
            migrationInfo.constraintMapping.setMappingType(DatabaseMappingType.skip);
            return Collections.emptyList();
        }

        Map<String, Object> options = new HashMap<>();
        options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        options.put(SQLForeignKeyManager.OPTION_REF_TABLE, migrationInfo.referencedEntity);
        options.put(SQLForeignKeyManager.OPTION_REF_CONSTRAINT, migrationInfo.referencedPrimaryKey);
        options.put(SQLForeignKeyManager.OPTION_REF_ATTRIBUTES, migrationInfo.referencedAttributes);
        options.put(SQLForeignKeyManager.OPTION_OWN_ATTRIBUTES, migrationInfo.ownAttributes);

        DBECommandContext commandContext = new TargetCommandContext(executionContext);
        DBSObject foreignKeyObject = foreignKeyManager.createNewObject(
            monitor,
            commandContext,
            migrationInfo.targetEntity,
            null,
            options);
        if (!(foreignKeyObject instanceof AbstractTableConstraint targetConstraint) ||
            !(foreignKeyObject instanceof DBSTableForeignKey targetForeignKey)) {
            return null;
        }

        targetConstraint.setName(migrationInfo.constraintName);
        targetConstraint.setConstraintType(DBSEntityConstraintType.FOREIGN_KEY);

        setReferencedConstraint(targetForeignKey, migrationInfo.referencedPrimaryKey);
        targetForeignKey.setDeleteRule(migrationInfo.deleteRule);
        targetForeignKey.setUpdateRule(migrationInfo.updateRule);

        if (CommonUtils.isEmpty(targetForeignKey.getAttributeReferences(monitor))) {
            return null;
        }
        migrationInfo.constraintMapping.setTarget(targetConstraint);
        return DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            options,
            null);
    }

    private static void setReferencedConstraint(
        @NotNull DBSTableForeignKey foreignKey,
        @NotNull DBSEntityConstraint referencedConstraint
    ) {
        for (Method method : foreignKey.getClass().getMethods()) {
            if (!method.getName().equals("setReferencedConstraint") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            if (!parameterType.isInstance(referencedConstraint)) {
                continue;
            }
            try {
                method.invoke(foreignKey, referencedConstraint);
            } catch (ReflectiveOperationException e) {
                log.debug("Error setting foreign key referenced constraint", e);
            }
            return;
        }
    }

    @Nullable
    private static SQLForeignKeyManager getForeignKeyManager(@NotNull DBSEntity targetEntity) {
        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();
        SQLObjectEditor tableManager = editorsRegistry.getObjectManager(targetEntity.getClass(), SQLObjectEditor.class);
        if (!(tableManager instanceof DBEStructEditor<?> structEditor)) {
            return null;
        }
        Class<? extends DBSTableForeignKey> foreignKeyClass = BeanUtils.findAssignableType(
            structEditor.getChildTypes(),
            DBSTableForeignKey.class);
        return foreignKeyClass == null ? null :
            editorsRegistry.getObjectManager(foreignKeyClass, SQLForeignKeyManager.class);
    }

    @NotNull
    private static DBEPersistAction generateForeignKeySQL(
        @NotNull DBPDataSource dataSource,
        @NotNull ForeignKeyMigrationInfo migrationInfo
    ) {
        Map<String, Object> options = Collections.emptyMap();
        StringBuilder sql = new StringBuilder(150);
        sql.append("ALTER TABLE ")
            .append(DBUtils.getEntityScriptName(migrationInfo.targetEntity, options))
            .append(" ADD CONSTRAINT ")
            .append(DBUtils.getQuotedIdentifier(dataSource, migrationInfo.constraintName))
            .append(" FOREIGN KEY (");
        appendAttributeList(sql, migrationInfo.ownAttributes);
        sql.append(") REFERENCES ")
            .append(DBUtils.getEntityScriptName(migrationInfo.referencedEntity, options))
            .append("(");
        appendAttributeList(sql, migrationInfo.referencedAttributes);
        sql.append(")");
        appendForeignKeyRule(sql, " ON DELETE ", migrationInfo.deleteRule);
        appendForeignKeyRule(sql, " ON UPDATE ", migrationInfo.updateRule);
        return new SQLDatabasePersistAction("Create foreign key", sql.toString());
    }

    private static void appendAttributeList(
        @NotNull StringBuilder sql,
        @NotNull List<DBSEntityAttribute> attributes
    ) {
        for (int i = 0; i < attributes.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append(DBUtils.getQuotedIdentifier(attributes.get(i)));
        }
    }

    private static void appendForeignKeyRule(
        @NotNull StringBuilder sql,
        @NotNull String prefix,
        @Nullable DBSForeignKeyModifyRule rule
    ) {
        if (rule != null && CommonUtils.isNotEmpty(rule.getClause())) {
            sql.append(prefix).append(rule.getClause());
        }
    }

    @NotNull
    private static String getForeignKeyConstraintName(
        @NotNull DBSEntity targetEntity,
        @NotNull DatabaseMappingConstraint constraintMapping,
        @NotNull DBSEntity referencedTargetEntity
    ) {
        String targetName = constraintMapping.getTargetName();
        if (CommonUtils.isEmpty(targetName) || targetName.equals(DatabaseMappingAttribute.TARGET_NAME_SKIP)) {
            targetName = targetEntity.getName() + "_" + referencedTargetEntity.getName() + "_FK";
        }
        return DBObjectNameCaseTransformer.transformName(targetEntity.getDataSource(), targetName);
    }

    private static class SimpleEntityAttributeRef implements DBSEntityAttributeRef {
        @NotNull
        private final DBSEntityAttribute attribute;

        private SimpleEntityAttributeRef(@NotNull DBSEntityAttribute attribute) {
            this.attribute = attribute;
        }

        @NotNull
        @Override
        public DBSEntityAttribute getAttribute() {
            return attribute;
        }
    }

    private record ForeignKeyMigrationInfo(@NotNull DatabaseMappingConstraint constraintMapping,
        @NotNull DBSEntity targetEntity,
        @NotNull List<DBSEntityAttribute> ownAttributes,
        @NotNull DBSEntity referencedEntity,
        @NotNull List<DBSEntityAttribute> referencedAttributes,
        @NotNull DBSEntityConstraint referencedPrimaryKey,
        @NotNull String constraintName,
        @NotNull DBSForeignKeyModifyRule deleteRule,
        @NotNull DBSForeignKeyModifyRule updateRule) {
    }


    @Nullable
    static DBSEntity findTargetEntityBySourceName(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseConsumerSettings consumerSettings,
        @NotNull DBSEntity sourceEntity
    ) throws DBException {
        DBSObjectContainer container = consumerSettings.getContainer();
        if (container == null) {
            return null;
        }
        DBPDataSource dataSource = container.getDataSource();
        String transformedName = DBUtils.getUnQuotedIdentifier(
            dataSource,
            getTransformedName(dataSource, sourceEntity.getName(), false));
        DBSObject child = container.getChild(monitor, transformedName);
        if (child == null && !CommonUtils.equalObjects(transformedName, sourceEntity.getName())) {
            child = container.getChild(monitor, sourceEntity.getName());
        }
        return child instanceof DBSEntity entity ? entity : null;
    }

    @Nullable
    static DBSEntityAttribute findTargetAttributeBySourceName(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity targetEntity,
        @NotNull DBSEntityAttribute sourceAttribute
    ) throws DBException {
        DBPDataSource dataSource = targetEntity.getDataSource();
        String transformedName = DBUtils.getUnQuotedIdentifier(
            dataSource,
            getTransformedName(dataSource, sourceAttribute.getName(), false));
        DBSEntityAttribute targetAttribute = targetEntity.getAttribute(monitor, transformedName);
        if (targetAttribute == null && !CommonUtils.equalObjects(transformedName, sourceAttribute.getName())) {
            targetAttribute = targetEntity.getAttribute(monitor, sourceAttribute.getName());
        }
        return targetAttribute;
    }

    public static void applyPropertyChanges(
        @Nullable DBRProgressMonitor monitor,
        @Nullable Map<DBPPropertyDescriptor, Object> changedProperties,
        @Nullable DBECommandContext commandContext,
        @Nullable DatabaseMappingContainer containerMapping,
        @NotNull DBSEntity table
    ) {
        PropertySourceEditable propertySource = new PropertySourceEditable(commandContext, table, table);
        if (CommonUtils.isEmpty(changedProperties) && containerMapping != null
            && !CommonUtils.isEmpty(containerMapping.getRawChangedPropertiesMap())) {
            // Probably it is the task with saved properties map
            // But this map has only the id of ObjectPropertyDescriptor
            // So we should find the correct properties and bound them
            propertySource.collectProperties();
            Map<String, Object> rawChangedPropertiesMap = containerMapping.getRawChangedPropertiesMap();
            for (Map.Entry<String, Object> entry : rawChangedPropertiesMap.entrySet()) {
                DBPPropertyDescriptor property = propertySource.getProperty(entry.getKey());
                if (property != null) {
                    propertySource.addChangedProperties(property, entry.getValue());
                }
            }
            changedProperties = propertySource.getChangedPropertiesValues();
        }
        if (!CommonUtils.isEmpty(changedProperties)) {
            for (Map.Entry<DBPPropertyDescriptor, Object> entry : changedProperties.entrySet()) {
                propertySource.setPropertyValue(monitor, table, (ObjectPropertyDescriptor) entry.getKey(), entry.getValue());
            }
        }
    }

    @NotNull
    private static String getTableFinalName(
        @NotNull String targetName,
        @NotNull Class<? extends DBSObject> tableClass,
        @Nullable DBSEntity table,
        boolean extraTransform
    ) throws DBException {
        if (table == null) {
            throw new DBException("Internal error - target table not set");
        }
        if (table.getDataSource() == null) {
            return targetName;
        }
        String tableFinalName;
        if (extraTransform) {
            tableFinalName = getTransformedName(table.getDataSource(), targetName, false);
        } else {
            tableFinalName = DBObjectNameCaseTransformer.transformName(table.getDataSource(), targetName);
        }
        if (table instanceof DBPNamedObject2 namedObject2 && tableFinalName != null) {
            namedObject2.setName(tableFinalName);
        } else {
            throw new DBException("Table name cannot be set for " + tableClass.getName());
        }
        return tableFinalName;
    }

    @NotNull
    static DBEPersistAction generateTargetAttributeDDL(@NotNull DBPDataSource dataSource, @NotNull DatabaseMappingAttribute attribute) {
        StringBuilder sql = new StringBuilder(500);
        sql.append("ALTER TABLE ").append(DBUtils.getObjectFullName(attribute.getParent().getTarget(), DBPEvaluationContext.DDL))
            .append(" ADD ");
        appendAttributeClause(dataSource, sql, attribute);
        return new SQLDatabasePersistAction(sql.toString());
    }

    private static void appendAttributeClause(
        @NotNull DBPDataSource dataSource,
        @NotNull StringBuilder sql,
        @NotNull DatabaseMappingAttribute attr
    ) {
        String attrName = getTransformedName(dataSource, attr.getTargetName(), false);
        sql.append(DBUtils.getQuotedIdentifier(dataSource, attrName)).append(" ").append(attr.getTargetType(dataSource, true));
        if (SQLUtils.getDialectFromDataSource(dataSource).supportsNullability()) {
            if (attr.getSource() != null && attr.getSource().isRequired()) {
                sql.append(" NOT NULL");
            }
        }
    }

    public static void executeDDL(@NotNull DBCSession session, @NotNull DBEPersistAction[] actions) throws DBException {
        if (actions.length == 0) {
            return;
        }
        ensureHasEditMetadataPermission(session.getDataSource().getContainer());
        // Process actions
        DBExecUtils.executePersistActions(session, actions);
        // Commit DDL changes
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
        if (txnManager != null && txnManager.isSupportsTransactions() && !txnManager.isAutoCommit()) {
            txnManager.commit(session);
        }
    }

    static void createTargetDynamicTable(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer schema,
        @NotNull DatabaseMappingContainer containerMapping,
        boolean recreate
    ) throws DBException {
        ensureHasEditMetadataPermission(executionContext.getDataSource().getContainer());
        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();

        Class<? extends DBSObject> tableClass = getTableClass(monitor, schema);
        SQLObjectEditor tableManager = getTableManager(editorsRegistry, tableClass);
        DBECommandContext commandContext = new TargetCommandContext(executionContext);
        Map<String, Object> options = new HashMap<>();
        options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        if (recreate && containerMapping.getTarget() != null) {
            tableManager.deleteObject(commandContext, containerMapping.getTarget(), options);
            commandContext.saveChanges(monitor, options);
        }
        DBSObject targetEntity = tableManager.createNewObject(monitor, commandContext, schema, null, options);
        if (targetEntity == null) {
            throw new DBException("Null target entity returned");
        }
        if (targetEntity instanceof DBPNamedObject2) {
            ((DBPNamedObject2) targetEntity).setName(containerMapping.getTargetName());
        } else {
            throw new DBException("Can not set name for target entity '" + targetEntity.getClass().getName() + "'");
        }
        commandContext.saveChanges(monitor, options);
    }

    @NotNull
    public static Pair<DBPDataKind, String> getDataType(@Nullable String value) {
        if (CommonUtils.isEmpty(value)) {
            return DATA_TYPE_UNKNOWN;
        }

        char firstChar = value.charAt(0);
        if (isNumericStart(firstChar)) {
            Pair<DBPDataKind, String> numeric = tryClassifyNumber(value);
            if (numeric != null) {
                return numeric;
            }
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return DATA_TYPE_BOOLEAN;
        }
        if (!SQLUtils.isLatinLetter(firstChar)) {
            return DATA_TYPE_NATIONAL_STRING;
        }
        return DATA_TYPE_STRING;
    }

    private static boolean isNumericStart(char c) {
        return Character.isDigit(c) || c == '+' || c == '-' || c == '.';
    }

    @Nullable
    private static Pair<DBPDataKind, String> tryClassifyNumber(@NotNull String value) {
        try {
            Integer.parseInt(value);
            return DATA_TYPE_INTEGER;
        } catch (NumberFormatException ignore) {
        }
        try {
            Long.parseLong(value);
            return DATA_TYPE_BIGINT;
        } catch (NumberFormatException ignore) {
        }
        try {
            Double.parseDouble(value);
            return DATA_TYPE_REAL;
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private static void ensureHasEditMetadataPermission(@NotNull DBPDataSourceContainer container) throws DBCException {
        if (!container.hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_METADATA)) {
            throw new DBCException("New table creation in database [" + container.getName() + "] restricted by connection configuration");
        }
    }

    static class TargetCommandContext extends AbstractCommandContext {
        TargetCommandContext(DBCExecutionContext executionContext) {
            super(executionContext, true);
        }
    }
}
