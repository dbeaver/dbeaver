/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ViewCheck;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 View
 * 
 * @author Denis Forveille
 */
public class DB2View extends DB2ViewBase implements DB2SourceObject {

    private DB2ViewCheck viewCheck;
    private Boolean      readOnly;

    // -----------------
    // Constructors
    // -----------------

    public DB2View(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult)
    {
        super(monitor, schema, dbResult);

        this.viewCheck = CommonUtils.valueOf(DB2ViewCheck.class, JDBCUtils.safeGetString(dbResult, "VIEWCHECK"));
        this.readOnly = JDBCUtils.safeGetBoolean(dbResult, "READONLY", DB2YesNo.Y.name());
    }

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public boolean isView()
    {
        return true;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);
        return getContainer().getViewCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public JDBCStructCache<DB2Schema, DB2View, DB2TableColumn> getCache()
    {
        return getContainer().getViewCache();
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, editable = false, order = 21)
    public Boolean getReadOnly()
    {
        return readOnly;
    }

    @Property(viewable = true, editable = false, order = 22)
    public DB2ViewCheck getViewCheck()
    {
        return viewCheck;
    }

    @Override
    @Property(hidden = true)
    public Integer getTableId()
    {
        return super.getTableId();
    }

}
