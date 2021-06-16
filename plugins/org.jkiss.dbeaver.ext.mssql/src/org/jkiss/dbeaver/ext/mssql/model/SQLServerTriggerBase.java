/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * SQLServerTriggerBase
 */
public abstract class SQLServerTriggerBase<OWNER extends DBSObject> implements DBSTrigger, DBSObjectWithScript, DBPQualifiedObject, DBPRefreshableObject, SQLServerObject, DBPStatefulObject
{
    private OWNER container;
    private String name;
    private String type;
    private String body;
    private long objectId;
    private String typeDescription;
    private String triggerTypeDescription;
    private Date createDate;
    private Date modifyDate;
    private boolean insteadOfTrigger;
    private volatile int disabled;
    private boolean isMsShipped;
    private boolean isNotForReplication;
    private volatile boolean persisted;

    public SQLServerTriggerBase(
        OWNER container,
        ResultSet dbResult)
    {
        this.container = container;

        this.name = JDBCUtils.safeGetString(dbResult, "name");
        this.type = JDBCUtils.safeGetString(dbResult, "type");
        this.objectId = JDBCUtils.safeGetLong(dbResult, "object_id");
        this.insteadOfTrigger = JDBCUtils.safeGetInt(dbResult, "is_instead_of_trigger") != 0;
        this.disabled = JDBCUtils.safeGetInt(dbResult, "is_disabled");
        this.typeDescription = JDBCUtils.safeGetString(dbResult, "type_desc");
        this.triggerTypeDescription = JDBCUtils.safeGetString(dbResult, "trigger_type");
        this.createDate = JDBCUtils.safeGetDate(dbResult, "create_date");
        this.modifyDate = JDBCUtils.safeGetDate(dbResult, "modify_date");
        this.isMsShipped = JDBCUtils.safeGetInt(dbResult, "is_ms_shipped") != 0;
        this.isNotForReplication = JDBCUtils.safeGetInt(dbResult, "is_not_for_replication") != 0;
        this.persisted = true;
    }

    public SQLServerTriggerBase(
        OWNER container,
        String name)
    {
        this.container = container;
        this.name = name;

        this.body = "";
        this.persisted = false;
    }

    public SQLServerTriggerBase(OWNER container, SQLServerTriggerBase source) {
        this.container = container;
        this.name = source.name;
        this.type = source.type;
        this.body = source.body;
        this.persisted = source.persisted;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Property(viewable = false, order = 10)
    public long getObjectId() {
        return objectId;
    }

    @Property(viewable = true, order = 11)
    public String getTypeDescription() {
        return typeDescription;
    }

    @Property(viewable = true, order = 12)
    public String getTriggerTypeDescription() {
        return triggerTypeDescription;
    }

    @Property(viewable = true, order = 13)
    public Date getCreateDate() {
        return createDate;
    }

    @Property(viewable = true, order = 14)
    public Date getModifyDate() {
        return modifyDate;
    }

    @Property(viewable = true, order = 15)
    public boolean isInsteadOfTrigger() {
        return insteadOfTrigger;
    }

    @Property(viewable = false, order = 16)
    public boolean isDisabled() {
        return disabled != 0;
    }

    @Property(viewable = true, order = 17)
    public boolean isMsShipped() {
        return isMsShipped;
    }

    @Property(viewable = true, order = 18)
    public boolean isNotForReplication() {
        return isNotForReplication;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = 1;
    }
    
    public String getBody()
    {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public OWNER getParentObject()
    {
        return container;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource()
    {
        return (SQLServerDataSource) container.getDataSource();
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        if (body == null && isPersisted()) {
            OWNER owner = getParentObject();
            SQLServerDatabase database = null;
            if (owner instanceof SQLServerDatabase) {
                database = (SQLServerDatabase) owner;
            } else if (owner instanceof SQLServerTableBase) {
                database = ((SQLServerTableBase) owner).getDatabase();
            }
            body = SQLServerUtils.extractSource(monitor, database, this);
        }
        return body;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        if (disabled != 0) {
            return DBSObjectState.INVALID;
        }
        return DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Refresh triggers state")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT is_disabled FROM sys.triggers WHERE object_id=?")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        disabled = JDBCUtils.safeGetInt(dbResult, 1);
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }

    }
    
    @Override
    public void setObjectDefinitionText(String sourceText) {
        this.body = sourceText;
    }

    @Override
    public String toString() {
        return getName();
    }
}
