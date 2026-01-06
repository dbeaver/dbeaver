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

import org.jkiss.dbeaver.model.exec.DBCSavepoint;

/**
 * QM Transaction info
 */
public class QMMTransactionInfo extends QMMObject {

    private final QMMConnectionInfo connection;
    private final transient QMMTransactionInfo previous;
    private boolean committed;
    private final transient QMMTransactionSavepointInfo savepointStack = new QMMTransactionSavepointInfo(this, null, null, null);

    QMMTransactionInfo(QMMConnectionInfo connection, QMMTransactionInfo previous) {
        super(QMMetaObjectType.TRANSACTION_INFO);
        this.connection = connection;
        this.previous = previous;
    }

    public QMMTransactionInfo(QMMConnectionInfo connection, long openTime) {
        super(QMMetaObjectType.TRANSACTION_INFO, openTime, openTime);
        this.connection = connection;
        this.previous = null;
    }

    void commit() {
        this.committed = true;
        for (QMMTransactionSavepointInfo sp = savepointStack; sp != null; sp = sp.getPrevious()) {
            if (!sp.isClosed()) {
                // Commit all non-finished savepoints
                sp.close(true);
            }
        }
        super.close();
    }

    void rollback(DBCSavepoint toSavepoint)
    {
        this.committed = false;
        for (QMMTransactionSavepointInfo sp = savepointStack; sp != null; sp = sp.getPrevious()) {
            sp.close(false);
            if (toSavepoint != null && sp.getReference() == toSavepoint) {
                break;
            }
        }
        super.close();
    }

    public QMMConnectionInfo getConnection() {
        return connection;
    }

    public QMMTransactionInfo getPrevious() {
        return previous;
    }

    public boolean isCommitted() {
        return committed;
    }

    public QMMTransactionSavepointInfo getCurrentSavepoint()
    {
        return savepointStack;
    }

    public QMMObject getSavepoint(DBCSavepoint savepoint)
    {
        for (QMMTransactionSavepointInfo sp = this.savepointStack; sp != null; sp = sp.getPrevious()) {
            if (sp.getReference() == savepoint) {
                return sp;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "TRANSACTION";
    }

    @Override
    public String getText() {
        return connection.getText();
    }

}
