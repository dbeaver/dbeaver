/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.DBPDataSource;
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
        this.reference.clear();
        this.reference = null;
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
        return null;
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
