/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.lang.ref.SoftReference;

/**
 * Data source information
 */
public class QMMSessionInfo extends QMMObject {

    private final String containerId;
    private SoftReference<DBPDataSource> reference;
    private SoftReference<DBSDataSourceContainer> container;
    private boolean transactional;

    private QMMSessionInfo previous;
    private QMMStatementInfo statementStack;
    private QMMStatementExecuteInfo executionStack;
    private QMMTransactionInfo transaction;

    QMMSessionInfo(DBPDataSource reference, boolean transactional, QMMSessionInfo previous)
    {
        this.containerId = reference.getContainer().getId();
        this.reference = new SoftReference<DBPDataSource>(reference);
        this.container = new SoftReference<DBSDataSourceContainer>(reference.getContainer());
        this.previous = previous;
        this.transactional = transactional;
    }

    protected void close()
    {
        if (transaction != null) {
            transaction.rollback(null);
        }
        for (QMMStatementInfo stat = statementStack; stat != null; stat = stat.getPrevious()) {
            if (!stat.isClosed()) {
                log.warn("Statement '" + stat.getObjectId() + "' is not closed");
                stat.close();
            }
        }
        super.close();
    }

    QMMTransactionInfo changeTransactional(boolean transactional)
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

    QMMTransactionInfo commit()
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

    QMMObject rollback(DBCSavepoint savepoint)
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

    QMMStatementInfo openStatement(DBCStatement statement)
    {
        return this.statementStack = new QMMStatementInfo(this, statement, null, this.statementStack);
    }

    QMMStatementInfo closeStatement(DBCStatement statement)
    {
        for (QMMStatementInfo stat = this.statementStack; stat != null; stat = stat.getPrevious()) {
            if (stat.getReference() == statement) {
                stat.close();
                return stat;
            }
        }
        return null;
    }

    QMMStatementInfo getStatement(DBCStatement statement)
    {
        for (QMMStatementInfo stat = this.statementStack; stat != null; stat = stat.getPrevious()) {
            if (stat.getReference() == statement) {
                return stat;
            }
        }
        log.warn("Statement meta info not found");
        return null;
    }

    QMMStatementExecuteInfo getExecution(DBCStatement statement)
    {
        for (QMMStatementExecuteInfo exec = this.executionStack; exec != null; exec = exec.getPrevious()) {
            if (exec.getStatement().getReference() == statement) {
                return exec;
            }
        }
        log.warn("Statement execution meta info not found");
        return null;
    }

    QMMStatementExecuteInfo beginExecution(DBCStatement statement)
    {
        QMMStatementInfo stat = getStatement(statement);
        if (stat != null) {
            String queryString = statement.getQueryString();
            if (queryString == null) {
                queryString = statement.getDescription();
            }
            return this.executionStack = new QMMStatementExecuteInfo(
                stat,
                isTransactional() ? getTransaction().getCurrentSavepoint() : null,
                queryString,
                this.executionStack);
        } else {
            return null;
        }
    }

    QMMStatementExecuteInfo endExecution(DBCStatement statement, long rowCount, Throwable error)
    {
        QMMStatementExecuteInfo exec = getExecution(statement);
        if (exec != null) {
            exec.close(rowCount, error);
        }
        return exec;
    }

    QMMStatementExecuteInfo beginFetch(DBCResultSet resultSet)
    {
        QMMStatementExecuteInfo exec = getExecution(resultSet.getSource());
        if (exec == null) {
            exec = beginExecution(resultSet.getSource());
        }
        exec.beginFetch();
        return exec;
    }

    QMMStatementExecuteInfo endFetch(DBCResultSet resultSet, long rowCount)
    {
        QMMStatementExecuteInfo exec = getExecution(resultSet.getSource());
        if (exec != null) {
            exec.endFetch(rowCount);
        }
        return exec;
    }

    public String getContainerId()
    {
        return containerId;
    }

    public DBSDataSourceContainer getContainer()
    {
        return container.get();
    }

    public DBPDataSource getReference()
    {
        return reference == null ? null : reference.get();
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

}
