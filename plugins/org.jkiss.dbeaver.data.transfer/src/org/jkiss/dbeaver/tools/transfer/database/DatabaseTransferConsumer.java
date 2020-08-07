/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingCustom;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.edit.DBEStructEditor;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.AbstractCommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.meta.DBSerializable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNodePrimary;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Stream transfer consumer
 */
@DBSerializable("databaseTransferConsumer")
public class DatabaseTransferConsumer implements IDataTransferConsumer<DatabaseConsumerSettings, IDataTransferProcessor>, IDataTransferNodePrimary {

    private static final Log log = Log.getLog(DatabaseTransferConsumer.class);

    private static final boolean USE_STRUCT_DDL = false;

    private DBSDataContainer sourceObject;
    private DBSDataManipulator targetObject;
    private DatabaseConsumerSettings settings;
    private DatabaseMappingContainer containerMapping;
    private ColumnMapping[] columnMappings;
    private DBDAttributeBinding[] sourceBindings;
    private DBCExecutionContext targetContext;
    private DBCSession targetSession;
    private DBSDataManipulator.ExecuteBatch executeBatch;
    private long rowsExported = 0;
    private boolean ignoreErrors = false;

    private List<DBSEntityAttribute> targetAttributes;
    private boolean useIsolatedConnection;
    private Boolean oldAutoCommit;

    // Used only for non-explicit import
    // In this case consumer will be replaced with explicit consumers during configuration
    private DBSObjectContainer targetObjectContainer;

    private boolean isPreview;
    private List<Object[]> previewRows;

    public static class ColumnMapping {
        public DBDAttributeBinding sourceAttr;
        public DatabaseMappingAttribute targetAttr;
        public DBDValueHandler sourceValueHandler;
        public DBDValueHandler targetValueHandler;
        public int targetIndex = -1;

        private ColumnMapping(DBDAttributeBinding sourceAttr) {
            this.sourceAttr = sourceAttr;
        }

        @Override
        public String toString() {
            return sourceAttr + "->" + targetAttr;
        }
    }

    public DatabaseTransferConsumer() {
    }

    public DatabaseTransferConsumer(DBSDataManipulator targetObject) {
        this.targetObject = targetObject;
    }

    public DatabaseTransferConsumer(DBSObjectContainer targetObjectContainer) {
        this.targetObjectContainer = targetObjectContainer;
    }

    public DBSObjectContainer getTargetObjectContainer() {
        return targetObjectContainer;
    }

    public ColumnMapping[] getColumnMappings() {
        return columnMappings;
    }

    @Override
    public DBSObject getDatabaseObject() {
        if (targetObjectContainer != null) {
            return targetObjectContainer;
        }
        return targetObject;
    }

    protected boolean isPreview() {
        return isPreview;
    }

    protected void setPreview(boolean preview) {
        isPreview = preview;
    }

    protected List<Object[]> getPreviewRows() {
        return previewRows;
    }

    @Override
    public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException {
        try {
            initExporter(session.getProgressMonitor());
        } catch (DBException e) {
            throw new DBCException("Error initializing exporter");
        }

        AbstractExecutionSource executionSource = new AbstractExecutionSource(sourceObject, targetContext, this);

        if (!isPreview && offset <= 0 && settings.isTruncateBeforeLoad() && (containerMapping == null || containerMapping.getMappingType() == DatabaseMappingType.existing)) {
            // Truncate target tables
            if ((targetObject.getSupportedFeatures() & DBSDataManipulator.DATA_TRUNCATE) != 0) {
                targetObject.truncateData(
                    targetSession,
                    executionSource);
            } else {
                log.error("Table '" + targetObject.getName() + "' doesn't support truncate operation");
            }
        }

        DBDAttributeBinding[] rsAttributes;
        boolean dynamicTarget = targetContext.getDataSource().getInfo().isDynamicMetadata();
        if (dynamicTarget) {
            // Document-based datasource
            rsAttributes = DBUtils.getAttributeBindings(session, sourceObject, resultSet.getMeta());
        } else {
            rsAttributes = DBUtils.makeLeafAttributeBindings(session, sourceObject, resultSet);
        }
        columnMappings = new ColumnMapping[rsAttributes.length];
        sourceBindings = rsAttributes;
        targetAttributes = new ArrayList<>(columnMappings.length);
        for (int i = 0; i < rsAttributes.length; i++) {
            if (isSkipColumn(rsAttributes[i])) {
                continue;
            }
            ColumnMapping columnMapping = new ColumnMapping(rsAttributes[i]);
            if (containerMapping == null) {
                // No explicit mappings. Mapping must be provided by data producer
                // Map all attributes directly.
                if (targetObject instanceof DBSEntity) {
                    try {
                        DBSEntityAttribute attribute = ((DBSEntity) targetObject).getAttribute(session.getProgressMonitor(), columnMapping.sourceAttr.getName());
                        if (attribute != null) {
                            columnMapping.targetAttr = new DatabaseMappingAttribute(null, columnMapping.sourceAttr);
                            columnMapping.targetAttr.setTarget(attribute);
                            columnMapping.targetAttr.setMappingType(DatabaseMappingType.existing);
                        }
                    } catch (DBException e) {
                        log.error("Error getting target attribute");
                    }
                }
                if (columnMapping.targetAttr == null) {
                    throw new DBCException("Can't resolve target attribute for [" + columnMapping.sourceAttr.getName() + "]");
                }
            } else if (!dynamicTarget) {
                columnMapping.targetAttr = containerMapping.getAttributeMapping(columnMapping.sourceAttr);
                if (columnMapping.targetAttr == null) {
                    throw new DBCException("Can't find target attribute [" + columnMapping.sourceAttr.getName() + "]");
                }
            } else {
                if (targetObject instanceof DBSDocumentContainer) {
                    try {
                        DBSEntityAttribute docAttribute = ((DBSDocumentContainer) targetObject).getDocumentAttribute(session.getProgressMonitor());
                        columnMapping.targetAttr = new DatabaseMappingAttribute(containerMapping, columnMapping.sourceAttr);
                        columnMapping.targetAttr.setTarget(docAttribute);
                    } catch (DBException e) {
                        throw new DBCException("");
                    }
                } else {
                    throw new DBCException("Can not transfer data into dynamic database which doesn't support documents");
                }
            }
            if (columnMapping.targetAttr.getMappingType() == DatabaseMappingType.skip) {
                continue;
            }
            DBSEntityAttribute targetAttr = columnMapping.targetAttr.getTarget();
            if (targetAttr == null) {
                if (columnMapping.targetAttr.getSource() instanceof DBSEntityAttribute) {
                    // Use source attr. Some datasource (e.g. document oriented do not have strict set of attributes)
                    targetAttr = (DBSEntityAttribute) columnMapping.targetAttr.getSource();
                } else {
                    throw new DBCException("Target attribute for [" + columnMapping.sourceAttr.getName() + "] wasn't resolved");
                }
            }
            columnMapping.sourceValueHandler = columnMapping.sourceAttr.getValueHandler();
            columnMapping.targetValueHandler = DBUtils.findValueHandler(targetContext.getDataSource(), targetAttr);
            columnMapping.targetIndex = targetAttributes.size();

            columnMappings[i] = columnMapping;

            targetAttributes.add(targetAttr);
        }
        DBSAttributeBase[] attributes = targetAttributes.toArray(new DBSAttributeBase[0]);

        if (!isPreview) {
            if (targetObject instanceof DBSDataManipulatorExt) {
                ((DBSDataManipulatorExt) targetObject).beforeDataChange(targetSession, DBSManipulationType.INSERT, attributes, executionSource);
            }
            executeBatch = targetObject.insertData(
                targetSession,
                attributes,
                null,
                executionSource);
        } else {
            previewRows = new ArrayList<>();
            executeBatch = new PreviewBatch();
        }
    }

    private boolean isSkipColumn(DBDAttributeBinding attr) {
        return attr.isPseudoAttribute() ||
            (!settings.isTransferAutoGeneratedColumns() && attr.isAutoGenerated()) ||
            attr instanceof DBDAttributeBindingCustom;
    }

    @Override
    public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {
        Object[] rowValues = new Object[targetAttributes.size()];
        for (int i = 0; i < columnMappings.length; i++) {
            ColumnMapping column = columnMappings[i];
            if (column == null || column.targetIndex < 0) {
                continue;
            }
            final Object attrValue;
            if (column.sourceValueHandler != null) {
                if (column.sourceAttr instanceof DBDAttributeBindingCustom) {
                    attrValue = DBUtils.getAttributeValue(column.sourceAttr, sourceBindings, rowValues);
                } else {
                    attrValue = column.sourceValueHandler.fetchValueObject(session, resultSet, column.sourceAttr, i);
                }
            } else {
                // No value handler - get raw value
                attrValue = resultSet.getAttributeValue(i);
            }
            DatabaseMappingAttribute targetAttr = column.targetAttr;
            rowValues[column.targetIndex] = column.targetValueHandler.getValueFromObject(
                targetSession,
                targetAttr.getTarget() == null ? targetAttr.getSource() : targetAttr.getTarget(),
                attrValue,
                false, false);
        }
        executeBatch.add(rowValues);

        rowsExported++;
        // No need. mnitor is incremented in data reader
        //session.getProgressMonitor().worked(1);

        insertBatch(false);
    }

    private void insertBatch(boolean force) throws DBCException {
        if (isPreview) {
            return;
        }
        boolean needCommit = force || ((rowsExported % settings.getCommitAfterRows()) == 0);
        if (needCommit && executeBatch != null) {
            targetSession.getProgressMonitor().subTask("Insert rows (" + rowsExported + ")");
            boolean retryInsert;
            do {
                retryInsert = false;
                try {
                    executeBatch.execute(targetSession);
                } catch (Throwable e) {
                    log.error("Error inserting row", e);
                    if (!ignoreErrors) {
                        switch (DBWorkbench.getPlatformUI().showErrorStopRetryIgnore(
                                DTMessages.database_transfer_consumer_task_error_occurred_during_data_load, e, true)) {
                            case STOP:
                                // just stop execution
                                throw new DBCException("Can't insert row", e);
                            case RETRY:
                                // do it again
                                retryInsert = true;
                                break;
                            case IGNORE:
                                // Just do nothing and go to the next row
                                retryInsert = false;
                                break;
                            case IGNORE_ALL:
                                ignoreErrors = true;
                                retryInsert = false;
                                break;
                        }
                    }
                }
            } while (retryInsert);
        }
        if (settings.isUseTransactions() && needCommit) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(targetSession.getExecutionContext());
            if (txnManager != null && txnManager.isSupportsTransactions() && !txnManager.isAutoCommit()) {
                targetSession.getProgressMonitor().subTask("Commit changes");
                txnManager.commit(targetSession);
            }
        }
    }

    @Override
    public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException {
        try {
            if (rowsExported > 0) {
                insertBatch(true);
            }
            if (executeBatch != null) {
                executeBatch.close();
                executeBatch = null;
            }
        } finally {
            if (!isPreview && targetObject instanceof DBSDataManipulatorExt) {
                ((DBSDataManipulatorExt) targetObject).afterDataChange(
                    targetSession,
                    DBSManipulationType.INSERT,
                    targetAttributes.toArray(new DBSAttributeBase[0]),
                    new AbstractExecutionSource(sourceObject, targetContext, this));
            }
        }
    }

    @Override
    public void close() {
        closeExporter();
    }

    private void initExporter(DBRProgressMonitor monitor) throws DBException {
        DBSObject targetDB = checkTargetContainer(monitor);

        DBPDataSourceContainer dataSourceContainer = targetDB.getDataSource().getContainer();
        if (!dataSourceContainer.hasModifyPermission(DBPDataSourcePermission.PERMISSION_IMPORT_DATA)) {
            throw new DBCException("Data transfer to database [" + dataSourceContainer.getName() + "] restricted by connection configuration");
        }

        try {
            useIsolatedConnection = !isPreview && settings.isOpenNewConnections() && !dataSourceContainer.getDriver().isEmbedded();
            targetContext = useIsolatedConnection ?
                DBUtils.getObjectOwnerInstance(targetDB).openIsolatedContext(monitor, "Data transfer consumer", null) : DBUtils.getDefaultContext(targetDB, false);
        } catch (DBException e) {
            throw new DBCException("Error opening new connection", e);
        }
        targetSession = targetContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Data load");
        targetSession.enableLogging(false);

        if (!isPreview) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(targetSession.getExecutionContext());
            if (txnManager != null && txnManager.isSupportsTransactions()) {
                oldAutoCommit = txnManager.isAutoCommit();
                if (settings.isUseTransactions()) {
                    if (oldAutoCommit) {
                        txnManager.setAutoCommit(monitor, false);
                    }
                } else {
                    if (!oldAutoCommit) {
                        txnManager.setAutoCommit(monitor, true);
                    }
                }
            }
        }
    }

    private DBSObject checkTargetContainer(DBRProgressMonitor monitor) throws DBException {
        if (targetObject == null) {
            if (settings.getContainerNode() != null && settings.getContainerNode().getDataSource() == null) {
                // Init connection
                settings.getContainerNode().initializeNode(monitor, null);
            }
            if (settings.getContainer() == null) {
                throw new DBCException("Can't initialize database consumer. No target object and no target container");
            }
        }
        containerMapping = sourceObject == null ? null : settings.getDataMapping(sourceObject);

        return targetObject == null ? settings.getContainer() : targetObject;
    }

    private void closeExporter() {
        if (!isPreview && targetSession != null && oldAutoCommit != null) {
            try {
                DBCTransactionManager txnManager = DBUtils.getTransactionManager(targetSession.getExecutionContext());
                if (txnManager != null) {
                    txnManager.setAutoCommit(targetSession.getProgressMonitor(), oldAutoCommit);
                }
            } catch (Exception e) {
                log.debug("Error reverting auto-commit mode", e);
            }
        }

        try {
            if (targetSession != null) {
                targetSession.close();
                targetSession = null;
            }
        } catch (Throwable e) {
            log.debug(e);
        }
        if (targetContext != null && useIsolatedConnection) {
            targetContext.close();
            targetContext = null;
        }
    }

    @Override
    public void initTransfer(DBSObject sourceObject, DatabaseConsumerSettings settings, TransferParameters parameters, IDataTransferProcessor processor, Map<String, Object> processorProperties) {
        this.sourceObject = (DBSDataContainer) sourceObject;
        this.settings = settings;
    }

    @Override
    public void startTransfer(DBRProgressMonitor monitor) throws DBException {
        // Create all necessary database objects
        monitor.beginTask("Create necessary database objects", 1);
        try {
            DBSObject dbObject = checkTargetContainer(monitor);

            if (!isPreview && containerMapping != null) {
                DBSObjectContainer container = settings.getContainer();
                if (container == null) {
                    throw new DBException("No target datasource - can't create target objects");
                }
                targetObject = containerMapping.getTarget();

                boolean hasNewObjects = createTargetDatabaseObjects(monitor, dbObject);

                if (hasNewObjects) {
                    refreshDatabaseModel(monitor, container);
                }
            }
        } finally {
            monitor.done();
        }
    }

    private boolean createTargetDatabaseObjects(DBRProgressMonitor monitor, DBSObject dbObject) throws DBException {
        try (DBCSession session = DBUtils.openMetaSession(monitor, dbObject, "Create target metadata")) {
            // We may need to change active catalog to create target object in the proper location
            DBSCatalog oldCatalog = null;
            DBSSchema oldSchema = null;
            DBSCatalog catalog = dbObject instanceof DBSSchema ? DBUtils.getParentOfType(DBSCatalog.class, dbObject) : null;
            if (catalog != null) {
                DBCExecutionContextDefaults contextDefaults = session.getExecutionContext().getContextDefaults();
                if (contextDefaults != null && contextDefaults.supportsCatalogChange() && contextDefaults.getDefaultCatalog() != catalog) {
                    oldCatalog = contextDefaults.getDefaultCatalog();
                    try {
                        contextDefaults.setDefaultCatalog(monitor, catalog, (DBSSchema) dbObject);
                    } catch (DBCException e) {
                        log.debug(e);
                    }
                }
            }
            try {
                switch (containerMapping.getMappingType()) {
                    case create:
                        createTargetTable(session, containerMapping);
                        return true;
                    case existing:
                        boolean hasNewObjects = false;
                        for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                            if (attr.getMappingType() == DatabaseMappingType.create) {
                                createTargetAttribute(session, attr);
                                hasNewObjects = true;
                            }
                        }
                        return hasNewObjects;
                    default:
                        return false;
                }
            }
            finally {
                if (oldCatalog != null) {
                    // Revert to old catalog
                    try {
                        session.getExecutionContext().getContextDefaults().setDefaultCatalog(monitor, oldCatalog, oldSchema);
                    } catch (DBCException e) {
                        log.debug(e);
                    }
                }
            }
        }
    }

    private void refreshDatabaseModel(DBRProgressMonitor monitor, DBSObjectContainer container) throws DBException {
        if (!USE_STRUCT_DDL) {
            monitor.subTask("Refresh navigator model");
            settings.getContainerNode().refreshNode(monitor, this);
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
                    targetObject = (DBSDataManipulator) newTarget;
                    // ! Fall down is ok here
                case existing:
                    for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                        if (attr.getMappingType() == DatabaseMappingType.create) {
                            attr.updateMappingType(monitor);
                            if (attr.getTarget() == null) {
                                log.debug("Can't find target attribute '" + attr.getTargetName() + "' in '" + containerMapping.getTargetName() + "'");
                            }
                        }
                    }
                    break;
            }
        }
    }

    private void createTargetTable(DBCSession session, DatabaseMappingContainer containerMapping) throws DBException {
        DBSObjectContainer schema = settings.getContainer();
        if (schema == null) {
            throw new DBException("No target container selected");
        }
        if (session.getDataSource().getInfo().isDynamicMetadata()) {
            createTargetDynamicTable(session.getProgressMonitor(), session.getExecutionContext(), schema, containerMapping);
        } else {
            String sql = generateTargetTableDDL(session.getProgressMonitor(), session.getExecutionContext(), schema, containerMapping);
            try {
                executeDDL(session, sql);
            } catch (DBCException e) {
                throw new DBCException("Can't create target table:\n" + sql, e);
            }
        }
    }

    private void createTargetDynamicTable(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer schema, DatabaseMappingContainer containerMapping) throws DBException {
        final DBERegistry editorsRegistry = executionContext.getDataSource().getContainer().getPlatform().getEditorsRegistry();

        Class<? extends DBSObject> tableClass = schema.getChildType(monitor);
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

    public static String generateTargetTableDDL(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer schema, DatabaseMappingContainer containerMapping) throws DBException {
        if (containerMapping.getMappingType() == DatabaseMappingType.skip) {
            return "";
        }
        monitor.subTask("Create table '" + containerMapping.getTargetName() + "'");
        if (USE_STRUCT_DDL) {
            String ddl = generateStructTableDDL(monitor, executionContext, schema, containerMapping);
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
        } else {
            for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                if (attr.getMappingType() == DatabaseMappingType.create) {
                    sql.append(generateTargetAttributeDDL(dataSource, attr)).append(";\n");
                }
            }
        }
        return sql.toString();
    }

    private static String generateStructTableDDL(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer schema, DatabaseMappingContainer containerMapping) {
        final DBERegistry editorsRegistry = executionContext.getDataSource().getContainer().getPlatform().getEditorsRegistry();

        try {
            Class<? extends DBSObject> tableClass = schema.getChildType(monitor);
            if (!DBSEntity.class.isAssignableFrom(tableClass)) {
                throw new DBException("Wrong table container child type: " + tableClass.getName());
            }
            SQLObjectEditor tableManager = editorsRegistry.getObjectManager(tableClass, SQLObjectEditor.class);
            if (tableManager == null) {
                throw new DBException("Table manager not found for '" + tableClass.getName() + "'");
            }
            if (!(tableManager instanceof DBEStructEditor)) {
                throw new DBException("Table create not supported by " + executionContext.getDataSource().getContainer().getDriver().getName());
            }
            Class<?>[] childTypes = ((DBEStructEditor<?>) tableManager).getChildTypes();
            Class<? extends DBSEntityAttribute> attrClass = getChildType(childTypes, DBSEntityAttribute.class);
            if (attrClass == null) {
                throw new DBException("Column manager not found for '" + tableClass.getName() + "'");
            }

            SQLObjectEditor attributeManager = editorsRegistry.getObjectManager(attrClass, SQLObjectEditor.class);

            Map<String, Object> options = new HashMap<>();
            options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);

            DBECommandContext commandContext = new TargetCommandContext(executionContext);

            DBSEntity newTable = (DBSEntity) tableManager.createNewObject(monitor, commandContext, schema, null, options);
            if (newTable instanceof DBPNamedObject2) {
                ((DBPNamedObject2) newTable).setName(containerMapping.getTargetName());
            } else {
                throw new DBException("Table name cannot be set for " + tableClass.getName());
            }

            SQLStructEditor.StructCreateCommand command = (SQLStructEditor.StructCreateCommand) tableManager.makeCreateCommand(newTable, options);

            for (DatabaseMappingAttribute attributeMapping : containerMapping.getAttributeMappings(monitor)) {
                DBSEntityAttribute newAttribute = (DBSEntityAttribute) attributeManager.createNewObject(monitor, commandContext, newTable, null, options);
                if (!(newAttribute instanceof DBPNamedObject2)) {
                    throw new DBException("Table column name cannot be set for " + attrClass.getName());
                }
                ((DBPNamedObject2) newAttribute).setName(attributeMapping.getTargetName());
                String targetAttrType = attributeMapping.getTargetType(executionContext.getDataSource());
                try {
                    BeanUtils.invokeObjectMethod(newAttribute, "setTypeName", new Class[]{String.class}, new Object[]{ targetAttrType} );
                } catch (Throwable throwable) {
                    throw new DBException("Table column data type cannot be set for " + newAttribute.getClass().getName());
                }

                SQLObjectEditor.ObjectCreateCommand attrCreateCommand = attributeManager.makeCreateCommand(newAttribute, options);
                command.aggregateCommand(attrCreateCommand);
            }

            DBEPersistAction[] persistActions = command.getPersistActions(monitor, executionContext, options);
            return SQLUtils.generateScript(executionContext.getDataSource(), persistActions, false);
        } catch (DBException e) {
            log.debug(e);
            return null;
        }
    }

    private static <T> Class<? extends T> getChildType(Class<?>[] types, Class<T> type) {
        for (Class<?> childType : types) {
            if (type.isAssignableFrom(childType)) {
                return (Class<? extends T>) childType;
            }
        }
        return null;
    }

    private static void appendAttributeClause(DBPDataSource dataSource, StringBuilder sql, DatabaseMappingAttribute attr) {
        sql.append(DBUtils.getQuotedIdentifier(dataSource, attr.getTargetName())).append(" ").append(attr.getTargetType(dataSource));
        if (SQLUtils.getDialectFromDataSource(dataSource).supportsNullability()) {
            if (attr.getSource().isRequired()) sql.append(" NOT NULL");
        }
    }

    private void createTargetAttribute(DBCSession session, DatabaseMappingAttribute attribute) throws DBCException {
        session.getProgressMonitor().subTask("Create column " + DBUtils.getObjectFullName(attribute.getParent().getTarget(), DBPEvaluationContext.DDL) + "." + attribute.getTargetName());
        String sql = generateTargetAttributeDDL(session.getDataSource(), attribute);
        try {
            executeDDL(session, sql);
        } catch (DBCException e) {
            throw new DBCException("Can't create target column:\n" + sql, e);
        }
    }

    @NotNull
    private static String generateTargetAttributeDDL(DBPDataSource dataSource, DatabaseMappingAttribute attribute) {
        StringBuilder sql = new StringBuilder(500);
        sql.append("ALTER TABLE ").append(DBUtils.getObjectFullName(attribute.getParent().getTarget(), DBPEvaluationContext.DDL))
            .append(" ADD ");
        appendAttributeClause(dataSource, sql, attribute);
        return sql.toString();
    }

    private void executeDDL(DBCSession session, String sql)
        throws DBCException {
        try (DBCStatement dbStat = DBUtils.makeStatement(session, sql, false)) {
            dbStat.executeStatement();
        }
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
        if (txnManager != null && txnManager.isSupportsTransactions() && !txnManager.isAutoCommit()) {
            // Commit DDL changes
            txnManager.commit(session);
        }
    }

    @Override
    public void finishTransfer(DBRProgressMonitor monitor, boolean last) {
        if (last) {
            // Refresh navigator
            monitor.subTask("Refresh navigator model");
            try {
                settings.getContainerNode().refreshNode(monitor, this);
            } catch (Exception e) {
                log.debug("Error refreshing navigator model after data consumer", e);
            }
        }

        if (!last && settings.isOpenTableOnFinish()) {
            if (targetObject != null) {
                DBWorkbench.getPlatformUI().openEntityEditor(targetObject);
            }
        }
    }

    public DBSDataManipulator getTargetObject() {
        return targetObject != null ? targetObject : containerMapping == null ? null : containerMapping.getTarget();
    }

    @Override
    public String getObjectName() {
        if (targetObjectContainer != null) {
            return targetObjectContainer.getName();
        }

        String targetName = null;
        if (targetObject != null) {
            targetName = DBUtils.getObjectFullName(targetObject, DBPEvaluationContext.UI);
        }
        if (settings == null) {
            return targetName;
        }

        if (targetName != null) {
            return targetName;
        }

        DatabaseMappingContainer dataMapping = settings.getDataMapping(sourceObject);
        if (dataMapping == null) {
            return "?";
        }

        targetName = dataMapping.getTargetName();

        switch (dataMapping.getMappingType()) {
            case create:
                return targetName + " [Create]";
            case existing:
                for (DatabaseMappingAttribute attr : dataMapping.getAttributeMappings(new VoidProgressMonitor())) {
                    if (attr.getMappingType() == DatabaseMappingType.create) {
                        return targetName + " [Alter]";
                    }
                }
                return targetName;// + " [No changes]";
            case skip:
                return "[Skip]";
            default:
                return targetName;
        }
    }

    @Override
    public DBPImage getObjectIcon() {
        if (targetObjectContainer != null) {
            return DBIcon.TREE_FOLDER_TABLE;
        }
        if (targetObject instanceof DBPImageProvider) {
            return DBValueFormatting.getObjectImage(targetObject);
        }
        return DBIcon.TREE_TABLE;
    }

    @Override
    public String getObjectContainerName() {
        if (targetObjectContainer != null) {
            return DBUtils.getObjectFullName(targetObjectContainer, DBPEvaluationContext.UI);
        }
        DBPDataSourceContainer container = getDataSourceContainer();
        return container != null ? container.getName() : "?";
    }

    @Override
    public DBPImage getObjectContainerIcon() {
        if (targetObjectContainer != null) {
            return DBIcon.TREE_FOLDER_TABLE;
        }
        DBPDataSourceContainer container = getDataSourceContainer();
        return container != null ? container.getDriver().getIcon() : null;
    }

    private DBPDataSourceContainer getDataSourceContainer() {
        if (targetObjectContainer != null) {
            return targetObjectContainer.getDataSource().getContainer();
        }
        if (targetObject != null) {
            return targetObject.getDataSource().getContainer();
        }
        DBSObjectContainer container = settings.getContainer();
        if (container != null) {
            return container.getDataSource().getContainer();
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DatabaseTransferConsumer &&
            CommonUtils.equalObjects(targetObject, ((DatabaseTransferConsumer) obj).targetObject);
    }

    public static class ObjectSerializer implements DBPObjectSerializer<DBTTask, DatabaseTransferConsumer> {

        @Override
        public void serializeObject(DBRRunnableContext runnableContext, DBTTask context, DatabaseTransferConsumer object, Map<String, Object> state) {
            try {
                DatabaseMappingContainer targetMapping = object.containerMapping;
                DBPDataSourceContainer targetDS = object.getDataSourceContainer();
                if (targetDS == null) {
                    throw new DBException("Can't get target datasource container");
                }
                state.put("type", "mappings");
                state.put("project", targetDS.getProject().getName());
                state.put("dataSource", targetDS.getId());

                DBSDataContainer dataContainer = object.getTargetObject();
                if (dataContainer instanceof DBSEntity) {
                    state.put("entityId", DBUtils.getObjectFullId(dataContainer));
                }
            } catch (Exception e) {
                log.error("Error initializing database consumer", e);
            }
        }

        @Override
        public DatabaseTransferConsumer deserializeObject(DBRRunnableContext runnableContext, DBTTask objectContext, Map<String, Object> state) throws DBCException {
            DatabaseTransferConsumer consumer = new DatabaseTransferConsumer();

            String entityId = CommonUtils.toString(state.get("entityId"), null);
            if (entityId != null) {
                try {
                    runnableContext.run(false, true, monitor -> {
                        try {
                            String projectName = CommonUtils.toString(state.get("project"));
                            DBPProject project = CommonUtils.isEmpty(projectName) ? null : DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
                            if (project == null) {
                                throw new DBCException("Project '" + projectName + "' not found");
                            }
                            consumer.targetObject = (DBSDataManipulator) DBUtils.findObjectById(monitor, project, entityId);
                        } catch (Exception e) {
                            throw new InvocationTargetException(e);
                        }
                    });
                } catch (InvocationTargetException e) {
                    throw new DBCException("Error instantiating data consumer", e.getTargetException());
                } catch (InterruptedException e) {
                    throw new DBCException("Deserialization canceled", e);
                }
            }

            return consumer;
        }
    }

    private static class TargetCommandContext extends AbstractCommandContext {
        TargetCommandContext(DBCExecutionContext executionContext) {
            super(executionContext, true);
        }
    }

    private class PreviewBatch implements DBSDataManipulator.ExecuteBatch {
        @Override
        public void add(@NotNull Object[] attributeValues) throws DBCException {
            previewRows.add(attributeValues);
        }

        @NotNull
        @Override
        public DBCStatistics execute(@NotNull DBCSession session) throws DBCException {
            return new DBCStatistics();
        }

        @Override
        public void generatePersistActions(@NotNull DBCSession session, @NotNull List<DBEPersistAction> actions, Map<String, Object> options) throws DBCException {

        }

        @Override
        public void close() {

        }
    }
}
