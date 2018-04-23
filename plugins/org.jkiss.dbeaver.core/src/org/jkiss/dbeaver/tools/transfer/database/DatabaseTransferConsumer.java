/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.dialogs.exec.ExecutionQueueErrorJob;
import org.jkiss.dbeaver.ui.dialogs.exec.ExecutionQueueErrorResponse;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
* Stream transfer consumer
*/
public class DatabaseTransferConsumer implements IDataTransferConsumer<DatabaseConsumerSettings, IDataTransferProcessor> {

    private static final Log log = Log.getLog(DatabaseTransferConsumer.class);

    private DBSDataContainer sourceObject;
    private DBSDataManipulator targetObject;
    private DatabaseConsumerSettings settings;
    private DatabaseMappingContainer containerMapping;
    private ColumnMapping[] columnMappings;
    private DBCExecutionContext targetContext;
    private DBCSession targetSession;
    private DBSDataManipulator.ExecuteBatch executeBatch;
    private long rowsExported = 0;
    private boolean ignoreErrors = false;
    private List<DBSEntityAttribute> targetAttributes;

    private static class ColumnMapping {
        DBCAttributeMetaData sourceAttr;
        DatabaseMappingAttribute targetAttr;
        DBDValueHandler sourceValueHandler;
        DBDValueHandler targetValueHandler;
        int targetIndex = -1;

        private ColumnMapping(DBCAttributeMetaData sourceAttr)
        {
            this.sourceAttr = sourceAttr;
        }
    }

    public DatabaseTransferConsumer()
    {
    }

    public DatabaseTransferConsumer(DBSDataManipulator targetObject)
    {
        this.targetObject = targetObject;
    }

    @Override
    public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException
    {
        initExporter(session.getProgressMonitor());

        if (settings.isTruncateBeforeLoad() && containerMapping.getMappingType() == DatabaseMappingType.existing) {
            // Truncate target tables
            if ((containerMapping.getTarget().getSupportedFeatures() & DBSDataManipulator.DATA_TRUNCATE) != 0) {
                containerMapping.getTarget().truncateData(
                    targetSession,
                    new AbstractExecutionSource(sourceObject, targetContext, this));
            } else {
                log.error("Table '" + targetObject.getName() + "' doesn't support truncate operation");
            }
        }

        DBCResultSetMetaData metaData = resultSet.getMeta();
        List<DBCAttributeMetaData> rsAttributes = metaData.getAttributes();
        columnMappings = new ColumnMapping[rsAttributes.size()];
        targetAttributes = new ArrayList<>(columnMappings.length);
        for (int i = 0; i < rsAttributes.size(); i++) {
            ColumnMapping columnMapping = new ColumnMapping(rsAttributes.get(i));
            columnMapping.targetAttr = containerMapping.getAttributeMapping(columnMapping.sourceAttr);
            if (columnMapping.targetAttr == null) {
                throw new DBCException("Can't find target attribute [" + columnMapping.sourceAttr.getName() + "]");
            }
            columnMapping.sourceValueHandler = DBUtils.findValueHandler(session, columnMapping.sourceAttr);
            columnMappings[i] = columnMapping;
            if (columnMapping.targetAttr.getMappingType() == DatabaseMappingType.skip) {
                continue;
            }
            if (columnMapping.targetAttr.getTarget() == null) {
                throw new DBCException("Target attribute for [" + columnMapping.sourceAttr.getName() + "] wasn't resolved");
            }
            columnMapping.targetValueHandler = DBUtils.findValueHandler(session, columnMapping.targetAttr.getTarget());
            columnMapping.targetIndex = targetAttributes.size();
            targetAttributes.add(columnMappings[i].targetAttr.getTarget());
        }
        executeBatch = containerMapping.getTarget().insertData(
            targetSession,
            targetAttributes.toArray(new DBSAttributeBase[targetAttributes.size()]),
            null,
            new AbstractExecutionSource(sourceObject, targetContext, this));
    }

    @Override
    public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException
    {
        Object[] rowValues = new Object[targetAttributes.size()];
        for (int i = 0; i < columnMappings.length; i++) {
            ColumnMapping column = columnMappings[i];
            if (column.targetIndex < 0) {
                continue;
            }
            final Object attrValue = column.sourceValueHandler.fetchValueObject(session, resultSet, column.sourceAttr, i);
            rowValues[column.targetIndex] = column.targetValueHandler.getValueFromObject(
                session,
                column.targetAttr.getTarget(),
                attrValue,
                false);
        }
        executeBatch.add(rowValues);

        rowsExported++;

        insertBatch(false);
    }

    private void insertBatch(boolean force) throws DBCException
    {
        boolean needCommit = force || ((rowsExported % settings.getCommitAfterRows()) == 0);
        if (needCommit && executeBatch != null) {
            boolean retryInsert;
            do {
                retryInsert = false;
                try {
                    executeBatch.execute(targetSession);
                } catch (Throwable e) {
                    log.error("Error inserting row", e);
                    if (!ignoreErrors) {
                        ExecutionQueueErrorResponse response = ExecutionQueueErrorJob.showError(
                            DBUtils.getObjectFullName(containerMapping.getTarget(), DBPEvaluationContext.UI) + " data load",
                            e,
                            true);
                        switch (response) {
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
            if (txnManager != null && !txnManager.isAutoCommit()) {
                txnManager.commit(targetSession);
            }
        }
    }

    @Override
    public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException
    {
        if (rowsExported > 0) {
            insertBatch(true);
        }
        if (executeBatch != null) {
            executeBatch.close();
            executeBatch = null;
        }

        closeExporter();
    }

    @Override
    public void close()
    {
    }

    private void initExporter(DBRProgressMonitor monitor) throws DBCException
    {
        containerMapping = settings.getDataMapping(sourceObject);
        if (containerMapping == null) {
            throw new DBCException("Can't find container mapping for " + DBUtils.getObjectFullName(sourceObject, DBPEvaluationContext.UI));
        }
        DBPDataSource dataSource = containerMapping.getTarget().getDataSource();
        assert (dataSource != null);
        try {
            targetContext = settings.isOpenNewConnections() ?
                dataSource.openIsolatedContext(monitor, "Data transfer consumer") : dataSource.getDefaultContext(false);
        } catch (DBException e) {
            throw new DBCException("Error opening new connection", e);
        }
        targetSession = targetContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Data load");
        targetSession.enableLogging(false);
        if (settings.isUseTransactions()) {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(targetSession.getExecutionContext());
            if (txnManager != null) {
                txnManager.setAutoCommit(monitor, false);
            }
        }
    }

    private void closeExporter()
    {
        if (targetSession != null) {
            targetSession.close();
            targetSession = null;
        }
        if (targetContext != null && settings.isOpenNewConnections()) {
            targetContext.close();
            targetContext = null;
        }
    }

    @Override
    public void initTransfer(DBSObject sourceObject, DatabaseConsumerSettings settings, boolean isBinary, IDataTransferProcessor processor, Map<Object, Object> processorProperties)
    {
        this.sourceObject = (DBSDataContainer)sourceObject;
        this.settings = settings;
    }

    @Override
    public void startTransfer(DBRProgressMonitor monitor) throws DBException
    {
        // Create all necessary database objects
        monitor.beginTask("Create necessary database objects", 1);
        try {
            DBSObjectContainer container = settings.getContainer();
            if (container == null) {
                throw new DBException("No target datasource");
            }

            boolean hasNewObjects = false;
            DBPDataSource dataSource = container.getDataSource();
            try (DBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Create target metadata")) {

                for (DatabaseMappingContainer containerMapping : settings.getDataMappings().values()) {
                    switch (containerMapping.getMappingType()) {
                        case create:
                            createTargetTable(session, containerMapping);
                            hasNewObjects = true;
                            break;
                        case existing:
                            for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
                                if (attr.getMappingType() == DatabaseMappingType.create) {
                                    createTargetAttribute(session, attr);
                                    hasNewObjects = true;
                                }
                            }
                            break;
                    }
                }
            }
            if (hasNewObjects) {
                // Refresh node
                monitor.subTask("Refresh navigator model");
                settings.getContainerNode().refreshNode(monitor, this);

                // Reflect database changes in mappings
                for (DatabaseMappingContainer containerMapping : settings.getDataMappings().values()) {
                    switch (containerMapping.getMappingType()) {
                        case create:
                            DBSObject newTarget = container.getChild(monitor, containerMapping.getTargetName());
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
                                    attr.updateMappingType(monitor);
                                    if (attr.getTarget() == null) {
                                        throw new DBCException("Can't find target attribute '" + attr.getTargetName() + "' in '" + containerMapping.getTargetName() + "'");

                                    }
                                }
                            }
                            break;
                    }
                }
            }
        }
        finally {
            monitor.done();
        }
    }

    private void createTargetTable(DBCSession session, DatabaseMappingContainer containerMapping) throws DBException
    {
        DBSObjectContainer schema = settings.getContainer();
        if (schema == null) {
            throw new DBException("No target container selected");
        }
        String sql = generateTargetTableDDL(session.getProgressMonitor(), session.getDataSource(), schema, containerMapping);
        try {
            executeDDL(session, sql);
        } catch (DBCException e) {
            throw new DBCException("Can't create target table:\n" + sql, e);
        }
    }

    public static String generateTargetTableDDL(DBRProgressMonitor monitor, DBPDataSource dataSource, DBSObjectContainer schema, DatabaseMappingContainer containerMapping) throws DBException
    {
        monitor.subTask("Create table " + containerMapping.getTargetName());
        StringBuilder sql = new StringBuilder(500);
        if (!(dataSource instanceof SQLDataSource)) {
            throw new DBException("Data source doesn't support SQL");
        }
        SQLDataSource targetDataSource = (SQLDataSource)dataSource;

        String tableName = DBObjectNameCaseTransformer.transformName(targetDataSource, containerMapping.getTargetName());
        containerMapping.setTargetName(tableName);
        sql.append("CREATE TABLE ");
        if (schema instanceof DBSSchema || schema instanceof DBSCatalog) {
            sql.append(DBUtils.getQuotedIdentifier(schema));
            sql.append(targetDataSource.getSQLDialect().getCatalogSeparator());
        }
        sql.append(DBUtils.getQuotedIdentifier(targetDataSource, tableName)).append("(\n");
        Map<DBSAttributeBase, DatabaseMappingAttribute> mappedAttrs = new HashMap<>();
        for (DatabaseMappingAttribute attr : containerMapping.getAttributeMappings(monitor)) {
            if (attr.getMappingType() != DatabaseMappingType.create) {
                continue;
            }
            if (!mappedAttrs.isEmpty()) sql.append(",\n");
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
                    sql.append(",\nPRIMARY KEY (");
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
        return sql.toString();
    }

    private static void appendAttributeClause(DBPDataSource dataSource, StringBuilder sql, DatabaseMappingAttribute attr)
    {
        sql.append(DBUtils.getQuotedIdentifier(dataSource, attr.getTargetName())).append(" ").append(attr.getTargetType(dataSource));
        if (SQLUtils.getDialectFromDataSource(dataSource).supportsNullability()) {
            if (attr.source.isRequired()) sql.append(" NOT NULL");
        }
    }

    private void createTargetAttribute(DBCSession session, DatabaseMappingAttribute attribute) throws DBCException
    {
        session.getProgressMonitor().subTask("Create column " + DBUtils.getObjectFullName(attribute.getParent().getTarget(), DBPEvaluationContext.DDL) + "." + attribute.getTargetName());
        StringBuilder sql = new StringBuilder(500);
        sql.append("ALTER TABLE ").append(DBUtils.getObjectFullName(attribute.getParent().getTarget(), DBPEvaluationContext.DDL))
            .append(" ADD ");
        appendAttributeClause(session.getDataSource(), sql, attribute);
        try {
            executeDDL(session, sql.toString());
        } catch (DBCException e) {
            throw new DBCException("Can't create target column:\n" + sql, e);
        }
    }

    private void executeDDL(DBCSession  session, String sql)
        throws DBCException
    {
        DBCStatement dbStat = DBUtils.makeStatement(session, sql, false);
        try {
            dbStat.executeStatement();
        } finally {
            dbStat.close();
        }
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
        if (txnManager != null && !txnManager.isAutoCommit()) {
            // Commit DDL changes
            txnManager.commit(session);
        }
    }

    @Override
    public void finishTransfer(DBRProgressMonitor monitor, boolean last)
    {
        if (!last && settings.isOpenTableOnFinish()) {
            if (containerMapping != null && containerMapping.getTarget() != null) {
                DBeaverUI.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        NavigatorHandlerObjectOpen.openEntityEditor(containerMapping.getTarget());
                    }
                });
            }
        }
    }

    public DBSDataManipulator getTargetObject()
    {
        return targetObject;
    }

    @Override
    public String getTargetName()
    {
        if (targetObject != null) {
            return DBUtils.getObjectFullName(targetObject, DBPEvaluationContext.UI);
        }
        DatabaseMappingContainer dataMapping = settings.getDataMapping(sourceObject);
        if (dataMapping == null) {
            return "?";
        }

        switch (dataMapping.getMappingType()) {
            case create: return dataMapping.getTargetName() + " [Create]";
            case existing: return dataMapping.getTargetName() + " [Insert]";
            case skip: return "[Skip]";
            default: return "?";
        }
    }

}
