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
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
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
    //private ConstraintCache constraintCache = new ConstraintCache();

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

    @NotNull
    public PostgreSchema getSchema() {
        final DBSObject parentObject = super.getParentObject();
        assert parentObject != null;
        return (PostgreSchema) parentObject;
    }

    @Override
    public List<PostgreAttribute> getAttributes(DBRProgressMonitor monitor)
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
        getContainer().classCache.clearChildrenCache(this);
        getContainer().constraintCache.clearObjectCache(this);

        return true;
    }

    /**
     * Constraint cache implementation
     *
    class ConstraintCache extends JDBCObjectCache<PostgreTableBase, PostgreTableConstraintBase> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreTableBase owner)
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
        protected PostgreTableConstraintBase fetchObject(@NotNull JDBCSession session, @NotNull PostgreTableBase table, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "conname");
            String type = JDBCUtils.safeGetString(resultSet, "contype");
            if (type == null) {
                log.warn("Null constraint type");
                return null;
            }
            DBSEntityConstraintType constraintType;
            switch (type) {
                case "c": constraintType = DBSEntityConstraintType.CHECK; break;
                case "f": constraintType = DBSEntityConstraintType.FOREIGN_KEY; break;
                case "p": constraintType = DBSEntityConstraintType.PRIMARY_KEY; break;
                case "u": constraintType = DBSEntityConstraintType.UNIQUE_KEY; break;
                case "t": constraintType = PostgreConstants.CONSTRAINT_TRIGGER; break;
                case "x": constraintType = PostgreConstants.CONSTRAINT_EXCLUSIVE; break;
                default:
                    log.warn("Unsupported constraint type");
                    return null;
            }
            if (constraintType == DBSEntityConstraintType.FOREIGN_KEY) {
                return new PostgreTableForeignKey(table, name, resultSet);
            } else {
                return new PostgreTableConstraint(table, name, constraintType, resultSet);
            }
        }
    }
     */
}
