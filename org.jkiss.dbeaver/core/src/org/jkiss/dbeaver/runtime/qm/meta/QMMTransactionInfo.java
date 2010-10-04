/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.exec.DBCSavepoint;

/**
 * QM Transaction info
 */
public class QMMTransactionInfo extends QMMObject {

    private final QMMSessionInfo session;
    private final QMMTransactionInfo previous;
    private final long startTime;
    private long endTime;
    private boolean finished;
    private boolean commited;
    private QMMSavepointInfo savepoint;

    QMMTransactionInfo(QMMSessionInfo session, QMMTransactionInfo previous)
    {
        this.session = session;
        this.previous = previous;
        this.startTime = getTimeStamp();
        this.savepoint = new QMMSavepointInfo(this, null, null, null);
    }

    void commit()
    {
        this.endTime = getTimeStamp();
        this.finished = true;
        this.commited = true;
        for (QMMSavepointInfo sp = savepoint; sp != null; sp = sp.getPrevious()) {
            if (!sp.isFinished()) {
                // Commit all non-finished savepoints
                sp.applySavepoint(true);
            }
        }
    }

    void rollback(DBCSavepoint toSavepoint)
    {
        this.endTime = getTimeStamp();
        this.finished = true;
        this.commited = false;
        for (QMMSavepointInfo sp = savepoint; sp != null; sp = sp.getPrevious()) {
            sp.applySavepoint(false);
            if (toSavepoint != null && sp.getReference() == toSavepoint) {
                break;
            }
        }
    }

    public QMMSessionInfo getSession()
    {
        return session;
    }

    public QMMTransactionInfo getPrevious()
    {
        return previous;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public boolean isFinished()
    {
        return finished;
    }

    public boolean isCommited()
    {
        return commited;
    }

    public QMMSavepointInfo getSavepoint()
    {
        return savepoint;
    }

}
