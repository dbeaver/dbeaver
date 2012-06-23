/*
 *
 *  * Copyright (C) 2010-2012 Serge Rieder
 *  * serge@jkiss.org
 *  *
 *  * This library is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU Lesser General Public
 *  * License as published by the Free Software Foundation; either
 *  * version 2.1 of the License, or (at your option) any later version.
 *  *
 *  * This library is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this library; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleStatefulObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * OracleTable base
 */
public abstract class OracleTableBase extends JDBCTable<OracleDataSource, OracleSchema>
    implements DBPNamedObject2, DBPRefreshableObject, OracleStatefulObject
{
    static final Log log = LogFactory.getLog(OracleTableBase.class);

    public static class TableAdditionalInfo {
        volatile boolean loaded = false;

        public boolean isLoaded() { return loaded; }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<OracleTableBase> {
        @Override
        public boolean isPropertyCached(OracleTableBase object, Object propertyId)
        {
            return object.getAdditionalInfo().isLoaded();
        }
    }

    public static class CommentsValidator implements IPropertyCacheValidator<OracleTableBase> {
        @Override
        public boolean isPropertyCached(OracleTableBase object, Object propertyId)
        {
            return object.comment != null;
        }
    }

    public abstract TableAdditionalInfo getAdditionalInfo();

    protected abstract String getTableTypeName();

    protected boolean valid;
    private String comment;
    private List<OracleTableColumn> columns;
    private List<OracleTableConstraint> constraints;

    protected OracleTableBase(OracleSchema schema, String name, boolean persisted)
    {
        super(schema, name, persisted);
    }

    protected OracleTableBase(OracleSchema oracleSchema, ResultSet dbResult)
    {
        super(oracleSchema, true);
        setName(JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
        //this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
    }

    @Override
    public OracleSchema getSchema()
    {
        return super.getContainer();
    }

    @Override
    @Property(name = "Name", viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1, description = "Name of the table")
    public String getName()
    {
        return super.getName();
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @Property(name = "Comments", viewable = true, editable = true, order = 100)
    @LazyProperty(cacheValidator = CommentsValidator.class)
    public String getComment(DBRProgressMonitor monitor)
        throws DBException
    {
        if (comment == null) {
            final JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table comments");
            try {
                comment = JDBCUtils.queryString(
                    context,
                    "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE OWNER=? AND TABLE_NAME=? AND TABLE_TYPE=?",
                    getSchema().getName(),
                    getName(),
                    getTableTypeName());
                if (comment == null) {
                    comment = "";
                }
            } catch (SQLException e) {
                log.warn("Can't fetch table '" + getName() + "' comment", e);
            } finally {
                context.close();
            }
        }
        return comment;
    }

    @Override
    public Collection<OracleTableColumn> getColumns(DBRProgressMonitor monitor)
        throws DBException
    {
        if (columns == null) {
            getContainer().tableCache.loadChildren(monitor, getContainer(), this);
        }
        return columns;
    }

    @Override
    public OracleTableColumn getColumn(DBRProgressMonitor monitor, String columnName)
        throws DBException
    {
        return DBUtils.findObject(getColumns(monitor), columnName);
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        columns = null;
        constraints = null;
        return true;
    }

    public boolean isColumnsCached()
    {
        return columns != null;
    }

    public void setColumns(List<OracleTableColumn> columns)
    {
        this.columns = columns;
    }

    @Association
    public Collection<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().triggerCache.getObjects(monitor, getContainer(), this);
    }

    @Override
    public List<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    @Association
    public List<OracleTableConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        if (constraints == null) {
            getContainer().constraintCache.getObjects(monitor, getContainer(), this);
        }
        return constraints;
    }

    public OracleTableConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return DBUtils.findObject(getConstraints(monitor), ukName);
    }

    void setConstraints(List<OracleTableConstraint> constraints)
    {
        this.constraints = constraints;
    }

    List<OracleTableConstraint> getConstraintsCache()
    {
        return constraints;
    }

    public DBSTableForeignKey getForeignKey(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return DBUtils.findObject(getAssociations(monitor), ukName);
    }

    @Override
    public List<? extends DBSTableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public List<? extends DBSTableForeignKey> getReferences(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public String getDDL(DBRProgressMonitor monitor)
        throws DBException
    {
        return OracleUtils.getDDL(monitor, getTableTypeName(), this);
    }

    @Override
    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    public static OracleTableBase findTable(DBRProgressMonitor monitor, OracleDataSource dataSource, String ownerName, String tableName) throws DBException
    {
        OracleSchema refSchema = dataSource.getSchema(monitor, ownerName);
        if (refSchema == null) {
            log.warn("Referenced schema '" + ownerName + "' not found");
            return null;
        } else {
            OracleTableBase refTable = refSchema.tableCache.getObject(monitor, refSchema, tableName);
            if (refTable == null) {
                log.warn("Referenced table '" + tableName + "' not found in schema '" + ownerName + "'");
            }
            return refTable;
        }
    }

}
