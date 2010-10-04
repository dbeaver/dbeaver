/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

/**
 * Abstract QM meta object
 */
public class QMMObject {

    private long objectId;
    private boolean synced;

    public long getObjectId()
    {
        return objectId;
    }

    public boolean isSynced()
    {
        return synced;
    }

    protected static long getTimeStamp()
    {
        return System.currentTimeMillis();
    }

}
