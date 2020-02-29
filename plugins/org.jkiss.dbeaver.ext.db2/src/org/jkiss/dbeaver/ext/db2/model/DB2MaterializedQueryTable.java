/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.editors.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableRefreshMode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * DB2 MQT
 * 
 * @author Denis Forveille
 */
public class DB2MaterializedQueryTable extends DB2ViewBase implements DB2SourceObject {

    private DB2TableRefreshMode refreshMode;
    private Timestamp           refreshTime;

    // -----------------
    // Constructors
    // -----------------

    public DB2MaterializedQueryTable(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult)
    {
        super(monitor, schema, dbResult);

        this.refreshTime = JDBCUtils.safeGetTimestamp(dbResult, "REFRESH_TIME");
        this.refreshMode = CommonUtils.valueOf(DB2TableRefreshMode.class, JDBCUtils.safeGetString(dbResult, "REFRESH"));

    }

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public boolean isView()
    {
        return true; // DF: Not sure of that..
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().getMaterializedQueryTableCache().clearChildrenCache(this);

        super.refreshObject(monitor);

        return getContainer().getMaterializedQueryTableCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public JDBCStructCache<DB2Schema, DB2MaterializedQueryTable, DB2TableColumn> getCache()
    {
        return getContainer().getMaterializedQueryTableCache();
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, editable = false, order = 102)
    public DB2TableRefreshMode getRefreshMode()
    {
        return refreshMode;
    }

    @Property(viewable = true, editable = false, order = 103)
    public Timestamp getRefreshTime()
    {
        return refreshTime;
    }

}
