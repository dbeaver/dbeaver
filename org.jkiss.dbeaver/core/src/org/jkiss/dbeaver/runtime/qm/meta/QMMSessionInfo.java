/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.DBPDataSource;

import java.lang.ref.SoftReference;

/**
 * Data source information
 */
public class QMMSessionInfo extends QMMObject {

    private final String containerId;
    private SoftReference<DBPDataSource> reference;
    private final long openTime;
    private long closeTime;

    private QMMSessionInfo previous;
    private QMMStatementInfo statement;
    private QMMTransactionInfo transaction;

    QMMSessionInfo(DBPDataSource reference, QMMSessionInfo previous)
    {
        this.containerId = reference.getContainer().getId();
        this.reference = new SoftReference<DBPDataSource>(reference);
        this.previous = previous;
        this.openTime = getTimeStamp();
    }

    void close()
    {
        this.closeTime = getTimeStamp();
        this.reference.clear();
        this.reference = null;
    }

    boolean isClosed()
    {
        return closeTime > 0;
    }

    public String getContainerId()
    {
        return containerId;
    }

    public DBPDataSource getReference()
    {
        return reference.get();
    }

    public long getOpenTime()
    {
        return openTime;
    }

    public long getCloseTime()
    {
        return closeTime;
    }

    public QMMStatementInfo getStatement()
    {
        return statement;
    }

    public QMMTransactionInfo getTransaction()
    {
        return transaction;
    }

    public QMMSessionInfo getPrevious()
    {
        return previous;
    }

}
