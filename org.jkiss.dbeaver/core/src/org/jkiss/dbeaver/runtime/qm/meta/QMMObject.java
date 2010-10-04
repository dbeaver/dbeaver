/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

/**
 * Abstract QM meta object
 */
public class QMMObject {

    private static int globalObjectId = 0;

    private final long objectId;

    private final long openTime;
    private long closeTime;

    private boolean synced;
    private boolean updated;

    public QMMObject()
    {
        this.objectId = generateObjectId();
        this.openTime = getTimeStamp();
    }

    protected void close()
    {
        this.closeTime = getTimeStamp();
        this.update();
    }

    public long getObjectId()
    {
        return objectId;
    }

    public boolean isSynced()
    {
        return synced;
    }

    public boolean isUpdated()
    {
        return updated;
    }

    public long getOpenTime()
    {
        return openTime;
    }

    public long getCloseTime()
    {
        return closeTime;
    }

    boolean isClosed()
    {
        return closeTime > 0;
    }

    protected synchronized void update()
    {
        this.updated = true;
    }

    protected synchronized void sync()
    {
        this.synced = true;
        this.updated = false;
    }

    private static synchronized long generateObjectId()
    {
        globalObjectId++;
        return globalObjectId;
    }

    protected static long getTimeStamp()
    {
        return System.currentTimeMillis();
    }

}
