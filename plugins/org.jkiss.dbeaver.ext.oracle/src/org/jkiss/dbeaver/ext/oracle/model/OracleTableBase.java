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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleStatefulObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * OracleTable base
 */
public abstract class OracleTableBase extends JDBCTable<OracleDataSource, OracleSchema>
    implements DBPNamedObject2, DBPRefreshableObject, OracleStatefulObject
{
    private static final Log log = Log.getLog(OracleTableBase.class);

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
    public JDBCStructCache<OracleSchema, ? extends JDBCTable, ? extends JDBCTableColumn> getCache()
    {
        return getContainer().tableCache;
    }

    @Override
    @NotNull
    public OracleSchema getSchema()
    {
        return super.getContainer();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @Property(viewable = true, editable = true, updatable = true, order = 100)
    @LazyProperty(cacheValidator = CommentsValidator.class)
    public synchronized String getComment(DBRProgressMonitor monitor)
        throws DBException
    {
        if (comment == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load table comments")) {
                comment = JDBCUtils.queryString(
                    session,
                    "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE OWNER=? AND TABLE_NAME=? AND TABLE_TYPE=?",
                    getSchema().getName(),
                    getName(),
                    getTableTypeName());
                if (comment == null) {
                    comment = "";
                }
            } catch (SQLException e) {
                log.warn("Can't fetch table '" + getName() + "' comment", e);
            }
        }
        return comment;
    }

    void loadColumnComments(DBRProgressMonitor monitor) {
        try {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load table column comments")) {
                try (JDBCPreparedStatement stat = session.prepareStatement("SELECT COLUMN_NAME,COMMENTS FROM SYS.ALL_COL_COMMENTS cc WHERE CC.OWNER=? AND cc.TABLE_NAME=?")) {
                    stat.setString(1, getSchema().getName());
                    stat.setString(2, getName());
                    try (JDBCResultSet resultSet = stat.executeQuery()) {
                        while (resultSet.next()) {
                            String colName = resultSet.getString(1);
                            String colComment = resultSet.getString(2);
                            OracleTableColumn col = getAttribute(monitor, colName);
                            if (col == null) {
                                log.warn("Column '" + colName + "' not found in table '" + getFullQualifiedName() + "'");
                            } else {
                                col.setComment(CommonUtils.notEmpty(colComment));
                            }
                        }
                    }
                }
            }
            for (OracleTableColumn col : getAttributes(monitor)) {
                col.cacheComment();
            }
        } catch (Exception e) {
            log.warn("Error fetching table '" + getName() + "' column comments", e);
        }
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    @Override
    public Collection<OracleTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().tableCache.getChildren(monitor, getContainer(), this);
    }

    @Override
    public OracleTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().tableCache.clearChildrenCache(this);
        getContainer().constraintCache.clearObjectCache(this);

        return true;
    }

    @Association
    public Collection<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().triggerCache.getObjects(monitor, getContainer(), this);
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    @Association
    public synchronized Collection<OracleTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().constraintCache.getObjects(monitor, getContainer(), this);
    }

    public OracleTableConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return getContainer().constraintCache.getObject(monitor, getContainer(), this, ukName);
    }

    public DBSTableForeignKey getForeignKey(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return DBUtils.findObject(getAssociations(monitor), ukName);
    }

    @Override
    public Collection<OracleTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public Collection<OracleTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public String getDDL(DBRProgressMonitor monitor, OracleDDLFormat ddlFormat)
        throws DBException
    {
        return OracleUtils.getDDL(monitor, getTableTypeName(), this, ddlFormat);
    }

    @NotNull
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
