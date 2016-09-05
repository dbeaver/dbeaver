/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.editors.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2ViewBaseDepCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ViewStatus;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

/**
 * Base class for view-like objects (Views, MQT)
 * 
 * @author Denis Forveille
 */
public abstract class DB2ViewBase extends DB2TableBase implements DB2SourceObject {

    protected final DB2ViewBaseDepCache viewBaseDepCache = new DB2ViewBaseDepCache();

    private Timestamp alterTime;
    private Timestamp invalidateTime;
    private Timestamp lastRegenTime;

    private DB2ViewStatus valid;
    private String text;
    private String funcPath;

    // -----------------
    // Constructors
    // -----------------

    public DB2ViewBase(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult)
    {
        super(monitor, schema, dbResult);

        setName(JDBCUtils.safeGetString(dbResult, "TABNAME"));

        this.valid = CommonUtils.valueOf(DB2ViewStatus.class, JDBCUtils.safeGetString(dbResult, "VALID"));
        this.text = JDBCUtils.safeGetString(dbResult, "TEXT");
        this.funcPath = JDBCUtils.safeGetString(dbResult, "FUNC_PATH");

        this.invalidateTime = JDBCUtils.safeGetTimestamp(dbResult, "INVALIDATE_TIME");
        this.lastRegenTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REGEN_TIME");
        if (getDataSource().isAtLeastV9_5()) {
            this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
        }
    }

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public boolean isView()
    {
        return true;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid.getState();
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);

        viewBaseDepCache.clearCache();
        return this;
    }

    // -----------------
    // Source
    // -----------------

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        return SQLUtils.formatSQL(getDataSource(), text);
    }

    // -----------------
    // Association
    // -----------------

    @Association
    public Collection<DB2ViewBaseDep> getViewDeps(DBRProgressMonitor monitor) throws DBException
    {
        return viewBaseDepCache.getAllObjects(monitor, this);
    }

    // -----------------
    // Properties
    // -----------------

    @Nullable
    @Override
    @Property(viewable = false, editable = false, updatable = false)
    public String getDescription()
    {
        return super.getDescription();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = false, order = 20)
    public DB2ViewStatus getValid()
    {
        return valid;
    }

    @Property(viewable = false, editable = false, order = 20)
    public String getFuncPath()
    {
        return funcPath;
    }

    @Property(viewable = false, editable = false, order = 101, category = DB2Constants.CAT_DATETIME)
    public Timestamp getAlterTime()
    {
        return alterTime;
    }

    @Property(viewable = false, editable = false, order = 102, category = DB2Constants.CAT_DATETIME)
    public Timestamp getInvalidateTime()
    {
        return invalidateTime;
    }

    @Property(viewable = false, editable = false, order = 103, category = DB2Constants.CAT_DATETIME)
    public Timestamp getLastRegenTime()
    {
        return lastRegenTime;
    }

}
