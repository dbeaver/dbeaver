/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.dbeaver.Log;

/**
 * Abstract QM meta object
 */
public abstract class QMMObject {

    static final Log log = Log.getLog(QMMObject.class);

    private static int globalObjectId = 0;
    private final QMMetaObjectType type;

    private final long objectId;

    private final long openTime;
    private long closeTime;

    private transient boolean updated;

    public QMMObject(QMMetaObjectType type) {
        this.type = type;
        this.objectId = generateObjectId();
        this.openTime = getTimeStamp();
    }

    protected QMMObject(QMMetaObjectType type, long openTime, long closeTime) {
        this.type = type;
        this.objectId = generateObjectId();
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    protected void close() {
        this.closeTime = getTimeStamp();
        this.update();
    }

    protected void reopen() {
        this.closeTime = 0;
        this.update();
    }

    public long getObjectId() {
        return objectId;
    }

    public boolean isUpdated() {
        return updated;
    }

    public long getOpenTime() {
        return openTime;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public boolean isClosed() {
        return closeTime > 0;
    }

    public abstract String getText();

    // for serialization
    public QMMetaObjectType getObjectType() {
        return type;
    }

    protected synchronized void update() {
        this.updated = true;
    }

    private static synchronized long generateObjectId() {
        globalObjectId++;
        return globalObjectId;
    }

    protected static long getTimeStamp() {
        return System.currentTimeMillis();
    }

    public long getDuration() {
        if (!isClosed()) {
            return -1L;
        }
        return getCloseTime() - getOpenTime();
    }

    public abstract QMMConnectionInfo getConnection();

}
