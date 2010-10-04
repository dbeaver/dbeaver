/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

/**
 * QM Transaction info
 */
public class QMMTransactionInfo extends QMMObject {

    private final QMMSessionInfo session;
    private final long startTime;
    private long endTime;
    private boolean finished;
    private boolean commited;
    private QMMSavepointInfo savepoint;

    QMMTransactionInfo(QMMSessionInfo session)
    {
        this.session = session;
        this.startTime = getTimeStamp();
        this.savepoint = new QMMSavepointInfo(this, null, null, null);
    }

    void endTransaction(boolean commit)
    {
        this.finished = true;
        this.commited = commit;
        this.endTime = getTimeStamp();
        for (QMMSavepointInfo sp = savepoint; sp != null; sp = sp.getPrevious()) {
            sp.endSavepoint(commit);
        }
    }

    public QMMSessionInfo getSession()
    {
        return session;
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
