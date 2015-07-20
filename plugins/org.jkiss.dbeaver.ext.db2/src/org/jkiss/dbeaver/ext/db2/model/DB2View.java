/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * DB2 View
 * 
 * @author Denis Forveille
 */
public class DB2View extends DB2ViewBase implements DB2SourceObject {

    private DB2ViewCheck viewCheck;
    private Boolean readOnly;

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
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().getViewCache().clearChildrenCache(this);
        super.refreshObject(monitor);
        return true;
    }

    @Override
    public JDBCStructCache<DB2Schema, DB2View, DB2TableColumn> getCache()
    {
        return getContainer().getViewCache();
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public Collection<DB2TableColumn> getAttributes(DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().getViewCache().getChildren(monitor, getContainer(), this);
    }

    @Override
    public DB2TableColumn getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException
    {
        return getContainer().getViewCache().getChild(monitor, getContainer(), this, attributeName);
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
