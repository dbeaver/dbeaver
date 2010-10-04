/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.exec.DBCSavepoint;

import java.lang.ref.SoftReference;

/**
 * QM Savepoint info
 */
public class QMMSavepointInfo extends QMMObject {

    private final QMMTransactionInfo transaction;
    private SoftReference<DBCSavepoint> reference;
    private final long startTime;
    private final String name;
    private boolean finished;
    private boolean commited;
    private final QMMSavepointInfo previous;

    public QMMSavepointInfo(QMMTransactionInfo transaction, DBCSavepoint reference, String name, QMMSavepointInfo previous)
    {
        this.transaction = transaction;
        this.reference = new SoftReference<DBCSavepoint>(reference);
        this.startTime = getTimeStamp();
        this.name = name;
        this.previous = previous;
    }

    public QMMTransactionInfo getTransaction()
    {
        return transaction;
    }

    public DBCSavepoint getReference()
    {
        return reference.get();
    }

    public long getStartTime()
    {
        return startTime;
    }

    public String getName()
    {
        return name;
    }

    public boolean isFinished()
    {
        return finished;
    }

    public boolean isCommited()
    {
        return commited;
    }

    public QMMSavepointInfo getPrevious()
    {
        return previous;
    }

    public void endSavepoint(boolean commit)
    {
        
    }
}