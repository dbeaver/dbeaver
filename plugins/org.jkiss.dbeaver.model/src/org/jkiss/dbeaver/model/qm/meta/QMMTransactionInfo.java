/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

    private final QMMSessionInfo session;
    private final QMMTransactionInfo previous;
    private boolean committed;
    private QMMTransactionSavepointInfo savepointStack;

    QMMTransactionInfo(QMMSessionInfo session, QMMTransactionInfo previous)
    {
        this.session = session;
        this.previous = previous;
        this.savepointStack = new QMMTransactionSavepointInfo(this, null, null, null);
    }

    void commit()
    {
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

    public QMMSessionInfo getSession()
    {
        return session;
    }

    public QMMTransactionInfo getPrevious()
    {
        return previous;
    }

    public boolean isCommitted()
    {
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
        return session.getText();
    }
}
