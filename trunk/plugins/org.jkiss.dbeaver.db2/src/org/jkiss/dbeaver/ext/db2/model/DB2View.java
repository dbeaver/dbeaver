/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.actions.DB2ObjectPersistAction;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2ViewDepCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ViewCheck;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ViewStatus;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceType;
import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * DB2 View
 * 
 * @author Denis Forveille
 */
public class DB2View extends DB2TableBase implements DB2SourceObject {

    private final DB2ViewDepCache viewDepCache = new DB2ViewDepCache();

    private DB2ViewStatus status;
    private String text;
    private DB2ViewCheck viewCheck;
    private Boolean readOnly;

    // -----------------
    // Constructors
    // -----------------

    public DB2View(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult)
    {
        super(monitor, schema, dbResult);

        setName(JDBCUtils.safeGetString(dbResult, "VIEWNAME"));

        this.status = CommonUtils.valueOf(DB2ViewStatus.class, JDBCUtils.safeGetString(dbResult, "VALID"));
        this.text = JDBCUtils.safeGetString(dbResult, "TEXT");
        this.viewCheck = CommonUtils.valueOf(DB2ViewCheck.class, JDBCUtils.safeGetString(dbResult, "VIEWCHECK"));
        this.readOnly = JDBCUtils.safeGetBoolean(dbResult, "READONLY", DB2YesNo.Y.name());
    }

    @Override
    public boolean isView()
    {
        return true;
    }

    @Override
    public DBSObjectState getObjectState()
    {
        return status.getState();
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        getContainer().getViewCache().clearChildrenCache(this);
        viewDepCache.clearCache();
        return true;
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException
    {
        // this.valid = DB2Utils.getObjectStatus(monitor, this,
        // DB2ObjectType.VIEW);
    }

    // @Override
    public IDatabasePersistAction[] getCompileActions()
    {
        return new IDatabasePersistAction[] { new DB2ObjectPersistAction(DB2ObjectType.VIEW, "Compile view", "ALTER VIEW "
            + getFullQualifiedName() + " COMPILE") };
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
    // Source
    // -----------------

    @Override
    public DB2SourceType getSourceType()
    {
        return DB2SourceType.VIEW;
    }

    @Override
    public String getSourceDeclaration(DBRProgressMonitor monitor) throws DBException
    {
        return SQLUtils.formatSQL(getDataSource(), text);
    }

    @Override
    public void setSourceDeclaration(String source)
    {
        // TODO Auto-generated method stub

    }

    // -----------------
    // Association
    // -----------------

    @Association
    public Collection<DB2ViewDep> getViewDeps(DBRProgressMonitor monitor) throws DBException
    {
        return viewDepCache.getObjects(monitor, this);
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(viewable = false, editable = false, updatable = false)
    public String getDescription()
    {
        return super.getDescription();
    }

    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = false, order = 20)
    public DB2ViewStatus getStatus()
    {
        return status;
    }

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

}
