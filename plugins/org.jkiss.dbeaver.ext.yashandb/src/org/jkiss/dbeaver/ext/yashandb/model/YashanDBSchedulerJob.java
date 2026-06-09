/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectType;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchedulerJob;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchedulerJobArgument;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * YashanDBSchedulerJob
 */
public class YashanDBSchedulerJob extends OracleSchedulerJob {

    public YashanDBSchedulerJob(OracleSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    public YashanDBSchedulerJob(OracleSchema schema, String name, String state, String jobAction) {
        super(schema, name, state, jobAction);
    }

    @Override
    public Collection<OracleSchedulerJobArgument> getArguments(DBRProgressMonitor monitor) throws DBException {
        // YashanDB not support yet
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        // YashanDB custom
        if (jobAction == null && monitor != null) {
            monitor.beginTask("Load action for '" + this.getName() + "'...", 1);
            try (final JDBCSession session = DBUtils.openMetaSession(monitor, this,
                "Load action for " + OracleObjectType.JOB + " '" + this.getName() + "'")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT JOB_ACTION FROM " + OracleUtils.getSysSchemaPrefix(getDataSource()) + "ALL_SCHEDULER_JOBS "
                        + "WHERE OWNER=? AND JOB_NAME=? ")) {
                    dbStat.setString(1, getOwner());
                    dbStat.setString(2, getName());
                    dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        StringBuilder action = null;
                        int lineCount = 0;
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            final String line = dbResult.getString(1);
                            if (action == null) {
                                action = new StringBuilder(4000);
                            }
                            action.append(line);
                            lineCount++;
                            monitor.subTask("Line " + lineCount);
                        }
                        if (action != null) {
                            jobAction = action.toString();
                        }
                    }
                } catch (SQLException e) {
                    throw new DBCException(e, session.getExecutionContext());
                }
            } finally {
                monitor.done();
            }
        }
        return jobAction;
    }
}
