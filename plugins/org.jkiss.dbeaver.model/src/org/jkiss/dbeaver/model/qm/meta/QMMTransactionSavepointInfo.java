/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.exec.DBCSavepoint;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * QM Savepoint info
 */
public class QMMTransactionSavepointInfo extends QMMObject {

    private final QMMTransactionInfo transaction;
    private final String name;
    private boolean commited;
    private final QMMTransactionSavepointInfo previous;
    private QMMStatementExecuteInfo lastExecute;

    private transient DBCSavepoint reference;

    QMMTransactionSavepointInfo(QMMTransactionInfo transaction, DBCSavepoint reference, String name, QMMTransactionSavepointInfo previous)
    {
        this.transaction = transaction;
        this.reference = reference;
        this.name = name;
        this.previous = previous;
    }

    protected void close(boolean commit)
    {
        this.commited = commit;
        super.close();
        this.reference = null;
    }

    DBCSavepoint getReference()
    {
        return reference;
    }

    public QMMTransactionInfo getTransaction()
    {
        return transaction;
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
                case USER_FILTERED:
                case USER_SCRIPT:
                case UTIL:
                case META_DDL:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }
}