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
package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.utils.CommonUtils;

/**
 * Data source information
 */
public class QMMSessionInfo extends QMMObject {

    private final DBPProject project;
    private final String containerId;
    private final String driverId;
    private String containerName;
    @Nullable
    private DBPConnectionConfiguration connectionConfiguration;
    private String instanceId;
    private String contextName;
    @Nullable
    private SQLDialect sqlDialect;
    private boolean transactional;

    private QMMStatementInfo statementStack;
    private QMMStatementExecuteInfo executionStack;
    private QMMTransactionInfo transaction;
    //private Throwable stack;

    public QMMSessionInfo(DBCExecutionContext context, boolean transactional) {
        this.project = context.getDataSource().getContainer().getProject();
        this.containerId = context.getDataSource().getContainer().getId();
        this.driverId = context.getDataSource().getContainer().getDriver().getId();

        initFromContext(context, transactional);
    }

    private void initFromContext(DBCExecutionContext context, boolean transactional) {
        this.containerName = context.getDataSource().getContainer().getName();
        this.connectionConfiguration = context.getDataSource().getContainer().getConnectionConfiguration();
        this.instanceId = context.getOwnerInstance().getName();
        this.contextName = context.getContextName();
        this.sqlDialect = context.getDataSource().getSQLDialect();
        this.transactional = transactional;
        if (transactional) {
            this.transaction = new QMMTransactionInfo(this, null);
        }
    }

    public QMMSessionInfo(long openTime, long closeTime, String containerId, String containerName, String driverId, DBPConnectionConfiguration connectionConfiguration, String instanceID, String contextName, boolean transactional) {
        super(openTime, closeTime);
        this.project = null;
        this.containerId = containerId;
        this.containerName = containerName;
        this.driverId = driverId;
        this.connectionConfiguration = connectionConfiguration;
        this.instanceId = instanceID;
        this.contextName = contextName;
        this.transactional = transactional;
    }

    @Override
    public void close()
    {
        if (transaction != null) {
            transaction.rollback(null);
        }
        for (QMMStatementInfo stat = statementStack; stat != null; stat = stat.getPrevious()) {
            if (!stat.isClosed()) {
                DBCStatement statRef = stat.getReference();
                String query = statRef == null ? "?" : statRef.getQueryString();
                log.warn("Statement " + stat.getObjectId() + " (" + query + ") is not closed");
                stat.close();
            }
        }
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

    public QMMTransactionInfo changeTransactional(boolean transactional)
    {
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
        if (execution != null) {
            if (execution.getUpdateRowCount() < 0) {
                execution.close(rows, null);
            }
        }
        for (QMMStatementInfo stat = this.statementStack; stat != null; stat = stat.getPrevious()) {
            if (stat.getReference() == statement) {
                stat.close();
                return stat;
            }
        }
        log.warn("Statement " + statement + " meta info not found");
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
            return this.executionStack = new QMMStatementExecuteInfo(
                stat,
                savepoint,
                queryString,
                this.executionStack);
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

    public DBPProject getProject() {
        return project;
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

    public DBPConnectionConfiguration getConnectionConfiguration() {
        return connectionConfiguration;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getContextName() {
        return contextName;
    }

    public QMMStatementInfo getStatementStack()
    {
        return statementStack;
    }

    public QMMTransactionInfo getTransaction()
    {
        return transaction;
    }

    public boolean isTransactional()
    {
        return transactional;
    }

    public SQLDialect getSQLDialect() {
        return sqlDialect;
    }

    @Override
    public String toString()
    {
        return "SESSION " + containerName + " [" + contextName + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof QMMSessionInfo)) {
            return false;
        }
        QMMSessionInfo si = (QMMSessionInfo)obj;
        return
            CommonUtils.equalObjects(containerId, si.containerId) &&
            CommonUtils.equalObjects(contextName, si.contextName);
    }
}
