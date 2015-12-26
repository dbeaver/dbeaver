/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * PostgreTable base
 */
public abstract class PostgreTableBase extends JDBCTable<PostgreDataSource, PostgreSchema> implements PostgreClass, PostgreScriptObject
{
    static final Log log = Log.getLog(PostgreTableBase.class);

    private int oid;

    protected PostgreTableBase(PostgreSchema catalog)
    {
        super(catalog, false);
    }

    protected PostgreTableBase(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, JDBCUtils.safeGetString(dbResult, "relname"), true);
        this.oid = JDBCUtils.safeGetInt(dbResult, "oid");
    }

    @Override
    public JDBCStructCache<PostgreSchema, ? extends JDBCTable, ? extends JDBCTableColumn> getCache()
    {
        return getContainer().classCache;
    }

    @Override
    public int getObjectId() {
        return this.oid;
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @Override
    public Collection<PostgreAttribute> getAttributes(DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().classCache.getChildren(monitor, getContainer(), this);
    }

    @Override
    public PostgreAttribute getAttribute(DBRProgressMonitor monitor, String attributeName)
        throws DBException
    {
        return getContainer().classCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().classCache.clearChildrenCache(this);
        return true;
    }

}
