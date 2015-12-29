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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * PostgreTable base
 */
public abstract class PostgreTableBase extends JDBCTable<PostgreDataSource, PostgreSchema> implements PostgreClass, PostgreScriptObject
{
    static final Log log = Log.getLog(PostgreTableBase.class);

    private int oid;
    private ConstraintCache constraintCache = new ConstraintCache();

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
    public JDBCStructCache<PostgreSchema, ? extends PostgreClass, ? extends PostgreAttribute> getCache()
    {
        return getContainer().classCache;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return getContainer().getDatabase();
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
    public Collection<? extends DBSTableConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException {
        return constraintCache.getAllObjects(monitor, this);
    }

    public PostgreTableConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return constraintCache.getObject(monitor, this, ukName);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().classCache.clearChildrenCache(this);
        constraintCache.clearCache();

        return true;
    }

    /**
     * Constraint cache implementation
     */
    class ConstraintCache extends JDBCObjectCache<PostgreTableBase, PostgreTableConstraint> {

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, PostgreTableBase owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.oid,c.*" +
                "\nFROM pg_catalog.pg_constraint c" +
                "\nWHERE c.conrelid=?" +
                "\nORDER BY c.oid");
            dbStat.setInt(1, owner.getObjectId());
            return dbStat;
        }

        @Override
        protected PostgreTableConstraint fetchObject(@NotNull JDBCSession session, @NotNull PostgreTableBase table, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "conname");
            return new PostgreTableConstraint(table, name, resultSet);
        }
    }

}
