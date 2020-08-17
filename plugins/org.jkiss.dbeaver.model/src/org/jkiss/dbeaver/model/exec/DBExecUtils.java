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
package org.jkiss.dbeaver.model.exec;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.net.DBWForwarder;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.InvalidateJob;
import org.jkiss.dbeaver.runtime.net.GlobalProxyAuthenticator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.util.*;

/**
 * Execution utils
 */
public class DBExecUtils {

    public static final int DEFAULT_READ_FETCH_SIZE = 10000;

    private static final Log log = Log.getLog(DBExecUtils.class);

    /**
     * Current execution context. Used by global authenticators and network handlers
     */
    private static final ThreadLocal<DBPDataSourceContainer> ACTIVE_CONTEXT = new ThreadLocal<>();
    private static final List<DBPDataSourceContainer> ACTIVE_CONTEXTS = new ArrayList<>();
    public static final boolean BROWSE_LAZY_ASSOCIATIONS = false;

    public static DBPDataSourceContainer getCurrentThreadContext() {
        return ACTIVE_CONTEXT.get();
    }

    public static List<DBPDataSourceContainer> getActiveContexts() {
        synchronized (ACTIVE_CONTEXTS) {
            return new ArrayList<>(ACTIVE_CONTEXTS);
        }
    }

    public static void startContextInitiation(DBPDataSourceContainer context) {
        ACTIVE_CONTEXT.set(context);
        synchronized (ACTIVE_CONTEXTS) {
            ACTIVE_CONTEXTS.add(context);
        }
        // Set proxy auth (if required)
        // Note: authenticator may be changed by Eclipse frameword on startup or later.
        // That's why we set new default authenticator on connection initiation
        boolean hasProxy = false;
        for (DBWHandlerConfiguration handler : context.getConnectionConfiguration().getHandlers()) {
            if (handler.isEnabled() && handler.getType() == DBWHandlerType.PROXY) {
                hasProxy = true;
                break;
            }
        }
        if (hasProxy) {
            Authenticator.setDefault(new GlobalProxyAuthenticator());
        }
    }

    public static void finishContextInitiation(DBPDataSourceContainer context) {
        ACTIVE_CONTEXT.remove();
        synchronized (ACTIVE_CONTEXTS) {
            ACTIVE_CONTEXTS.remove(context);
        }
    }

    public static DBPDataSourceContainer findConnectionContext(String host, int port, String path) {
        DBPDataSourceContainer curContext = getCurrentThreadContext();
        if (curContext != null) {
            return contextMatches(host, port, curContext) ? curContext : null;
        }
        synchronized (ACTIVE_CONTEXTS) {
            for (DBPDataSourceContainer ctx : ACTIVE_CONTEXTS) {
                if (contextMatches(host, port, ctx)) {
                    return ctx;
                }
            }
        }
        return null;
    }

    private static boolean contextMatches(String host, int port, DBPDataSourceContainer ctx) {
        DBPConnectionConfiguration cfg = ctx.getConnectionConfiguration();
        if (CommonUtils.equalObjects(cfg.getHostName(), host) && String.valueOf(port).equals(cfg.getHostPort())) {
            return true;
        }
        for (DBWNetworkHandler networkHandler : ctx.getActiveNetworkHandlers()) {
            if (networkHandler instanceof DBWForwarder && ((DBWForwarder) networkHandler).matchesParameters(host, port)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static DBPErrorAssistant.ErrorType discoverErrorType(@NotNull DBPDataSource dataSource, @NotNull Throwable error) {
        DBPErrorAssistant errorAssistant = DBUtils.getAdapter(DBPErrorAssistant.class, dataSource);
        if (errorAssistant != null) {
            return ((DBPErrorAssistant) dataSource).discoverErrorType(error);
        }

        return DBPErrorAssistant.ErrorType.NORMAL;
    }

    /**
     * @param param DBRProgressProgress monitor or DBCSession
     *
     */
    public static <T> boolean tryExecuteRecover(@NotNull T param, @NotNull DBPDataSource dataSource, @NotNull DBRRunnableParametrized<T> runnable) throws DBException {
        int tryCount = 1;
        boolean recoverEnabled = dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.EXECUTE_RECOVER_ENABLED);
        if (recoverEnabled) {
            tryCount += dataSource.getContainer().getPreferenceStore().getInt(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT);
        }
        Throwable lastError = null;
        for (int i = 0; i < tryCount; i++) {
            try {
                runnable.run(param);
                lastError = null;
                break;
            } catch (InvocationTargetException e) {
                lastError = e.getTargetException();
                if (!recoverEnabled) {
                    // Can't recover
                    break;
                }
                DBPErrorAssistant.ErrorType errorType = discoverErrorType(dataSource, lastError);
                if (errorType != DBPErrorAssistant.ErrorType.TRANSACTION_ABORTED && errorType != DBPErrorAssistant.ErrorType.CONNECTION_LOST) {
                    // Some other error
                    break;
                }
                DBRProgressMonitor monitor;
                if (param instanceof DBRProgressMonitor) {
                    monitor = (DBRProgressMonitor) param;
                } else if (param instanceof DBCSession) {
                    monitor = ((DBCSession) param).getProgressMonitor();
                } else {
                    monitor = new VoidProgressMonitor();
                }
                if (!monitor.isCanceled()) {

                    if (errorType == DBPErrorAssistant.ErrorType.TRANSACTION_ABORTED) {
                        // Transaction aborted
                        DBCExecutionContext executionContext = null;
                        if (lastError instanceof DBCException) {
                            executionContext = ((DBCException) lastError).getExecutionContext();
                        }
                        if (executionContext != null) {
                            log.debug("Invalidate context [" + executionContext.getDataSource().getContainer().getName() + "/" + executionContext.getContextName() + "] transactions");
                        } else {
                            log.debug("Invalidate datasource [" + dataSource.getContainer().getName() + "] transactions");
                        }
                        InvalidateJob.invalidateTransaction(monitor, dataSource, executionContext);
                    } else {
                        // Do not recover if connection was canceled
                        log.debug("Invalidate datasource '" + dataSource.getContainer().getName() + "' connections...");
                        InvalidateJob.invalidateDataSource(
                            monitor,
                            dataSource,
                            false,
                            true,
                            () -> DBWorkbench.getPlatformUI().openConnectionEditor(dataSource.getContainer()));
                        if (i < tryCount - 1) {
                            log.error("Operation failed. Retry count remains = " + (tryCount - i - 1), lastError);
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.error("Operation interrupted");
                return false;
            }
        }
        if (lastError != null) {
            if (lastError instanceof DBException) {
                throw (DBException) lastError;
            } else {
                throw new DBException(lastError, dataSource);
            }
        }
        return true;
    }

    public static void setStatementFetchSize(DBCStatement dbStat, long firstRow, long maxRows, int fetchSize) {
        boolean useFetchSize = fetchSize > 0 || dbStat.getSession().getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_USE_FETCH_SIZE);
        if (useFetchSize) {
            if (fetchSize <= 0) {
                fetchSize = DEFAULT_READ_FETCH_SIZE;
            }
            try {
                dbStat.setResultsFetchSize(
                    firstRow < 0 || maxRows <= 0 ? fetchSize : (int) (firstRow + maxRows));
            } catch (Exception e) {
                log.warn(e);
            }
        }
    }

    public static void executeScript(DBRProgressMonitor monitor, DBCExecutionContext executionContext, String jobName, List<DBEPersistAction> persistActions) {
        try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, jobName)) {
            executeScript(session, persistActions.toArray(new DBEPersistAction[0]));
        }
    }

    public static void executeScript(DBCSession session, DBEPersistAction[] persistActions) {
        DBRProgressMonitor monitor = session.getProgressMonitor();
        boolean ignoreErrors = false;
        monitor.beginTask(session.getTaskTitle(), persistActions.length);
        try {
            for (DBEPersistAction action : persistActions) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (!CommonUtils.isEmpty(action.getTitle())) {
                    monitor.subTask(action.getTitle());
                }
                try {
                    executePersistAction(session, action);
                } catch (Exception e) {
                    log.debug("Error executing query", e);
                    if (ignoreErrors) {
                        continue;
                    }
                    boolean keepRunning = true;
                    switch (DBWorkbench.getPlatformUI().showErrorStopRetryIgnore(session.getTaskTitle(), e, true)) {
                        case STOP:
                            keepRunning = false;
                            break;
                        case RETRY:
                            // just make it again
                            continue;
                        case IGNORE:
                            // Just do nothing
                            break;
                        case IGNORE_ALL:
                            ignoreErrors = true;
                            break;
                    }
                    if (!keepRunning) {
                        break;
                    }
                } finally {
                    monitor.worked(1);
                }
            }
        } finally {
            monitor.done();
        }
    }

    public static void executePersistAction(DBCSession session, DBEPersistAction action) throws DBCException {
        if (action instanceof SQLDatabasePersistActionComment) {
            return;
        }
        String script = action.getScript();
        if (script == null) {
            action.afterExecute(session, null);
        } else {
            DBCStatement dbStat = DBUtils.createStatement(session, script, false);
            try {
                action.beforeExecute(session);
                dbStat.executeStatement();
                action.afterExecute(session, null);
            } catch (DBCException e) {
                action.afterExecute(session, e);
                throw e;
            } finally {
                dbStat.close();
            }
        }
    }

    public static void checkSmartAutoCommit(DBCSession session, String queryText) {
        DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
        if (txnManager != null) {
            try {
                if (!txnManager.isAutoCommit()) {
                    return;
                }

                SQLDialect sqlDialect = SQLUtils.getDialectFromDataSource(session.getDataSource());
                if (!sqlDialect.isTransactionModifyingQuery(queryText)) {
                    return;
                }

                if (txnManager.isAutoCommit()) {
                    txnManager.setAutoCommit(session.getProgressMonitor(), false);
                }
            } catch (DBCException e) {
                log.warn(e);
            }
        }
    }

    public static void setExecutionContextDefaults(DBRProgressMonitor monitor, DBPDataSource dataSource, DBCExecutionContext executionContext, @Nullable String newInstanceName, @Nullable String curInstanceName, @Nullable String newObjectName) throws DBException {
        DBSObjectContainer rootContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (rootContainer == null) {
            return;
        }

        DBCExecutionContextDefaults contextDefaults = null;
        if (executionContext != null) {
            contextDefaults = executionContext.getContextDefaults();
        }
        if (contextDefaults != null && (contextDefaults.supportsSchemaChange() || contextDefaults.supportsCatalogChange())) {
            changeDefaultObject(monitor, rootContainer, contextDefaults, newInstanceName, curInstanceName, newObjectName);
        }
    }

    @SuppressWarnings("unchecked")
    public static void changeDefaultObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer rootContainer,
        @NotNull DBCExecutionContextDefaults contextDefaults,
        @Nullable String newCatalogName,
        @Nullable String curCatalogName,
        @Nullable String newObjectName) throws DBException
    {
        DBSCatalog newCatalog = null;
        DBSSchema newSchema = null;

        if (newCatalogName != null) {
            DBSObject newInstance = rootContainer.getChild(monitor, newCatalogName);
            if (newInstance instanceof DBSCatalog) {
                newCatalog = (DBSCatalog) newInstance;
            }
        }
        DBSObject newObject;
        if (newObjectName != null) {
            if (newCatalog == null) {
                newObject = rootContainer.getChild(monitor, newObjectName);
            } else {
                newObject = newCatalog.getChild(monitor, newObjectName);
            }
            if (newObject instanceof DBSSchema) {
                newSchema = (DBSSchema) newObject;
            } else if (newObject instanceof DBSCatalog) {
                newCatalog = (DBSCatalog) newObject;
            }
        }

        boolean changeCatalog = (curCatalogName != null ? !CommonUtils.equalObjects(curCatalogName, newCatalogName) : newCatalog != null);

        if (newCatalog != null && newSchema != null && changeCatalog) {
            contextDefaults.setDefaultCatalog(monitor, newCatalog, newSchema);
        } else if (newSchema != null) {
            contextDefaults.setDefaultSchema(monitor, newSchema);
        } else if (newCatalog != null && changeCatalog) {
            contextDefaults.setDefaultCatalog(monitor, newCatalog, null);
        }
    }

    public static void recoverSmartCommit(DBCExecutionContext executionContext) {
        DBPPreferenceStore preferenceStore = executionContext.getDataSource().getContainer().getPreferenceStore();
        if (preferenceStore.getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT) && preferenceStore.getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT_RECOVER)) {
            DBCTransactionManager transactionManager = DBUtils.getTransactionManager(executionContext);
            if (transactionManager != null) {
                new AbstractJob("Recover smart commit mode") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        try {
                            monitor.beginTask("Switch to auto-commit mode", 1);
                            if (!transactionManager.isAutoCommit()) {
                                transactionManager.setAutoCommit(monitor,true);
                            }
                        } catch (DBCException e) {
                            return GeneralUtils.makeExceptionStatus(e);
                        }
                        finally {
                            monitor.done();
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        }
    }

    public static DBSEntityConstraint getBestIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, DBDAttributeBinding[] bindings, boolean readMetaData)
        throws DBException
    {
        List<DBSEntityConstraint> identifiers = new ArrayList<>(2);
        //List<DBSEntityConstraint> nonIdentifyingConstraints = null;

        if (readMetaData) {
            if (table instanceof DBSTable && ((DBSTable) table).isView()) {
                // Skip physical identifiers for views. There are nothing anyway

            } else {
                // Check indexes first.
                if (table instanceof DBSTable) {
                    try {
                        Collection<? extends DBSTableIndex> indexes = ((DBSTable) table).getIndexes(monitor);
                        if (!CommonUtils.isEmpty(indexes)) {
                            // First search for primary index
                            for (DBSTableIndex index : indexes) {
                                if (index.isPrimary() && DBUtils.isIdentifierIndex(monitor, index)) {
                                    identifiers.add(index);
                                    break;
                                }
                            }
                            // Then search for unique index
                            for (DBSTableIndex index : indexes) {
                                if (DBUtils.isIdentifierIndex(monitor, index) && !identifiers.contains(index)) {
                                    identifiers.add(index);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Indexes are not supported or not available
                        // Just skip them
                        log.debug(e);
                    }
                }
                {
                    // Check constraints
                    Collection<? extends DBSEntityConstraint> constraints = table.getConstraints(monitor);
                    if (constraints != null) {
                        for (DBSEntityConstraint constraint : constraints) {
                            if (DBUtils.isIdentifierConstraint(monitor, constraint)) {
                                identifiers.add(constraint);
                            }/* else {
                                if (nonIdentifyingConstraints == null) nonIdentifyingConstraints = new ArrayList<>();
                                nonIdentifyingConstraints.add(constraint);
                            }*/
                        }
                    }
                }

            }
        }

        if (!CommonUtils.isEmpty(identifiers)) {
            // Find PK or unique key
            DBSEntityConstraint uniqueId = null;
            for (DBSEntityConstraint constraint : identifiers) {
                if (constraint instanceof DBSEntityReferrer) {
                    DBSEntityReferrer referrer = (DBSEntityReferrer) constraint;
                    if (isGoodReferrer(monitor, bindings, referrer)) {
                        if (referrer.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                            return referrer;
                        } else if (uniqueId == null &&
                            (referrer.getConstraintType().isUnique() ||
                                (referrer instanceof DBSTableIndex && ((DBSTableIndex) referrer).isUnique()))) {
                            uniqueId = referrer;
                        }
                    }
                } else {
                    uniqueId = constraint;
                }
            }
            if (uniqueId != null) {
                return uniqueId;
            }
        }

        {
            // Check for pseudo attrs (ROWID)
            // Do this after natural identifiers search (see #3829)
            for (DBDAttributeBinding column : bindings) {
                DBDPseudoAttribute pseudoAttribute = column instanceof DBDAttributeBindingMeta ? ((DBDAttributeBindingMeta) column).getPseudoAttribute() : null;
                if (pseudoAttribute != null && pseudoAttribute.getType() == DBDPseudoAttributeType.ROWID) {
                    identifiers.add(new DBDPseudoReferrer(table, column));
                    break;
                }
            }
        }

        // No physical identifiers or row ids
        // Make new or use existing virtual identifier
        DBVEntity virtualEntity = DBVUtils.getVirtualEntity(table, true);
        return virtualEntity.getBestIdentifier();
    }

    private static boolean isGoodReferrer(DBRProgressMonitor monitor, DBDAttributeBinding[] bindings, DBSEntityReferrer referrer) throws DBException
    {
        if (referrer instanceof DBDPseudoReferrer) {
            return true;
        }
        Collection<? extends DBSEntityAttributeRef> references = referrer.getAttributeReferences(monitor);
        if (references == null || references.isEmpty()) {
            return referrer instanceof DBVEntityConstraint;
        }
        for (DBSEntityAttributeRef ref : references) {
            boolean refMatches = false;
            for (DBDAttributeBinding binding : bindings) {
                if (binding.matches(ref.getAttribute(), false)) {
                    refMatches = true;
                    break;
                }
            }
            if (!refMatches) {
                return false;
            }
        }
        return true;
    }

    public static DBSEntityAssociation getAssociationByAttribute(DBDAttributeBinding attr) throws DBException {
        List<DBSEntityReferrer> referrers = attr.getReferrers();
        if (referrers != null) {
            for (final DBSEntityReferrer referrer : referrers) {
                if (referrer instanceof DBSEntityAssociation) {
                    return (DBSEntityAssociation) referrer;
                }
            }
        }
        throw new DBException("Association not found in attribute [" + attr.getName() + "]");
    }

    public static boolean equalAttributes(DBCAttributeMetaData attr1, DBCAttributeMetaData attr2) {
        return
            attr1 != null && attr2 != null &&
            SQLUtils.compareAliases(attr1.getLabel(), attr2.getLabel()) &&
            SQLUtils.compareAliases(attr1.getName(), attr2.getName()) &&
            CommonUtils.equalObjects(attr1.getEntityMetaData(), attr2.getEntityMetaData()) &&
            attr1.getOrdinalPosition() == attr2.getOrdinalPosition() &&
            attr1.isRequired() == attr2.isRequired() &&
            attr1.getMaxLength() == attr2.getMaxLength() &&
            CommonUtils.equalObjects(attr1.getPrecision(), attr2.getPrecision()) &&
            CommonUtils.equalObjects(attr1.getScale(), attr2.getScale()) &&
            attr1.getTypeID() == attr2.getTypeID() &&
            CommonUtils.equalObjects(attr1.getTypeName(), attr2.getTypeName());
    }

    public static double makeNumericValue(Object value) {
        if (value == null) {
            return 0;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof Date) {
            return ((Date) value).getTime();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        } else {
            return 0;
        }
    }

    public static void bindAttributes(
        @NotNull DBCSession session,
        @Nullable DBSEntity sourceEntity,
        @Nullable DBCResultSet resultSet,
        @NotNull DBDAttributeBinding[] bindings,
        @Nullable List<Object[]> rows) throws DBException
    {
        final DBRProgressMonitor monitor = session.getProgressMonitor();
        final DBPDataSource dataSource = session.getDataSource();
        boolean readMetaData = dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_READ_METADATA);
        if (!readMetaData && sourceEntity == null) {
            // Do not read metadata if source entity is not known
            return;
        }
        boolean readReferences = dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_READ_REFERENCES);

        final Map<DBCEntityMetaData, DBSEntity> entityBindingMap = new IdentityHashMap<>();

        monitor.beginTask("Discover resultset metadata", 3);
        try {
            SQLQuery sqlQuery = null;
            DBSEntity entity = null;
            if (sourceEntity != null) {
                entity = sourceEntity;
            } else if (resultSet != null) {
                DBCStatement sourceStatement = resultSet.getSourceStatement();
                if (sourceStatement != null && sourceStatement.getStatementSource() != null) {
                    DBCExecutionSource executionSource = sourceStatement.getStatementSource();

                    monitor.subTask("Discover owner entity");
                    DBSDataContainer dataContainer = executionSource.getDataContainer();
                    if (dataContainer instanceof DBSEntity) {
                        entity = (DBSEntity) dataContainer;
                    }
                    DBCEntityMetaData entityMeta = null;
                    if (entity == null) {
                        // Discover from entity metadata
                        Object sourceDescriptor = executionSource.getSourceDescriptor();
                        if (sourceDescriptor instanceof SQLQuery) {
                            sqlQuery = (SQLQuery) sourceDescriptor;
                            entityMeta = sqlQuery.getSingleSource();
                        }
                        if (entityMeta != null) {
                            entity = DBUtils.getEntityFromMetaData(monitor, session.getExecutionContext(), entityMeta);
                            if (entity != null) {
                                entityBindingMap.put(entityMeta, entity);
                            }
                        }
                    }
                }
            }

            final Map<DBSEntity, DBDRowIdentifier> locatorMap = new IdentityHashMap<>();

            monitor.subTask("Discover attributes");
            for (DBDAttributeBinding binding : bindings) {
                monitor.subTask("Discover attribute '" + binding.getName() + "'");
                DBCAttributeMetaData attrMeta = binding.getMetaAttribute();
                if (attrMeta == null) {
                    continue;
                }
                // We got table name and column name
                // To be editable we need this resultset contain set of columns from the same table
                // which construct any unique key
                DBSEntity attrEntity = null;
                final DBCEntityMetaData attrEntityMeta = attrMeta.getEntityMetaData();
                if (attrEntityMeta != null) {
                    attrEntity = entityBindingMap.get(attrEntityMeta);
                    if (attrEntity == null) {
                        if (entity != null && entity instanceof DBSTable && ((DBSTable) entity).isView()) {
                            // If this is a view then don't try to detect entity for each attribute
                            // MySQL returns source table name instead of view name. That's crazy.
                            attrEntity = entity;
                        } else {
                            attrEntity = DBUtils.getEntityFromMetaData(monitor, session.getExecutionContext(), attrEntityMeta);
                        }
                    }
                    if (attrEntity != null) {
                        entityBindingMap.put(attrEntityMeta, attrEntity);
                    }
                }
                if (attrEntity == null) {
                    attrEntity = entity;
                }
                if (attrEntity == null) {
                    if (attrEntityMeta != null) {
                        log.debug("Table '" + DBUtils.getSimpleQualifiedName(attrEntityMeta.getCatalogName(), attrEntityMeta.getSchemaName(), attrEntityMeta.getEntityName()) + "' not found in metadata catalog");
                    }
                } else if (binding instanceof DBDAttributeBindingMeta){
                    DBDAttributeBindingMeta bindingMeta = (DBDAttributeBindingMeta) binding;
                    DBDPseudoAttribute pseudoAttribute = DBUtils.getPseudoAttribute(attrEntity, attrMeta.getName());
                    if (pseudoAttribute != null) {
                        bindingMeta.setPseudoAttribute(pseudoAttribute);
                    }

                    DBSEntityAttribute tableColumn;
                    if (bindingMeta.getPseudoAttribute() != null) {
                        tableColumn = bindingMeta.getPseudoAttribute().createFakeAttribute(attrEntity, attrMeta);
                    } else {
                        tableColumn = attrEntity.getAttribute(monitor, attrMeta.getName());
                    }

                    if (tableColumn != null &&
                        // Table column can be found from results metadata or from SQL query parser
                        // If datasource supports table names in result metadata then table name must present in results metadata.
                        // Otherwise it is an expression.

                        // It is a real table columns if:
                        //  - We use some explicit entity (e.g. table data editor)
                        //  - Table metadata was specified for column
                        //  - Database doesn't support column name collisions (default)
                        (sourceEntity != null || bindingMeta.getMetaAttribute().getEntityMetaData() != null || !bindingMeta.getDataSource().getInfo().needsTableMetaForColumnResolution()) &&
                        bindingMeta.setEntityAttribute(
                            tableColumn,
                            ((sqlQuery == null || tableColumn.getTypeID() != attrMeta.getTypeID()) && rows != null)))
                    {
                        // We have new type and new value handler.
                        // We have to fix already fetched values.
                        // E.g. we fetched strings and found out that we should handle them as LOBs or enums.
                        try {
                            int pos = attrMeta.getOrdinalPosition();
                            for (Object[] row : rows) {
                                row[pos] = binding.getValueHandler().getValueFromObject(session, tableColumn, row[pos], false, false);
                            }
                        } catch (DBCException e) {
                            log.warn("Error resolving attribute '" + binding.getName() + "' values", e);
                        }
                    }
                }
            }
            monitor.worked(1);

            {
                // Init row identifiers
                monitor.subTask("Detect unique identifiers");
                for (DBDAttributeBinding binding : bindings) {
                    if (!(binding instanceof DBDAttributeBindingMeta)) {
                        continue;
                    }
                    DBDAttributeBindingMeta bindingMeta = (DBDAttributeBindingMeta) binding;
                    //monitor.subTask("Find attribute '" + binding.getName() + "' identifier");
                    DBSEntityAttribute attr = binding.getEntityAttribute();
                    if (attr == null) {
                        bindingMeta.setRowIdentifierStatus("No corresponding table column");
                        continue;
                    }
                    DBSEntity attrEntity = attr.getParentObject();
                    if (attrEntity != null) {
                        DBDRowIdentifier rowIdentifier = locatorMap.get(attrEntity);
                        if (rowIdentifier == null) {
                            DBSEntityConstraint entityIdentifier = getBestIdentifier(monitor, attrEntity, bindings, readMetaData);
                            if (entityIdentifier != null) {
                                rowIdentifier = new DBDRowIdentifier(
                                    attrEntity,
                                    entityIdentifier);
                                locatorMap.put(attrEntity, rowIdentifier);
                            } else {
                                bindingMeta.setRowIdentifierStatus("Cannot determine unique row identifier");
                            }
                        }
                        bindingMeta.setRowIdentifier(rowIdentifier);
                    }
                }
                monitor.worked(1);
            }

            if (readMetaData && readReferences && rows != null) {
                monitor.subTask("Read results metadata");
                // Read nested bindings
                for (DBDAttributeBinding binding : bindings) {
                    binding.lateBinding(session, rows);
                }
            }

/*
            monitor.subTask("Load transformers");
            // Load transformers
            for (DBDAttributeBinding binding : bindings) {
                binding.loadTransformers(session, rows);
            }
*/

            monitor.subTask("Complete metadata load");
            // Reload attributes in row identifiers
            for (DBDRowIdentifier rowIdentifier : locatorMap.values()) {
                rowIdentifier.reloadAttributes(monitor, bindings);
            }
        }
        finally {
            monitor.done();
        }
    }

    public static boolean isAttributeReadOnly(@NotNull DBDAttributeBinding attribute) {
        if (attribute == null || attribute.getMetaAttribute() == null || attribute.getMetaAttribute().isReadOnly()) {
            return true;
        }
        DBDRowIdentifier rowIdentifier = attribute.getRowIdentifier();
        if (rowIdentifier == null || !(rowIdentifier.getEntity() instanceof DBSDataManipulator)) {
            return true;
        }
        DBSDataManipulator dataContainer = (DBSDataManipulator) rowIdentifier.getEntity();
        return (dataContainer.getSupportedFeatures() & DBSDataManipulator.DATA_UPDATE) == 0;
    }

    public static String getAttributeReadOnlyStatus(@NotNull DBDAttributeBinding attribute) {
        if (attribute == null || attribute.getMetaAttribute() == null) {
            return "Null meta attribute";
        }
        if (attribute.getMetaAttribute().isReadOnly()) {
            return "Attribute is read-only";
        }
        DBDRowIdentifier rowIdentifier = attribute.getRowIdentifier();
        if (rowIdentifier == null) {
            String status = attribute.getRowIdentifierStatus();
            return status != null ? status : "No row identifier found";
        }
        DBSEntity dataContainer = rowIdentifier.getEntity();
        if (!(dataContainer instanceof DBSDataManipulator)) {
            return "Underlying entity doesn't support data modification";
        }
        if ((((DBSDataManipulator) dataContainer).getSupportedFeatures() & DBSDataManipulator.DATA_UPDATE) == 0) {
            return "Underlying entity doesn't support data update";
        }
        return null;
    }

}