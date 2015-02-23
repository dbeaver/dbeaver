/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
            @Override
            public boolean hasNext()
            {
                return curExec != null && curExec.getSavepoint() == QMMTransactionSavepointInfo.this;
            }
            @Override
            public QMMStatementExecuteInfo next()
            {
                if (curExec == null || curExec.getSavepoint() != QMMTransactionSavepointInfo.this) {
                    throw new NoSuchElementException();
                }
                QMMStatementExecuteInfo n = curExec;
                curExec = curExec.getPrevious();
                return n;
            }
            @Override
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
                case UTIL:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }
}