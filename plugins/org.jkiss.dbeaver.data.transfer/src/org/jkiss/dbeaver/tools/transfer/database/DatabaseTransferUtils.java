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
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.*;

/**
* DatabaseTransferUtils
*/
public class DatabaseTransferUtils {

    private static final Log log = Log.getLog(DatabaseTransferUtils.class);

    private static final boolean USE_STRUCT_DDL = true;

    private static final Pair<DBPDataKind, String> DATA_TYPE_UNKNOWN = new Pair<>(DBPDataKind.UNKNOWN, null);
    private static final Pair<DBPDataKind, String> DATA_TYPE_INTEGER = new Pair<>(DBPDataKind.NUMERIC, "INTEGER");
    private static final Pair<DBPDataKind, String> DATA_TYPE_REAL = new Pair<>(DBPDataKind.NUMERIC, "REAL");
    private static final Pair<DBPDataKind, String> DATA_TYPE_BOOLEAN = new Pair<>(DBPDataKind.BOOLEAN, "BOOLEAN");
    private static final Pair<DBPDataKind, String> DATA_TYPE_STRING = new Pair<>(DBPDataKind.STRING, "VARCHAR");

    public static void refreshDatabaseModel(DBRProgressMonitor monitor, DatabaseConsumerSettings consumerSettings, DatabaseMappingContainer containerMapping) throws DBException {
        monitor.subTask("Refresh database model");
        DBSObjectContainer container = consumerSettings.getContainer();
        DBNModel navigatorModel = DBNUtils.getNavigatorModel(container);
        if (navigatorModel != null) {
            var containerNode = navigatorModel.getNodeByObject(monitor, container, false);
            if (containerNode != null) {
                containerNode.refreshNode(monitor, containerMapping);
            }
        } else if (container instanceof DBPRefreshableObject) {
            ((DBPRefreshableObject) container).refreshObject(monitor);
        }
        refreshDatabaseMappings(monitor, consumerSettings, containerMapping, false);
    }

    public static void refreshDatabaseMappings(@NotNull DBRProgressMonitor monitor, @NotNull DatabaseConsumerSettings consumerSettings, @NotNull DatabaseMappingContainer containerMapping, boolean force) throws DBException {
        DBSObjectContainer container = consumerSettings.getContainer();
        if (container == null) {
            throw new DBException("Null target container");
        }

        // Reflect database changes in mappings
        {
            monitor.subTask("Refresh database mappings");

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
                DBSObject newTarget = container.getChild(monitor, DBUtils.getUnQuotedIdentifier(container.getDataSource(), containerMapping.getTargetName()));
                if (newTarget == null) {
                    throw new DBCException("New table " + containerMapping.getTargetName() + " not found in container " + DBUtils.getObjectFullName(container, DBPEvaluationContext.UI));
                } else if (!(newTarget instanceof DBSDataManipulator)) {
                    throw new DBCException("New table " + DBUtils.getObjectFullName(newTarget, DBPEvaluationContext.UI) + " doesn't support data manipulation");
                }
                containerMapping.setTarget((DBSDataManipulator) newTarget);
                if (containerMapping.getMappingType() == DatabaseMappingType.create) {
                    containerMapping.setMappingType(DatabaseMappingType.existing);
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
    public static DBEPersistAction[] generateTargetTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer schema,
        @NotNull DatabaseMappingContainer containerMapping,
        @Nullable Map<DBPPropertyDescriptor, Object> changedProperties) throws DBException
    {
        if (containerMapping.getMappingType() == DatabaseMappingType.skip) {
            return new DBEPersistAction[0];
        }
        // Check whether we have any changes in mappings
        if (containerMapping.getMappingType() == DatabaseMappingType.existing) {
            boolean hasChanges = false;
            for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                if (attr.getMappingType() != DatabaseMappingType.existing &&
                    attr.getMappingType() != DatabaseMappingType.skip) {
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
                generateStructTableDDL(monitor, executionContext, schema, containerMapping, actions, changedProperties);
                return actions.toArray(DBEPersistAction[]::new);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Can't create or update target table", null, e);
                if (!DBWorkbench.getPlatformUI().confirmAction(
                    "Generate DDL automatically",
                    "Do you want to create or update target object with auto-generated SQL script?"))
                {
                    throw new DBException("Target table create or update was canceled");
                }
            }
        }

        // Struct doesn't work (no proper object managers?)
        // Try plain SQL mode

        DBPDataSource dataSource = executionContext.getDataSource();
        StringBuilder sql = new StringBuilder(500);

        String tableName = DBObjectNameCaseTransformer.transformName(dataSource, containerMapping.getTargetName());
        containerMapping.setTargetName(tableName);

        if (CommonUtils.isEmpty(tableName)) {
            return new DBEPersistAction[0];
        }

        List<DBEPersistAction> actions = new ArrayList<>();

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
                if (!mappedAttrs.isEmpty()) sql.append(",\n");
                sql.append("\t");
                appendAttributeClause(dataSource, sql, attr);
                mappedAttrs.put(attr.getSource(), attr);
            }
            if (containerMapping.getSource() instanceof DBSEntity) {
                // Make primary key
                Collection<? extends DBSEntityAttribute> identifier = DBUtils.getBestTableIdentifier(monitor, (DBSEntity) containerMapping.getSource());
                if (!CommonUtils.isEmpty(identifier)) {
                    boolean idMapped = true;
                    for (DBSEntityAttribute idAttr : identifier) {
                        if (!mappedAttrs.containsKey(idAttr)) {
                            idMapped = false;
                            break;
                        }
                    }
                    if (idMapped) {
                        sql.append(",\n\tPRIMARY KEY (");
                        boolean hasAttr = false;
                        for (DBSEntityAttribute idAttr : identifier) {
                            DatabaseMappingAttribute mappedAttr = mappedAttrs.get(idAttr);
                            if (hasAttr) sql.append(",");
                            sql.append(DBUtils.getQuotedIdentifier(dataSource, mappedAttr.getTargetName()));
                            hasAttr = true;
                        }
                        sql.append(")\n");
                    }
                }
            }
            sql.append(")");
            actions.add(new SQLDatabasePersistAction("Table DDL", sql.toString()));
        } else {
            for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                if (attr.getMappingType() == DatabaseMappingType.create) {
                    actions.add(generateTargetAttributeDDL(dataSource, attr));
                }
            }
        }
        return actions.toArray(new DBEPersistAction[0]);
    }

    private static void getTableFullName(@Nullable DBSObjectContainer schema, @NotNull DBPDataSource dataSource, @NotNull StringBuilder sql, @NotNull String tableName) {
        if (schema instanceof DBSSchema || schema instanceof DBSCatalog) {
            sql.append(DBUtils.getFullyQualifiedName(dataSource, schema.getName(), tableName));
        } else {
            sql.append(DBUtils.getQuotedIdentifier(dataSource, tableName));
        }
    }

    @NotNull
    private static SQLObjectEditor<DBSEntity, ?> getTableManager(DBERegistry editorsRegistry, Class<? extends DBSObject> tableClass)
        throws DBException {
        SQLObjectEditor<DBSEntity, ?> tableManager = editorsRegistry.getObjectManager(tableClass, SQLObjectEditor.class);
        if (tableManager == null) {
            throw new DBException("Table manager not found for '" + tableClass.getName() + "'");
        }
        return tableManager;
    }

    @NotNull
    private static Class<? extends DBSObject> getTableClass(DBRProgressMonitor monitor, DBSObjectContainer schema) throws DBException {
        Class<? extends DBSObject> tableClass = schema.getPrimaryChildType(monitor);
        if (!DBSEntity.class.isAssignableFrom(tableClass)) {
            throw new DBException("Wrong table container child type: " + tableClass.getName());
        }
        return tableClass;
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
        @Nullable Map<DBPPropertyDescriptor, Object> changedProperties
    ) throws DBException {
        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();

        try {
            Class<? extends DBSObject> tableClass = getTableClass(monitor, schema);
            SQLObjectEditor<DBSEntity, ?> tableManager = getTableManager(editorsRegistry, tableClass);
            if (!tableManager.canCreateObject(schema)) {
                throw new DBException("Table create is not supported by driver " + schema.getDataSource().getContainer().getDriver().getName());
            }
            Class<? extends DBSEntityAttribute> attrClass;
            SQLObjectEditor<DBSEntityAttribute,?> attributeManager;
            if (executionContext.getDataSource().getInfo().isDynamicMetadata()) {
                attrClass = null;
                attributeManager = null;
            } else {
                if (!(tableManager instanceof DBEStructEditor)) {
                    throw new DBException("Table create not supported by " + executionContext.getDataSource().getContainer().getDriver().getName());
                }
                Class<?>[] childTypes = ((DBEStructEditor<?>) tableManager).getChildTypes();
                attrClass = BeanUtils.findAssignableType(childTypes, DBSEntityAttribute.class);
                if (attrClass == null) {
                    throw new DBException("Column manager not found for '" + tableClass.getName() + "'");
                }
                attributeManager = editorsRegistry.getObjectManager(attrClass, SQLObjectEditor.class);
            }

            Map<String, Object> options = new HashMap<>();
            options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);

            DBECommandContext commandContext = new TargetCommandContext(executionContext);

            String tableFinalName;

            DBSEntity table;
            DBECommand createCommand = null;
            if (containerMapping.getMappingType() == DatabaseMappingType.create ||
                (containerMapping.getMappingType() == DatabaseMappingType.recreate
                    && containerMapping.getTarget() == null))
            {
                table = tableManager.createNewObject(monitor, commandContext, schema, null, options);
                applyPropertyChanges(monitor, changedProperties, commandContext, containerMapping, table);
                tableFinalName = getTableFinalName(containerMapping.getTargetName(), tableClass, table);
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
                    tableFinalName = getTableFinalName(containerMapping.getTargetName(), tableClass, table);
                    createCommand = tableManager.makeCreateCommand(table, options);
                } else {
                    tableFinalName = table.getName();
                }
            }

            if (attributeManager != null) {
                for (DatabaseMappingAttribute attributeMapping : containerMapping.getAttributeMappings(monitor)) {
                    if (attributeMapping.getMappingType() != DatabaseMappingType.create) {
                        continue;
                    }
                    DBSEntityAttribute newAttribute = attributeManager.createNewObject(monitor, commandContext, table, null, options);
                    if (!(newAttribute instanceof DBPNamedObject2)) {
                        throw new DBException("Table column name cannot be set for " + attrClass.getName());
                    }
                    ((DBPNamedObject2) newAttribute).setName(
                        DBObjectNameCaseTransformer.transformName(newAttribute.getDataSource(),
                            attributeMapping.getTargetName()));

                    // Set attribute properties
                    if (newAttribute instanceof DBSTypedObjectExt2) {
                        DBSTypedObjectExt2 typedAttr = (DBSTypedObjectExt2) newAttribute;

                        boolean typeModifiersSet = false;
                        if (typedAttr instanceof DBSTypedObjectExt3) {
                            String fullTargetTypeName = attributeMapping.getTargetType(executionContext.getDataSource(), true);
                            typeModifiersSet = fullTargetTypeName.contains("(");
                            ((DBSTypedObjectExt3) typedAttr).setFullTypeName(fullTargetTypeName);
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
                    if (createCommand instanceof DBECommandAggregator) {
                        ((DBECommandAggregator)createCommand).aggregateCommand(attrCreateCommand);
                    }
                }
            }

            containerMapping.setTargetName(tableFinalName);

            actions.addAll(
                DBExecUtils.getActionsListFromCommandContext(
                    monitor,
                    commandContext,
                    executionContext,
                    options,
                    null));
            return table;
        } catch (DBException e) {
            throw new DBException("Can't create or modify target table", e);
        }
    }

    public static void applyPropertyChanges(
        @Nullable DBRProgressMonitor monitor,
        @Nullable Map<DBPPropertyDescriptor, Object> changedProperties,
        @Nullable DBECommandContext commandContext,
        @Nullable DatabaseMappingContainer containerMapping,
        @NotNull DBSEntity table)
    {
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

    private static String getTableFinalName(String targetName, @NotNull Class<? extends DBSObject> tableClass, DBSEntity table) throws DBException {
        if (table == null) {
            throw new DBException("Internal error - target table not set");
        }
        String tableFinalName = DBObjectNameCaseTransformer.transformName(table.getDataSource(), targetName);
        if (table instanceof DBPNamedObject2) {
            ((DBPNamedObject2) table).setName(tableFinalName);
        } else {
            throw new DBException("Table name cannot be set for " + tableClass.getName());
        }
        return tableFinalName;
    }

    @NotNull
    static DBEPersistAction generateTargetAttributeDDL(DBPDataSource dataSource, DatabaseMappingAttribute attribute) {
        StringBuilder sql = new StringBuilder(500);
        sql.append("ALTER TABLE ").append(DBUtils.getObjectFullName(attribute.getParent().getTarget(), DBPEvaluationContext.DDL))
            .append(" ADD ");
        appendAttributeClause(dataSource, sql, attribute);
        return new SQLDatabasePersistAction(sql.toString());
    }

    private static void appendAttributeClause(DBPDataSource dataSource, StringBuilder sql, DatabaseMappingAttribute attr) {
        String attrName = DBObjectNameCaseTransformer.transformName(dataSource, attr.getTargetName());
        sql.append(DBUtils.getQuotedIdentifier(dataSource, attrName)).append(" ").append(attr.getTargetType(dataSource, true));
        if (SQLUtils.getDialectFromDataSource(dataSource).supportsNullability()) {
            if (attr.getSource().isRequired()) sql.append(" NOT NULL");
        }
    }

    public static void executeDDL(DBCSession session, DBEPersistAction[] actions) throws DBCException {
        // Process actions
        DBExecUtils.executePersistActions(session, actions);
        // Commit DDL changes
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
        if (txnManager != null && txnManager.isSupportsTransactions() && !txnManager.isAutoCommit()) {
            txnManager.commit(session);
        }
    }

    static void createTargetDynamicTable(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull DBSObjectContainer schema, @NotNull DatabaseMappingContainer containerMapping, boolean recreate) throws DBException {
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

    public static Pair<DBPDataKind, String> getDataType(String value) {
        if (CommonUtils.isEmpty(value)) {
            return DATA_TYPE_UNKNOWN;
        }
        char firstChar = value.charAt(0);
        if (Character.isDigit(firstChar) || firstChar == '+' || firstChar == '-' || firstChar == '.') {
            try {
                Long.parseLong(value);
                return DATA_TYPE_INTEGER;
            } catch (NumberFormatException ignored) {
            }
            try {
                Double.parseDouble(value);
                return DATA_TYPE_REAL;
            } catch (NumberFormatException ignored) {
            }
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return DATA_TYPE_BOOLEAN;
        }
        return DATA_TYPE_STRING;
    }

    static class TargetCommandContext extends AbstractCommandContext {
        TargetCommandContext(DBCExecutionContext executionContext) {
            super(executionContext, true);
        }
    }
}
