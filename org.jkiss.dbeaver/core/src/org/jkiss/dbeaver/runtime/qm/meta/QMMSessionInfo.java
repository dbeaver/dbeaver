/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;

import java.lang.ref.SoftReference;

/**
 * Data source information
 */
public class QMMSessionInfo extends QMMObject {

    static final Log log = LogFactory.getLog(QMMSessionInfo.class);

    private final String containerId;
    private SoftReference<DBPDataSource> reference;
    private final long openTime;
    private long closeTime;
    private boolean transactional;

    private QMMSessionInfo previous;
    private QMMStatementInfo statementStack;
    private QMMTransactionInfo transaction;

    QMMSessionInfo(DBPDataSource reference, boolean transactional, QMMSessionInfo previous)
    {
        this.containerId = reference.getContainer().getId();
        this.reference = new SoftReference<DBPDataSource>(reference);
        this.previous = previous;
        this.openTime = getTimeStamp();
        this.transactional = transactional;
    }

    void close()
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
        this.closeTime = getTimeStamp();
        this.reference.clear();
        this.reference = null;
    }

    boolean isClosed()
    {
        return closeTime > 0;
    }

    void changeTransactional(boolean transactional)
    {
        if (this.transactional == transactional) {
            return;
        }
        this.transactional = transactional;
        if (this.transaction != null) {
            // Commit current transaction
            this.transaction.commit();
        }
        // start new transaction
        this.transaction = new QMMTransactionInfo(this, this.transaction);
    }

    void commit()
    {
        if (this.transactional) {
            if (this.transaction != null) {
                this.transaction.commit();
            }
            this.transaction = new QMMTransactionInfo(this, this.transaction);
        }
    }

    void rollback(DBCSavepoint savepoint)
    {
        if (this.transactional) {
            if (this.transaction != null) {
                this.transaction.rollback(savepoint);
            }
            this.transaction = new QMMTransactionInfo(this, this.transaction);
        }
    }

    void openStatement(DBCStatement statement)
    {
        this.statementStack = new QMMStatementInfo(this, statement, null, this.statementStack);
    }

    boolean closeStatement(DBCStatement statement)
    {
        for (QMMStatementInfo stat = this.statementStack; stat != null; stat = stat.getPrevious()) {
            if (stat.getReference() == statement) {
                stat.close();
                return true;
            }
        }
        return false;
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

    public DBPDataSource getReference()
    {
        return reference == null ? null : reference.get();
    }

    public long getOpenTime()
    {
        return openTime;
    }

    public long getCloseTime()
    {
        return closeTime;
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

}
