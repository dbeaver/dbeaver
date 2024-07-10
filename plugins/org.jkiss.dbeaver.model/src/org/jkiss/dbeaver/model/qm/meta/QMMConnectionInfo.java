/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.utils.CommonUtils;

/**
 * Data source information
 */
public class QMMConnectionInfo extends QMMObject {

    @Include
    private final QMMProjectInfo projectInfo;
    @Include
    private final String containerId;

    private String containerName;
    private final String driverId;
    private String connectionUserName;
    private String connectionUrl;
    @Include
    private String instanceId;
    @Include
    private String contextName;
    private boolean transactional;

    private transient QMMStatementInfo statementStack;
    private transient QMMStatementExecuteInfo executionStack;
    private transient QMMTransactionInfo transaction;
    //private Throwable stack;

    public QMMConnectionInfo(DBCExecutionContext context, boolean transactional) {
        super(QMMetaObjectType.CONNECTION_INFO);
        this.containerId = context.getDataSource().getContainer().getId();
        this.driverId = context.getDataSource().getContainer().getDriver().getFullId();

        this.projectInfo = new QMMProjectInfo(context.getDataSource().getContainer().getProject());
        initFromContext(context, transactional);
    }

    private QMMConnectionInfo(Builder builder) {
        super(QMMetaObjectType.CONNECTION_INFO, builder.openTime, builder.closeTime);
        projectInfo = builder.projectInfo;
        containerId = builder.containerId;
        driverId = builder.driverId;
        containerName = builder.containerName;
        connectionUserName = builder.connectionUserName;
        connectionUrl = builder.connectionUrl;
        instanceId = builder.instanceId;
        contextName = builder.contextName;
        transactional = builder.transactional;
        statementStack = builder.statementStack;
        executionStack = builder.executionStack;
        transaction = builder.transaction;
    }

    private void initFromContext(DBCExecutionContext context, boolean transactional) {
        this.containerName = context.getDataSource().getContainer().getName();
        var connectionConfiguration = context.getDataSource().getContainer().getConnectionConfiguration();
        this.connectionUserName = connectionConfiguration.getUserName();
        this.connectionUrl = connectionConfiguration.getUrl();
        this.instanceId = context.getOwnerInstance().getName();
        this.contextName = context.getContextName();
        this.transactional = transactional;
        if (transactional) {
            this.transaction = new QMMTransactionInfo(this, null);
        }
    }

    public QMMConnectionInfo(
        long openTime,
        long closeTime,
        QMMProjectInfo projectInfo,
        String containerId,
        String containerName,
        String driverId,
        DBPConnectionConfiguration connectionConfiguration,
        String instanceID,
        String contextName,
        boolean transactional)
    {
        super(QMMetaObjectType.CONNECTION_INFO, openTime, closeTime);
        this.projectInfo = projectInfo;
        this.containerId = containerId;
        this.containerName = containerName;
        this.driverId = driverId;
        this.connectionUserName = connectionConfiguration.getUserName();
        this.connectionUrl = connectionConfiguration.getUrl();
        this.instanceId = instanceID;
        this.contextName = contextName;
        this.transactional = transactional;
    }

    @Override
    public void close()
    {
        if (transaction != null) {
            transaction.rollback(null);
            transaction = null;
        }
        for (QMMStatementInfo stat = statementStack; stat != null; stat = stat.getPrevious()) {
            if (!stat.isClosed()) {
                DBCStatement statRef = stat.getReference();
                String query = statRef == null ? "?" : statRef.getQueryString();
                log.warn("Statement " + stat.getObjectId() + " (" + query + ") is not closed");
                stat.close();
            }
        }
        statementStack = null;
        super.close();
    }

    public void reopen(DBCExecutionContext context) {
        initFromContext(context, transactional);
        super.reopen();
    }

    @Override
    public String getText() {
        return this.containerName + " - " + contextName;
    }

    @Override
    public QMMConnectionInfo getConnection() {
        return this;
    }

    public QMMTransactionInfo changeTransactional(boolean transactional) {
        if (this.transactional == transactional) {
            return null;
        }
        this.transactional = transactional;
        if (this.transaction != null) {
            // Commit current transaction
            this.transaction.commit();
        }
        // start new transaction
        this.transaction = new QMMTransactionInfo(this, this.transaction);
        return this.transaction.getPrevious();
    }

    public QMMTransactionInfo commit()
    {
        if (this.transactional) {
            if (this.transaction != null) {
                this.transaction.commit();
            }
            this.transaction = new QMMTransactionInfo(this, this.transaction);
            return this.transaction.getPrevious();
        }
        return null;
    }

    public QMMObject rollback(DBCSavepoint savepoint)
    {
        if (this.transactional) {
            if (this.transaction != null) {
                this.transaction.rollback(savepoint);
            }
            if (savepoint == null) {
                this.transaction = new QMMTransactionInfo(this, this.transaction);
                return this.transaction.getPrevious();
            } else {
                if (this.transaction != null) {
                    return this.transaction.getSavepoint(savepoint);
                }
            }
        }
        return null;
    }

    public QMMStatementInfo openStatement(DBCStatement statement)
    {
        return this.statementStack = new QMMStatementInfo(this, statement, this.statementStack);
    }

    public QMMStatementInfo closeStatement(DBCStatement statement, long rows)
    {
        QMMStatementExecuteInfo execution = getExecution(statement);
        if (execution != null && execution.getUpdateRowCount() < 0) {
            execution.close(rows, null);
        }
        for (QMMStatementInfo stat = this.statementStack; stat != null; stat = stat.getPrevious()) {
            if (stat.getReference() == statement) {
                stat.close();
                return stat;
            }
        }
        if (statementStack != null) {
            log.warn("Closed statement " + statement + " meta info not found");
        }
        return null;
    }

    public QMMStatementInfo getStatement(DBCStatement statement)
    {
        for (QMMStatementInfo stat = this.statementStack; stat != null; stat = stat.getPrevious()) {
            if (stat.getReference() == statement) {
                return stat;
            }
        }
        log.warn("Statement " + statement + " meta info not found");
        return null;
    }

    public QMMStatementExecuteInfo getExecution(DBCStatement statement)
    {
        for (QMMStatementExecuteInfo exec = this.executionStack; exec != null; exec = exec.getPrevious()) {
            if (exec.getStatement().getReference() == statement) {
                return exec;
            }
        }
        return null;
    }

    public QMMStatementExecuteInfo beginExecution(DBCStatement statement)
    {
        QMMStatementInfo stat = getStatement(statement);
        if (stat != null) {
            String queryString = statement instanceof DBCParameterizedStatement ?
                ((DBCParameterizedStatement) statement).getFormattedQuery() : statement.getQueryString();
            final QMMTransactionSavepointInfo savepoint =
                isTransactional() && getTransaction() != null ?
                    getTransaction().getCurrentSavepoint() : null;
            var sqlDialect = statement.getSession().getDataSource().getSQLDialect();
            String schema = null;
            String catalog = null;
            DBCExecutionContextDefaults contextDefaults = statement.getSession().getExecutionContext().getContextDefaults();
            if (contextDefaults != null) {
                DBCCachedContextDefaults cachedDefault = contextDefaults.getCachedDefault();
                schema = cachedDefault.schemaName();
                catalog = cachedDefault.catalogName();
            }
            return this.executionStack = new QMMStatementExecuteInfo(
                stat,
                savepoint,
                queryString,
                this.executionStack,
                sqlDialect,
                schema,
                catalog
            );
        } else {
            return null;
        }
    }

    public QMMStatementExecuteInfo endExecution(DBCStatement statement, long rowCount, Throwable error)
    {
        QMMStatementExecuteInfo exec = getExecution(statement);
        if (exec != null) {
            exec.close(rowCount, error);
        }
        return exec;
    }

    public QMMStatementExecuteInfo beginFetch(DBCResultSet resultSet)
    {
        QMMStatementExecuteInfo exec = getExecution(resultSet.getSourceStatement());
        if (exec == null) {
            exec = beginExecution(resultSet.getSourceStatement());
        }
        if (exec != null) {
            exec.beginFetch();
        }
        return exec;
    }

    public QMMStatementExecuteInfo endFetch(DBCResultSet resultSet, long rowCount)
    {
        QMMStatementExecuteInfo exec = getExecution(resultSet.getSourceStatement());
        if (exec != null) {
            exec.endFetch(rowCount);
        }
        return exec;
    }

    public QMMProjectInfo getProjectInfo() {
        return projectInfo;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getContextName() {
        return contextName;
    }

    public QMMStatementInfo getStatementStack() {
        return statementStack;
    }

    public QMMStatementExecuteInfo getExecutionStack() {
        return executionStack;
    }

    public QMMTransactionInfo getTransaction()
    {
        return transaction;
    }

    public boolean isTransactional()
    {
        return transactional;
    }

    public String getConnectionUserName() {
        return connectionUserName;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    @Override
    public String toString()
    {
        return "SESSION " + containerName + " [" + contextName + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof QMMConnectionInfo)) {
            return false;
        }
        QMMConnectionInfo si = (QMMConnectionInfo) obj;
        return
            CommonUtils.equalObjects(containerId, si.containerId) &&
            CommonUtils.equalObjects(contextName, si.contextName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private QMMProjectInfo projectInfo;
        private String containerId;
        private String driverId;
        private String containerName;
        private String connectionUserName;
        private String connectionUrl;
        private String instanceId;
        private long openTime;
        private long closeTime;
        private String contextName;
        private boolean transactional;
        private QMMStatementInfo statementStack;
        private QMMStatementExecuteInfo executionStack;
        private QMMTransactionInfo transaction;

        public Builder() {
        }

        public Builder setProjectInfo(QMMProjectInfo projectInfo) {
            this.projectInfo = projectInfo;
            return this;
        }

        public Builder setContainerId(String containerId) {
            this.containerId = containerId;
            return this;
        }

        public Builder setDriverId(String driverId) {
            this.driverId = driverId;
            return this;
        }

        public Builder setContainerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Builder setConnectionUserName(String connectionUserName) {
            this.connectionUserName = connectionUserName;
            return this;
        }

        public Builder setConnectionUrl(String url) {
            this.connectionUrl = url;
            return this;
        }

        public Builder setInstanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder setOpenTime(long openTime) {
            this.openTime = openTime;
            return this;
        }

        public Builder setCloseTime(long closeTime) {
            this.closeTime = closeTime;
            return this;
        }

        public Builder setContextName(String contextName) {
            this.contextName = contextName;
            return this;
        }

        public Builder setTransactional(boolean transactional) {
            this.transactional = transactional;
            return this;
        }

        public Builder setStatementStack(QMMStatementInfo statementStack) {
            this.statementStack = statementStack;
            return this;
        }

        public Builder setExecutionStack(QMMStatementExecuteInfo executionStack) {
            this.executionStack = executionStack;
            return this;
        }

        public Builder setTransaction(QMMTransactionInfo transaction) {
            this.transaction = transaction;
            return this;
        }

        public QMMConnectionInfo build() {
            return new QMMConnectionInfo(this);
        }
    }
}
