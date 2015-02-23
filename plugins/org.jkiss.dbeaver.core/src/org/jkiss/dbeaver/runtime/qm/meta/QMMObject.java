/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.core.Log;

/**
 * Abstract QM meta object
 */
public class QMMObject {

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
