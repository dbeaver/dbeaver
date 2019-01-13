/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.exec.DBCSavepoint;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * QM Savepoint info
 */
public class QMMTransactionSavepointInfo extends QMMObject {

    private final QMMTransactionInfo transaction;
    private final String name;
    private boolean committed;
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
        this.committed = commit;
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

    public boolean isCommitted()
    {
        return committed;
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

    @Override
    public String getText() {
        return transaction.getText();
    }
}