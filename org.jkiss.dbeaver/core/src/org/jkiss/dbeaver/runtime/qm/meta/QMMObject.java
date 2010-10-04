/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

/**
 * Abstract QM meta object
 */
public class QMMObject {

    private long id;
    private boolean synced;


    protected static long getTimeStamp()
    {
        return System.currentTimeMillis();
    }

}
