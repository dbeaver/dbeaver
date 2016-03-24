/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;

/**
 * Data source information
 */
public class QMMSessionInfo extends QMMObject {

    private String containerId;
    private String containerName;
    private String driverId;
    private DBPConnectionConfiguration connectionConfiguration;
    private String contextName;
    private SQLDialect sqlDialect;
    private boolean transactional;

    private QMMSessionInfo previous;
    private QMMStatementInfo statementStack;
    private QMMStatementExecuteInfo executionStack;
    private QMMTransactionInfo transaction;

    public QMMSessionInfo(DBCExecutionContext context, boolean transactional, QMMSessionInfo previous)
    {
        this.containerId = context.getDataSource().getContainer().getId();
        this.containerName = context.getDataSource().getContainer().getName();
        this.driverId = context.getDataSource().getContainer().getDriver().getId();
        this.connectionConfiguration = context.getDataSource().getContainer().getConnectionConfiguration();
        this.contextName = context.getContextName();
        if (context.getDataSource() instanceof SQLDataSource) {
            this.sqlDialect = ((SQLDataSource) context.getDataSource()).getSQLDialect();
        }
        this.previous = previous;
        this.transactional = transactional;
        if (transactional) {
            this.transaction = new QMMTransactionInfo(this, null);
        }
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
            if (execution.getRowCount() == -1) {
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
        log.warn("Statement " + statement + " execution meta info not found");
        return null;
    }

    public QMMStatementExecuteInfo beginExecution(DBCStatement statement)
    {
        QMMStatementInfo stat = getStatement(statement);
        if (stat != null) {
            String queryString = statement instanceof JDBCPreparedStatement ?
                ((JDBCPreparedStatement) statement).getFormattedQuery() : statement.getQueryString();
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

    public String getContainerId()
    {
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

    public QMMSessionInfo getPrevious()
    {
        return previous;
    }

    public boolean isTransactional()
    {
        return transactional;
    }

    @Override
    public String toString()
    {
        return "SESSION " + containerId;
    }

    public SQLDialect getSQLDialect() {
        return sqlDialect;
    }
}
