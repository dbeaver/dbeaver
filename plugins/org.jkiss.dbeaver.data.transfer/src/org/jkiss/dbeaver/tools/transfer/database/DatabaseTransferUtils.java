/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.AbstractCommandContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
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
        DBSObjectContainer container = consumerSettings.getContainer();
        {
            monitor.subTask("Refresh navigator model");
            consumerSettings.getContainerNode().refreshNode(monitor, containerMapping);
        }

        // Reflect database changes in mappings
        {
            switch (containerMapping.getMappingType()) {
                case create:
                    DBSObject newTarget = container.getChild(monitor, DBUtils.getUnQuotedIdentifier(container.getDataSource(), containerMapping.getTargetName()));
                    if (newTarget == null) {
                        throw new DBCException("New table " + containerMapping.getTargetName() + " not found in container " + DBUtils.getObjectFullName(container, DBPEvaluationContext.UI));
                    } else if (!(newTarget instanceof DBSDataManipulator)) {
                        throw new DBCException("New table " + DBUtils.getObjectFullName(newTarget, DBPEvaluationContext.UI) + " doesn't support data manipulation");
                    }
                    containerMapping.setTarget((DBSDataManipulator) newTarget);
                    containerMapping.setMappingType(DatabaseMappingType.existing);
                    // ! Fall down is ok here
                case existing:
                    for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                        if (attr.getMappingType() == DatabaseMappingType.create) {
                            attr.updateMappingType(monitor, false);
                            if (attr.getTarget() == null) {
                                log.debug("Can't find target attribute '" + attr.getTargetName() + "' in '" + containerMapping.getTargetName() + "'");
                            }
                        }
                    }
                    break;
            }
        }
    }

    public static DBEPersistAction[] generateTargetTableDDL(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer schema, DatabaseMappingContainer containerMapping) throws DBException {
        if (containerMapping.getMappingType() == DatabaseMappingType.skip) {
            return new DBEPersistAction[0];
        }
        monitor.subTask("Create table '" + containerMapping.getTargetName() + "'");
        if (USE_STRUCT_DDL) {
            DBEPersistAction[] ddl = generateStructTableDDL(monitor, executionContext, schema, containerMapping);
            if (ddl != null) {
                return ddl;
            }
        }

        // Struct doesn't work (no proper object managers?)
        // Try plain SQL mode

        DBPDataSource dataSource = executionContext.getDataSource();
        StringBuilder sql = new StringBuilder(500);

        String tableName = DBObjectNameCaseTransformer.transformName(dataSource, containerMapping.getTargetName());
        containerMapping.setTargetName(tableName);

        List<DBEPersistAction> actions = new ArrayList<>();

        if (containerMapping.getMappingType() == DatabaseMappingType.create) {
            sql.append("CREATE TABLE ");
            if (schema instanceof DBSSchema || schema instanceof DBSCatalog) {
                sql.append(DBUtils.getQuotedIdentifier(schema));
                sql.append(dataSource.getSQLDialect().getCatalogSeparator());
            }
            sql.append(DBUtils.getQuotedIdentifier(dataSource, tableName)).append("(\n");
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

    private static DBEPersistAction[] generateStructTableDDL(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer schema, DatabaseMappingContainer containerMapping) {
        final DBERegistry editorsRegistry = executionContext.getDataSource().getContainer().getPlatform().getEditorsRegistry();

        try {
            Class<? extends DBSObject> tableClass = schema.getPrimaryChildType(monitor);
            if (!DBSEntity.class.isAssignableFrom(tableClass)) {
                throw new DBException("Wrong table container child type: " + tableClass.getName());
            }
            SQLObjectEditor<DBSEntity, ?> tableManager = editorsRegistry.getObjectManager(tableClass, SQLObjectEditor.class);
            if (tableManager == null) {
                throw new DBException("Table manager not found for '" + tableClass.getName() + "'");
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
            if (containerMapping.getMappingType() == DatabaseMappingType.create) {
                table = tableManager.createNewObject(monitor, commandContext, schema, null, options);
                tableFinalName = DBObjectNameCaseTransformer.transformName(table.getDataSource(), containerMapping.getTargetName());
                if (table instanceof DBPNamedObject2) {
                    ((DBPNamedObject2) table).setName(tableFinalName);
                } else {
                    throw new DBException("Table name cannot be set for " + tableClass.getName());
                }

                createCommand = tableManager.makeCreateCommand(table, options);
            } else {
                table = (DBSEntity) containerMapping.getTarget();
                if (table == null) {
                    throw new DBException("Internal error - target table not set");
                }
                tableFinalName = table.getName();
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

            List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, options, null);
            return actions.toArray(new DBEPersistAction[0]);
        } catch (DBException e) {
            log.debug(e);
            return null;
        }
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
        DBExecUtils.executeScript(session, actions);
        // Commit DDL changes
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
        if (txnManager != null && txnManager.isSupportsTransactions() && !txnManager.isAutoCommit()) {
            txnManager.commit(session);
        }
    }

    public static void createTargetDynamicTable(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer schema, DatabaseMappingContainer containerMapping) throws DBException {
        final DBERegistry editorsRegistry = executionContext.getDataSource().getContainer().getPlatform().getEditorsRegistry();

        Class<? extends DBSObject> tableClass = schema.getPrimaryChildType(monitor);
        if (!DBSEntity.class.isAssignableFrom(tableClass)) {
            throw new DBException("Wrong table container child type: " + tableClass.getName());
        }
        SQLObjectEditor tableManager = editorsRegistry.getObjectManager(tableClass, SQLObjectEditor.class);
        if (tableManager == null) {
            throw new DBException("Entity manager not found for '" + tableClass.getName() + "'");
        }
        DBECommandContext commandContext = new TargetCommandContext(executionContext);
        Map<String, Object> options = new HashMap<>();
        options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
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
