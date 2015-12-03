/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.dbeaver.Log;

/**
 * Abstract QM meta object
 */
public abstract class QMMObject {

    static final Log log = Log.getLog(QMMObject.class);

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

    public boolean isClosed()
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
