/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.exec.DBCSavepoint;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * QM Savepoint info
 */
public class QMMTransactionSavepointInfo extends QMMObject {

    private final QMMTransactionInfo transaction;
    private SoftReference<DBCSavepoint> reference;
    private final String name;
    private boolean commited;
    private final QMMTransactionSavepointInfo previous;
    private QMMStatementExecuteInfo lastExecute;

    QMMTransactionSavepointInfo(QMMTransactionInfo transaction, DBCSavepoint reference, String name, QMMTransactionSavepointInfo previous)
    {
        this.transaction = transaction;
        this.reference = new SoftReference<DBCSavepoint>(reference);
        this.name = name;
        this.previous = previous;
    }

    protected void close(boolean commit)
    {
        this.commited = commit;
        super.close();
    }

    public QMMTransactionInfo getTransaction()
    {
        return transaction;
    }

    public DBCSavepoint getReference()
    {
        return reference == null ? null : reference.get();
    }

    public String getName()
    {
        return name;
    }

    public boolean isCommited()
    {
        return commited;
    }

    public QMMTransactionSavepointInfo getPrevious()
    {
        return previous;
    }

    public QMMStatementExecuteInfo getLastExecute()
    {
        return lastExecute;
    }

    void setLastExecute(QMMStatementExecuteInfo lastExecute)
    {
        this.lastExecute = lastExecute;
    }

    public Iterator<QMMStatementExecuteInfo> getExecutions()
    {
        return new Iterator<QMMStatementExecuteInfo>() {
            private QMMStatementExecuteInfo curExec = lastExecute;
            public boolean hasNext()
            {
                return curExec != null && curExec.getSavepoint() == QMMTransactionSavepointInfo.this;
            }
            public QMMStatementExecuteInfo next()
            {
                if (curExec == null || curExec.getSavepoint() != QMMTransactionSavepointInfo.this) {
                    throw new NoSuchElementException();
                }
                QMMStatementExecuteInfo n = curExec;
                curExec = curExec.getPrevious();
                return n;
            }
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public String toString()
    {
        return "SAVEPOINT" + (name == null ? "" : name);
    }

    public boolean hasUserExecutions()
    {
        for (QMMStatementExecuteInfo exec = lastExecute; exec != null; exec = exec.getPrevious()) {
            switch (exec.getStatement().getPurpose()) {
                case USER:
                case USER_SCRIPT:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }
}