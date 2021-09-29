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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingCustom;
import org.jkiss.dbeaver.model.data.DBDInsertReplaceMethod;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.meta.DBSerializable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.registry.SQLInsertReplaceMethodDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLInsertReplaceMethodRegistry;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.tools.transfer.IDataTransferAttributeTransformer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNodePrimary;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Stream transfer consumer
 */
@DBSerializable("databaseTransferConsumer")
public class DatabaseTransferConsumer implements IDataTransferConsumer<DatabaseConsumerSettings, IDataTransferProcessor>,
        IDataTransferNodePrimary, DBPReferentialIntegrityController {
    private static final Log log = Log.getLog(DatabaseTransferConsumer.class);

    private DatabaseConsumerSettings settings;
    private DatabaseMappingContainer containerMapping;
    private ColumnMapping[] columnMappings;
    private DBDAttributeBinding[] sourceBindings;
    private DBCExecutionContext targetContext;
    private DBCSession targetSession;
    private DBSDataManipulator.ExecuteBatch executeBatch;
    private long rowsExported = 0;
    private boolean ignoreErrors = false;

    private List<DBSAttributeBase> targetAttributes;
    private boolean useIsolatedConnection;
    private Boolean oldAutoCommit;

    // Used only for non-explicit import
    // In this case consumer will be replaced with explicit consumers during configuration
    private DBSObjectContainer targetObjectContainer;
    // Used in deserialized or directly instantiated consumers
    private DBSDataManipulator localTargetObject;

    private boolean isPreview;
    private List<Object[]> previewRows;
    private DBDAttributeBinding[] rsAttributes;

    public static class ColumnMapping {
        public DBDAttributeBinding sourceAttr;
        public DatabaseMappingAttribute targetAttr;
        public DBDValueHandler sourceValueHandler;
        public DBDValueHandler targetValueHandler;
        public int targetIndex = -1;
        public IDataTransferAttributeTransformer valueTransformer;
        public Map<String, Object> valueTransformerProperties;

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
        this.localTargetObject = targetObject;
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
        return containerMapping == null ? localTargetObject : containerMapping.getTarget();
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
            throw new DBCException("Error initializing exporter", e);
        }
        if (containerMapping == null) {
            throw new DBCException("Internal error: consumer mappings not set");
        }

        AbstractExecutionSource executionSource = new AbstractExecutionSource(containerMapping.getSource(), targetContext, this);

        DBSDataManipulator targetObject = getTargetObject();
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

        boolean dynamicTarget = targetContext.getDataSource().getInfo().isDynamicMetadata();
        DBSDataContainer sourceObject = getSourceObject();
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
            } else if (sourceObject instanceof DBSDocumentContainer && dynamicTarget) {
                try {
                    DBSDocumentContainer docContainer = (DBSDocumentContainer) (targetObject instanceof DBSDocumentContainer ? targetObject : sourceObject);
                    DBSEntityAttribute docAttribute = docContainer.getDocumentAttribute(session.getProgressMonitor());
                    if (docAttribute != null) {
                        columnMapping.targetAttr = new DatabaseMappingAttribute(containerMapping, columnMapping.sourceAttr);
                        columnMapping.targetAttr.setTarget(docAttribute);
                        columnMapping.targetAttr.setMappingType(DatabaseMappingType.existing);
                    }
                } catch (DBException e) {
                    throw new DBCException("Error getting document attribute", e);
                }
            } else {
                columnMapping.targetAttr = containerMapping.getAttributeMapping(columnMapping.sourceAttr);
                if (columnMapping.targetAttr == null) {
                    throw new DBCException("Can't find target attribute [" + columnMapping.sourceAttr.getName() + "]");
                }
            }
            if (columnMapping.targetAttr.getMappingType() == DatabaseMappingType.skip) {
                continue;
            }
            if (columnMapping.targetAttr.getTransformer() != null) {
                try {
                    columnMapping.valueTransformer = columnMapping.targetAttr.getTransformer().createTransformer();
                    columnMapping.valueTransformerProperties = columnMapping.targetAttr.getTransformerProperties();
                } catch (DBException e) {
                    throw new DBCException("Can't create attribute transformer", e);
                }
            }
            DBSAttributeBase targetAttr = columnMapping.targetAttr.getTarget();
            if (targetAttr == null) {
                if (isPreview) {
                    targetAttr = new PreviewColumnInfo(null, columnMapping.sourceAttr, columnMapping.targetIndex);
                } else if (columnMapping.targetAttr.getSource() instanceof DBSEntityAttribute || targetObject instanceof DBSDocumentContainer) {
                    // Use source attr. Some datasource (e.g. document oriented do not have strict set of attributes)
                    targetAttr = columnMapping.targetAttr.getSource();
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

        Map<String, Object> options = new HashMap<>();
        options.put(DBSDataManipulator.OPTION_USE_MULTI_INSERT, settings.isUseMultiRowInsert());
        options.put(DBSDataManipulator.OPTION_SKIP_BIND_VALUES, settings.isSkipBindValues());

        if (!isPreview) {
            if (targetObject instanceof DBSDataManipulatorExt) {
                ((DBSDataManipulatorExt) targetObject).beforeDataChange(targetSession, DBSManipulationType.INSERT, attributes, executionSource);
            }
            executeBatch = targetObject.insertData(
                targetSession,
                attributes,
                null,
                executionSource,
                options);
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
            Object attrValue;
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

            if (containerMapping != null && containerMapping.getTarget() instanceof DBSDocumentContainer) {
                rowValues[column.targetIndex] = attrValue;
            } else {
                DatabaseMappingAttribute targetAttr = column.targetAttr;
                rowValues[column.targetIndex] = column.targetValueHandler.getValueFromObject(
                    targetSession,
                    targetAttr.getTarget() == null ? targetAttr.getSource() : targetAttr.getTarget(),
                    attrValue,
                    false, false);
            }
        }

        // Transform value
        for (ColumnMapping column : columnMappings) {
            if (column == null || column.targetIndex < 0) {
                continue;
            }
            if (column.valueTransformer != null) {
                Object attrValue = rowValues[column.targetIndex];
                try {
                    rowValues[column.targetIndex] = column.valueTransformer.transformAttribute(
                        session,
                        rsAttributes,
                        rowValues,
                        column.sourceAttr,
                        attrValue,
                        column.valueTransformerProperties);
                } catch (DBException e) {
                    throw new DBCException(
                        "Error transforming attribute '" + column.sourceAttr.getName() +
                            "' value with transformer '" + column.targetAttr.getTransformer().getName() + "'", e);
                }
            }
        }


        executeBatch.add(rowValues);

        rowsExported++;
        // No need. monitor is incremented in data reader
        //session.getProgressMonitor().worked(1);

        insertBatch(false);
    }

    private void insertBatch(boolean force) throws DBCException {
        if (isPreview) {
            return;
        }
        boolean needCommit = force || ((rowsExported % settings.getCommitAfterRows()) == 0);
        Map<String, Object> options = new HashMap<>();
        boolean disableUsingBatches = settings.isDisableUsingBatches();
        boolean onDuplicateKeyCaseOn = settings.getOnDuplicateKeyInsertMethodId() != null && !settings.getOnDuplicateKeyInsertMethodId().equals(DBSDataManipulator.INSERT_NONE_METHOD);
        options.put(DBSDataManipulator.OPTION_DISABLE_BATCHES, disableUsingBatches);
        options.put(DBSDataManipulator.OPTION_MULTI_INSERT_BATCH_SIZE, settings.getMultiRowInsertBatch());
        options.put(DBSDataManipulator.OPTION_SKIP_BIND_VALUES, settings.isSkipBindValues());
        if (onDuplicateKeyCaseOn) {
            String insertMethodId = settings.getOnDuplicateKeyInsertMethodId();
            SQLInsertReplaceMethodDescriptor insertReplaceMethod = SQLInsertReplaceMethodRegistry.getInstance().getInsertMethod(insertMethodId);
            if (insertReplaceMethod != null) {
                try {
                    DBDInsertReplaceMethod insertMethod = insertReplaceMethod.createInsertMethod();
                    options.put(DBSDataManipulator.OPTION_INSERT_REPLACE_METHOD, insertMethod);
                } catch (DBException e) {
                    log.debug("Can't get insert replace method", e);
                }
            }
        }
        if ((needCommit || disableUsingBatches) && executeBatch != null) {
            targetSession.getProgressMonitor().subTask("Insert rows (" + rowsExported + ")");
            boolean retryInsert;
            do {
                retryInsert = false;
                try {
                    executeBatch.execute(targetSession, options);
                } catch (Throwable e) {
                    log.error("Error inserting row", e);
                    if (ignoreErrors) {
                        break;
                    }
                    String message;
                    if (disableUsingBatches) {
                        message = DTMessages.database_transfer_consumer_task_error_occurred_during_data_load;
                    } else {
                        message = DTMessages.database_transfer_consumer_task_error_occurred_during_batch_insert;
                    }
                    DBPPlatformUI.UserResponse response = DBWorkbench.getPlatformUI().showErrorStopRetryIgnore(message, e, true);
                    switch (response) {
                        case STOP:
                            throw new DBCException("Can't insert row", e);
                        case RETRY:
                            retryInsert = true;
                            break;
                        case IGNORE:
                            retryInsert = false;
                            break;
                        case IGNORE_ALL:
                            ignoreErrors = true;
                            retryInsert = false;
                            break;
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
            DBSDataManipulator targetObject = getTargetObject();
            if (!isPreview && targetObject instanceof DBSDataManipulatorExt) {
                ((DBSDataManipulatorExt) targetObject).afterDataChange(
                    targetSession,
                    DBSManipulationType.INSERT,
                    targetAttributes.toArray(new DBSAttributeBase[0]),
                    new AbstractExecutionSource(getSourceObject(), targetContext, this));
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

    DBSObject checkTargetContainer(DBRProgressMonitor monitor) throws DBException {
        DBSDataManipulator targetObject = getTargetObject();
        if (targetObject == null) {
            if (settings.getContainerNode() != null && settings.getContainerNode().getDataSource() == null) {
                // Init connection
                settings.getContainerNode().initializeNode(monitor, null);
            }
            if (settings.getContainer() == null) {
                throw new DBCException("Can't initialize database consumer. No target object and no target container");
            }
        }

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
        this.settings = settings;
        this.containerMapping = settings.getDataMapping((DBSDataContainer) sourceObject);
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

                boolean hasNewObjects = createTargetDatabaseObjects(monitor, dbObject);

                if (hasNewObjects) {
                    DatabaseTransferUtils.refreshDatabaseModel(monitor, settings, containerMapping);
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
                    case existing:
                        return createTargetTable(session, containerMapping);
                    default:
                        return false;
                }
            } finally {
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

    private boolean createTargetTable(DBCSession session, DatabaseMappingContainer containerMapping) throws DBException {
        DBPDataSourceContainer dataSourceContainer = session.getDataSource().getContainer();
        if (!dataSourceContainer.hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_METADATA)) {
            throw new DBCException("New table creation in database [" + dataSourceContainer.getName() + "] restricted by connection configuration");
        }
        DBSObjectContainer schema = settings.getContainer();
        if (schema == null) {
            throw new DBException("No target container selected");
        }
        if (session.getDataSource().getInfo().isDynamicMetadata()) {
            DatabaseTransferUtils.createTargetDynamicTable(session.getProgressMonitor(), session.getExecutionContext(), schema, containerMapping);
            return true;
        } else {
            DBEPersistAction[] actions = DatabaseTransferUtils.generateTargetTableDDL(session.getProgressMonitor(), session.getExecutionContext(), schema, containerMapping);
            try {
                DatabaseTransferUtils.executeDDL(session, actions);
            } catch (DBCException e) {
                throw new DBCException("Can't create or update target table:\n" + Arrays.toString(actions), e);
            }
            return actions.length > 0;
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
            if (getTargetObject() != null) {
                DBWorkbench.getPlatformUI().openEntityEditor(getTargetObject());
            }
        }
    }

    public DBSDataContainer getSourceObject() {
        return containerMapping == null ? null : containerMapping.getSource();
    }

    public DBSDataManipulator getTargetObject() {
        return containerMapping == null ? localTargetObject : containerMapping.getTarget();
    }

    public void setTargetObject(DBSDataManipulator targetObject) {
        this.localTargetObject = targetObject;
    }

    @Override
    public String getObjectName() {
        if (targetObjectContainer != null) {
            return targetObjectContainer.getName();
        }

        DBSDataManipulator targetObject = getTargetObject();

        String targetName = null;
        if (targetObject != null) {
            targetName = DBUtils.getObjectFullName(targetObject, DBPEvaluationContext.UI);
        }

        if (targetName != null) {
            return targetName + " [Existing]";
        }

        if (containerMapping == null) {
            return "?";
        }

        targetName = containerMapping.getTargetFullName();

        switch (containerMapping.getMappingType()) {
            case create:
                return targetName + " [Create]";
            case existing:
                for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(new VoidProgressMonitor())) {
                    if (attr.getMappingType() == DatabaseMappingType.create) {
                        return targetName + " [Alter]";
                    }
                }
                return targetName;// + " [No changes]";
            case skip:
                return "[Skip]";
            default:
                return targetName + " [Existing]";
        }
    }

    @Override
    public DBPImage getObjectIcon() {
        if (targetObjectContainer != null) {
            return DBIcon.TREE_FOLDER_TABLE;
        }
        DBSDataManipulator targetObject = getTargetObject();
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

    DBPDataSourceContainer getDataSourceContainer() {
        if (targetObjectContainer != null) {
            return targetObjectContainer.getDataSource().getContainer();
        }
        DBSDataManipulator targetObject = getTargetObject();
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
            CommonUtils.equalObjects(getTargetObject(), ((DatabaseTransferConsumer) obj).getTargetObject());
    }

    @Override
    public boolean supportsChangingReferentialIntegrity(@NotNull DBRProgressMonitor monitor) throws DBException {
        return checkTargetContainer(monitor) instanceof DBPReferentialIntegrityController;
    }

    @Override
    public void enableReferentialIntegrity(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        DBSObject dbsObject = checkTargetContainer(monitor);
        if (!(dbsObject instanceof DBPReferentialIntegrityController)) {
            throw new DBException("Changing referential integrity is unsupported!");
        }
        DBPReferentialIntegrityController controller = (DBPReferentialIntegrityController) dbsObject;
        controller.enableReferentialIntegrity(monitor, enable);
    }

    @Nullable
    @Override
    public String getChangeReferentialIntegrityStatement(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        DBSObject dbsObject = checkTargetContainer(monitor);
        if (dbsObject instanceof DBPReferentialIntegrityController) {
            return ((DBPReferentialIntegrityController) dbsObject).getChangeReferentialIntegrityStatement(monitor, enable);
        }
        return null;
    }

    public DatabaseConsumerSettings getSettings() {
        return settings;
    }

    private class PreviewBatch implements DBSDataManipulator.ExecuteBatch {
        @Override
        public void add(@NotNull Object[] attributeValues) throws DBCException {
            previewRows.add(attributeValues);
        }

        @NotNull
        @Override
        public DBCStatistics execute(@NotNull DBCSession session, Map<String, Object> options) throws DBCException {
            return new DBCStatistics();
        }

        @Override
        public void generatePersistActions(@NotNull DBCSession session, @NotNull List<DBEPersistAction> actions, Map<String, Object> options) throws DBCException {

        }

        @Override
        public void close() {

        }
    }

    /*
     * This class is only suitable for data transfer preview.
     */
    private static class PreviewColumnInfo extends AbstractAttribute implements DBSEntityAttribute {
        private final DBSEntity entity;
        private final DBDAttributeBinding binding;

        public PreviewColumnInfo(DBSEntity entity, DBDAttributeBinding binding, int index) {
            super(binding.getName(), binding.getTypeName(), -1, index, binding.getMaxLength(), null, null, false, false);
            this.entity = entity;
            this.binding = binding;
        }

        @Nullable
        @Override
        public String getDefaultValue() {
            return null;
        }

        @NotNull
        @Override
        public DBSEntity getParentObject() {
            return entity;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource() {
            return this.binding.getDataSource();
        }

        @Override
        public DBPDataKind getDataKind() {
            return this.binding.getDataKind();
        }
    }
}
