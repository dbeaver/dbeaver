/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

import java.sql.ResultSet;
import java.util.Map;

/**
 * SQLServerTriggerBase
 */
public abstract class SQLServerTriggerBase<OWNER extends DBSObject> implements DBSTrigger, DBPScriptObject, DBPQualifiedObject, DBPRefreshableObject, SQLServerObject
{
    private OWNER container;
    private String name;
    private String type;
    private String body;
    private long objectId;
    private boolean insteadOfTrigger;
    private boolean disabled;
    private boolean persisted;

    public SQLServerTriggerBase(
        OWNER container,
        ResultSet dbResult)
    {
        this.container = container;

        this.name = JDBCUtils.safeGetString(dbResult, "name");
        this.type = JDBCUtils.safeGetString(dbResult, "type");
        this.objectId = JDBCUtils.safeGetLong(dbResult, "object_id");
        this.insteadOfTrigger = JDBCUtils.safeGetInt(dbResult, "is_instead_of_trigger") != 0;
        this.disabled = JDBCUtils.safeGetInt(dbResult, "is_disabled") != 0;
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

    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    @Property(viewable = false, order = 10)
    public long getObjectId() {
        return objectId;
    }

    @Property(viewable = true, order = 11)
    public boolean isInsteadOfTrigger() {
        return insteadOfTrigger;
    }

    @Property(viewable = false, order = 20)
    public boolean isDisabled() {
        return disabled;
    }

    public String getBody()
    {
        return body;
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
        if (body == null) {
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


}
