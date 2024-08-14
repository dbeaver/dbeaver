/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Map;

public class AltibaseJob extends AltibaseGlobalObject implements DBPScriptObject, DBPRefreshableObject {

    private String ddl;
    
    private int jobId;
    private String jobName;
    private String execQuery;
    private Timestamp startTime;
    private Timestamp endTime;
    private int interval;
    private String intervalType;
    private boolean activated; // state
    private Timestamp lastExecTime;
    private int execCount;
    private String errorCode;
    private boolean isEnable;
    private String comment;

    public AltibaseJob(GenericStructContainer owner, @NotNull ResultSet resultSet) {

        super((AltibaseDataSource) owner.getDataSource(), true);

        jobId = JDBCUtils.safeGetInt(resultSet, "JOB_ID");
        jobName = JDBCUtils.safeGetString(resultSet, "JOB_NAME");
        execQuery = JDBCUtils.safeGetString(resultSet, "EXEC_QUERY");
        startTime = JDBCUtils.safeGetTimestamp(resultSet, "START_TIME");
        endTime = JDBCUtils.safeGetTimestamp(resultSet, "END_TIME");
        interval = JDBCUtils.safeGetInt(resultSet, "INTERVAL");
        intervalType = JDBCUtils.safeGetString(resultSet, "INTERVAL_TYPE");
        activated = JDBCUtils.safeGetBoolean(resultSet, "STATE", "1");
        lastExecTime = JDBCUtils.safeGetTimestamp(resultSet, "LAST_EXEC_TIME");
        execCount = JDBCUtils.safeGetInt(resultSet, "EXEC_COUNT");
        errorCode = JDBCUtils.safeGetString(resultSet, "ERROR_CODE");
        isEnable = JDBCUtils.safeGetBoolean(resultSet, "IS_ENABLE", "T");
        comment = JDBCUtils.safeGetString(resultSet, "COMMENT");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return jobName;
    }

    @Property(viewable = true, order = 2)
    public int getJobId() {
        return jobId;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public String getExecQuery() {
        return execQuery;
    }

    @NotNull
    @Property(viewable = true, order = 4)
    public String getinterval() {
        return interval + " " + intervalType;
    }

    @Property(viewable = true, order = 5)
    public boolean getActivated() {
        return activated;
    }

    @NotNull
    @Property(viewable = true, order = 6)
    public Timestamp getLastExecTime() {
        return lastExecTime;
    }

    @NotNull
    @Property(viewable = true, order = 7)
    public String getErrorCode() {
        return errorCode;
    }

    @Property(viewable = true, order = 8)
    public int getExecCount() {
        return execCount;
    }

    @Property(viewable = true, order = 8)
    public boolean getIsEnable() {
        return isEnable;
    }

    @NotNull
    @Property(viewable = true, order = 10)
    public Timestamp getStartTime() {
        return startTime;
    }

    @NotNull
    @Property(viewable = true, order = 11)
    public Timestamp getEndTime() {
        return endTime;
    }

    @NotNull
    @Property(viewable = true, order = 12)
    public String getComment() {
        return comment;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(ddl)) {
            ddl = ((AltibaseMetaModel) getDataSource().getMetaModel()).getJobDDL(monitor, this, options) + ";";
        }
        
        return ddl;
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        AltibaseDataSource dataSouce = getDataSource();
        return dataSouce.getJobCache().refreshObject(monitor, dataSouce, this);
    }
}