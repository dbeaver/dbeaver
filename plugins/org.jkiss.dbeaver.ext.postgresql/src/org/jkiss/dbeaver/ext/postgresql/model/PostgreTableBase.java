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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PostgreTable base
 */
public abstract class PostgreTableBase extends JDBCTable<PostgreDataSource, PostgreSchema> implements PostgreClass, PostgreScriptObject
{
    static final Log log = Log.getLog(PostgreTableBase.class);

    private int oid;
    private String description;
    final TriggerCache triggerCache = new TriggerCache();

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
        this.description = JDBCUtils.safeGetString(dbResult, "description");
    }

    @Override
    public JDBCStructCache<PostgreSchema, ? extends PostgreClass, ? extends PostgreAttribute> getCache()
    {
        return getContainer().tableCache;
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

    @Property(viewable = true, editable = true, updatable = true, order = 10)
    @Nullable
    @Override
    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @NotNull
    public PostgreSchema getSchema() {
        final DBSObject parentObject = super.getParentObject();
        assert parentObject != null;
        return (PostgreSchema) parentObject;
    }

    @Override
    public List<PostgreTableColumn> getAttributes(DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().tableCache.getChildren(monitor, getContainer(), this);
    }

    @Override
    public PostgreTableColumn getAttribute(DBRProgressMonitor monitor, String attributeName)
        throws DBException
    {
        return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public Collection<PostgreTableConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException {
        return getSchema().constraintCache.getTypedObjects(monitor, getSchema(), this, PostgreTableConstraint.class);
    }

    public PostgreTableConstraintBase getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return getSchema().constraintCache.getObject(monitor, getSchema(), this, ukName);
    }

    @Override
    @Association
    public Collection<PostgreTableForeignKey> getReferences(DBRProgressMonitor monitor)
        throws DBException
    {
        List<PostgreTableForeignKey> refs = new ArrayList<>();
        // This is dummy implementation
        // Get references from this schema only
        final Collection<PostgreTableForeignKey> allForeignKeys =
            getContainer().constraintCache.getTypedObjects(monitor, getContainer(), PostgreTableForeignKey.class);
        for (PostgreTableForeignKey constraint : allForeignKeys) {
            if (constraint.getAssociatedEntity() == this) {
                refs.add(constraint);
            }
        }
        return refs;
    }

    @Association
    @Override
    public synchronized Collection<PostgreTableForeignKey> getAssociations(DBRProgressMonitor monitor)
        throws DBException
    {
        return getSchema().constraintCache.getTypedObjects(monitor, getSchema(), this, PostgreTableForeignKey.class);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().tableCache.clearChildrenCache(this);
        getContainer().constraintCache.clearObjectCache(this);
        triggerCache.clearCache();

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

    class TriggerCache extends JDBCObjectCache<PostgreTableBase, PostgreTrigger> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreTableBase owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SELECT x.oid,x.* FROM pg_catalog.pg_trigger x" +
                "\nWHERE x.tgrelid=" + owner.getObjectId() + " AND NOT x.tgisinternal");
        }

        @Override
        protected PostgreTrigger fetchObject(@NotNull JDBCSession session, @NotNull PostgreTableBase owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreTrigger(owner, dbResult);
        }

    }

}
