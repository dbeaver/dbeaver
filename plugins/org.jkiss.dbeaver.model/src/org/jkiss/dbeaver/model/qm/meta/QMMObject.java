/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import java.util.Map;

/**
 * Abstract QM meta object
 */
public abstract class QMMObject {

    static final Log log = Log.getLog(QMMObject.class);

    public enum ObjectType {
        ConnectionInfo("c"),
        StatementExecuteInfo("x"),
        StatementInfo("s"),
        TransactionInfo("t"),
        TransactionSavepointInfo("ts");

        private final String id;

        ObjectType(String id) {
            this.id = id;
        }

        public static ObjectType getById(String id) {
            for (ObjectType ot : values()) {
                if (ot.id.equals(id)) {
                    return ot;
                }
            }
            return null;
        }

        public String getId() {
            return id;
        }
    }

    private static int globalObjectId = 0;

    private final long objectId;

    private final long openTime;
    private long closeTime;

    private boolean synced;
    private boolean updated;

    public QMMObject() {
        this.objectId = generateObjectId();
        this.openTime = getTimeStamp();
    }

    protected QMMObject(long openTime, long closeTime) {
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

    public boolean isSynced() {
        return synced;
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

    // fore serialization
    public abstract ObjectType getObjectType();

    protected synchronized void update() {
        this.updated = true;
    }

    protected synchronized void sync() {
        this.synced = true;
        this.updated = false;
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

    public abstract Map<String, Object> toMap();

}
