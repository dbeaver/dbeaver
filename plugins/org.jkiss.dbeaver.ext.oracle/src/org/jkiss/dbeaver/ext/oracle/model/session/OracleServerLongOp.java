/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.model.session;

import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.util.Date;

/**
 * Session
 */
public class OracleServerLongOp implements DBPObject {


    private String opName;
    private String target;
    private String targetDesc;
    private long soFar;
    private long totalWork;
    private String units;
    private Date startTime;
    private Date lastUpdateTime;
    private Date timestamp;
    private long timeRemaining;
    private long elapsedSeconds;

    public OracleServerLongOp(ResultSet dbResult) {
        this.opName = JDBCUtils.safeGetString(dbResult, "OPNAME");
        this.target = JDBCUtils.safeGetString(dbResult, "TARGET");
        this.targetDesc = JDBCUtils.safeGetString(dbResult, "TARGET_DESC");
        this.soFar = JDBCUtils.safeGetLong(dbResult, "SOFAR");
        this.totalWork = JDBCUtils.safeGetLong(dbResult, "TOTALWORK");
        this.units = JDBCUtils.safeGetString(dbResult, "UNITS");
        this.startTime = JDBCUtils.safeGetTimestamp(dbResult, "START_TIME");
        this.lastUpdateTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_UPDATE_TIME");
        this.timestamp = JDBCUtils.safeGetTimestamp(dbResult, "TIMESTAMP");
        this.timeRemaining = JDBCUtils.safeGetLong(dbResult, "TIME_REMAINING");
        this.elapsedSeconds = JDBCUtils.safeGetLong(dbResult, "ELAPSED_SECONDS");
    }

    @Property(viewable = true, order = 1)
    public String getOpName() {
        return opName;
    }

    @Property(viewable = true, order = 2)
    public String getTarget() {
        return target;
    }

    @Property(viewable = true, order = 3)
    public String getTargetDesc() {
        return targetDesc;
    }

    @Property(viewable = true, order = 4)
    public long getSoFar() {
        return soFar;
    }

    @Property(viewable = true, order = 5)
    public long getTotalWork() {
        return totalWork;
    }

    @Property(viewable = false, order = 6)
    public String getUnits() {
        return units;
    }

    @Property(viewable = false, order = 7)
    public Date getStartTime() {
        return startTime;
    }

    @Property(viewable = false, order = 8)
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Property(viewable = false, order = 9)
    public Date getTimestamp() {
        return timestamp;
    }

    @Property(viewable = true, order = 10)
    public long getTimeRemaining() {
        return timeRemaining;
    }

    @Property(viewable = true, order = 11)
    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    @Override
    public String toString() {
        return opName;
    }
}
