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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * PostgreTable base
 */
public abstract class PostgreTableBase extends JDBCTable<PostgreDataSource, PostgreSchema> implements PostgreClass, PostgreScriptObject, DBPNamedObject2
{
    private long oid;
    private String description;

    protected PostgreTableBase(PostgreSchema catalog)
    {
        super(catalog, false);
    }

    protected PostgreTableBase(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, JDBCUtils.safeGetString(dbResult, "relname"), true);
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
    }

    // Copy constructor
    public PostgreTableBase(PostgreSchema container, DBSEntity source, boolean persisted) {
        super(container, source, persisted);
        if (source instanceof PostgreTableBase) {
            this.description = ((PostgreTableBase) source).description;
        }
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

    @Property(viewable = true, editable = false, updatable = false, order = 9)
    @Override
    public long getObjectId() {
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
    public String getFullyQualifiedName(DBPEvaluationContext context)
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
    public List<PostgreTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().tableCache.getChildren(monitor, getContainer(), this);
    }

    @Override
    public PostgreTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public Collection<PostgreTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    public PostgreTableConstraintBase getConstraint(@NotNull DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return null;
    }

    @Override
    @Association
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @Association
    @Override
    public synchronized Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().constraintCache.clearObjectCache(this);
        getContainer().indexCache.clearObjectCache(this);
        return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
    }

}
