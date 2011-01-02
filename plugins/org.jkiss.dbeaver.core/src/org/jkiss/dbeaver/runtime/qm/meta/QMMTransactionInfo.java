/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.exec.DBCSavepoint;

/**
 * QM Transaction info
 */
public class QMMTransactionInfo extends QMMObject {

    private final QMMSessionInfo session;
    private final QMMTransactionInfo previous;
    private boolean commited;
    private QMMTransactionSavepointInfo savepointStack;

    QMMTransactionInfo(QMMSessionInfo session, QMMTransactionInfo previous)
    {
        this.session = session;
        this.previous = previous;
        this.savepointStack = new QMMTransactionSavepointInfo(this, null, null, null);
    }

    void commit()
    {
        this.commited = true;
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
        this.commited = false;
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

    public boolean isCommited()
    {
        return commited;
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

}
