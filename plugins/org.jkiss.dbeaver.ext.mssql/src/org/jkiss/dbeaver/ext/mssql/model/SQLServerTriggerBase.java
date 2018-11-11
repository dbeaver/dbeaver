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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTrigger;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.Map;

/**
 * SQLServerTriggerBase
 */
public abstract class SQLServerTriggerBase<OWNER extends DBSObject> extends AbstractTrigger implements DBPScriptObject, DBPRefreshableObject, SQLServerObject
{
    private OWNER container;
    private String body;
    private long objectId;
    private boolean disabled;
    private boolean persisted;

    public SQLServerTriggerBase(
        OWNER container,
        ResultSet dbResult)
    {
        super(JDBCUtils.safeGetString(dbResult, "name"), null, true);

        this.objectId = JDBCUtils.safeGetLong(dbResult, "object_id");
        this.disabled = JDBCUtils.safeGetInt(dbResult, "is_disabled") != 0;
        this.container = container;
        this.persisted = true;
    }

    public SQLServerTriggerBase(
        OWNER container,
        String name)
    {
        super(name, null, false);
        this.container = container;

        this.body = "";
        this.persisted = false;
    }

    public SQLServerTriggerBase(OWNER container, SQLServerTriggerBase source) {
        super(source.name, source.getDescription(), false);
        this.container = container;
        this.body = source.body;
        this.persisted = source.persisted;
    }

    @Override
    @Property(viewable = false, order = 10)
    public long getObjectId() {
        return objectId;
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
