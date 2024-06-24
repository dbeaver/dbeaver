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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.Timestamp;
import java.util.Map;

public class AltibaseDirectory extends AltibaseObject<GenericStructContainer> implements DBPScriptObject, DBPRefreshableObject {

    protected String ddl;
    
    private String dirPath;
    private Timestamp created;
    private Timestamp lastDdlTime;

    protected AltibaseDirectory(GenericStructContainer parent, JDBCResultSet resultSet) {
        super(parent, 
                JDBCUtils.safeGetString(resultSet, "DIRECTORY_NAME"), 
                JDBCUtils.safeGetLong(resultSet, "DIRECTORY_ID"),
                true);

        updateState(resultSet);
    }
    
    private void updateState(JDBCResultSet resultSet) {
        dirPath = JDBCUtils.safeGetString(resultSet, "DIRECTORY_PATH");
        created = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
        lastDdlTime = JDBCUtils.safeGetTimestamp(resultSet, "LAST_DDL_TIME");
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public long getDirId() {
        return getObjectId();
    }

    @Nullable
    @Property(viewable = true, order = 4)
    public String getDirName() {
        return dirPath;
    }

    @NotNull
    @Property(viewable = true, order = 10)
    public Timestamp getCreated() {
        return created;
    }

    @NotNull
    @Property(viewable = true, order = 11)
    public Timestamp getLastDdlTime() {
        return lastDdlTime;
    }

    
    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(ddl)) {
            ddl = ((AltibaseMetaModel) getDataSource().getMetaModel()).getDirectoryDDL(monitor, this, options);
        }
        
        return (CommonUtils.isEmpty(ddl)) ? "" : ddl + ";";
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        ddl = null;
        if (monitor != null) {
            monitor.beginTask("Load action for '" + this.getName() + "'...", 1);
            try (final JDBCSession session = DBUtils.openMetaSession(monitor, this, 
                    "Load action for Directory '" + this.getName() + "'")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT * FROM system_.sys_directories_ WHERE directory_id = ?")) {
                    dbStat.setLong(1, getObjectId());

                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.next()) {
                            if (!monitor.isCanceled()) {
                                updateState(dbResult);
                            }
                        }
                    }
                } catch (Exception e) {
                    monitor.subTask("Error refreshing job state " + e.getMessage());
                } finally {
                    monitor.done();
                }
            }
        }
        return this;
    }
}