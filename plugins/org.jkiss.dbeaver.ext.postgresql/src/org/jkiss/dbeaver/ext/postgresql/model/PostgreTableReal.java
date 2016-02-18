/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PostgreTable base
 */
public abstract class PostgreTableReal extends PostgreTableBase
{
    static final Log log = Log.getLog(PostgreTableReal.class);

    private int oid;
    private String description;
    final TriggerCache triggerCache = new TriggerCache();

    protected PostgreTableReal(PostgreSchema catalog)
    {
        super(catalog);
    }

    protected PostgreTableReal(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @Override
    public Collection<PostgreTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchema().constraintCache.getTypedObjects(monitor, getSchema(), this, PostgreTableConstraint.class);
    }

    public PostgreTableConstraintBase getConstraint(@NotNull DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return getSchema().constraintCache.getObject(monitor, getSchema(), this, ukName);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        triggerCache.clearCache();
        super.refreshObject(monitor);

        return true;
    }

    @Association
    public Collection<PostgreTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    public PostgreTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, this, name);
    }

    class TriggerCache extends JDBCObjectCache<PostgreTableReal, PostgreTrigger> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreTableReal owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT x.oid,x.* FROM pg_catalog.pg_trigger x" +
                "\nWHERE x.tgrelid=" + owner.getObjectId() + " AND NOT x.tgisinternal");
        }

        @Override
        protected PostgreTrigger fetchObject(@NotNull JDBCSession session, @NotNull PostgreTableReal owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreTrigger(owner, dbResult);
        }

    }

}
