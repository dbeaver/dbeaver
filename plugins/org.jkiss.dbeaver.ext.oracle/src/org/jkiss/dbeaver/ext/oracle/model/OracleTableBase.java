/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleStatefulObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * OracleTable base
 */
public abstract class OracleTableBase extends JDBCTable<OracleDataSource, OracleSchema>
    implements DBPNamedObject2, DBPRefreshableObject, OracleStatefulObject
{
    private static final Log log = Log.getLog(OracleTableBase.class);

    public static class TableAdditionalInfo {
        volatile boolean loaded = false;

        boolean isLoaded() { return loaded; }
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

    public final TriggerCache triggerCache = new TriggerCache();
    private final TablePrivCache tablePrivCache = new TablePrivCache();

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
        return getComment();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
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
                                log.warn("Column '" + colName + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
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
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().constraintCache.clearObjectCache(this);

        return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
    }

    @Association
    public Collection<OracleTableTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
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

    public String getDDL(DBRProgressMonitor monitor, OracleDDLFormat ddlFormat, Map<String, Object> options)
        throws DBException
    {
        return OracleUtils.getDDL(monitor, getTableTypeName(), this, ddlFormat, options);
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

    @Association
    public Collection<OraclePrivTable> getTablePrivs(DBRProgressMonitor monitor) throws DBException
    {
        return tablePrivCache.getAllObjects(monitor, this);
    }


    static class TriggerCache extends JDBCStructCache<OracleTableBase, OracleTableTrigger, OracleTriggerColumn> {
        TriggerCache()
        {
            super("TRIGGER_NAME");
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleTableBase owner) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT *\n" +
                    "FROM " + OracleUtils.getAdminAllViewPrefix(owner.getDataSource()) + "TRIGGERS WHERE TABLE_OWNER=? AND TABLE_NAME=?\n" +
                    "ORDER BY TRIGGER_NAME");
            dbStat.setString(1, owner.getSchema().getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleTableTrigger fetchObject(@NotNull JDBCSession session, @NotNull OracleTableBase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleTableTrigger(owner, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull OracleTableBase owner, @Nullable OracleTableTrigger forObject) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TRIGGER_NAME,TABLE_OWNER,TABLE_NAME,COLUMN_NAME,COLUMN_LIST,COLUMN_USAGE\n" +
                    "FROM SYS.ALL_TRIGGER_COLS WHERE TABLE_OWNER=? AND TABLE_NAME=?" +
                    (forObject == null ? "" : " AND TRIGGER_NAME=?") +
                    "\nORDER BY TRIGGER_NAME");
            dbStat.setString(1, owner.getContainer().getName());
            dbStat.setString(2, owner.getName());
            if (forObject != null) {
                dbStat.setString(3, forObject.getName());
            }
            return dbStat;
        }

        @Override
        protected OracleTriggerColumn fetchChild(@NotNull JDBCSession session, @NotNull OracleTableBase owner, @NotNull OracleTableTrigger parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
        {
            OracleTableBase refTable = OracleTableBase.findTable(
                session.getProgressMonitor(),
                owner.getDataSource(),
                JDBCUtils.safeGetString(dbResult, "TABLE_OWNER"),
                JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
            if (refTable != null) {
                final String columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
                OracleTableColumn tableColumn = refTable.getAttribute(session.getProgressMonitor(), columnName);
                if (tableColumn == null) {
                    log.debug("Column '" + columnName + "' not found in table '" + refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "' for trigger '" + parent.getName() + "'");
                }
                return new OracleTriggerColumn(session.getProgressMonitor(), parent, tableColumn, dbResult);
            }
            return null;
        }

    }

    static class TablePrivCache extends JDBCObjectCache<OracleTableBase, OraclePrivTable> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleTableBase tableBase) throws SQLException
        {
            boolean hasDBA = tableBase.getDataSource().isViewAvailable(session.getProgressMonitor(), OracleConstants.SCHEMA_SYS, OracleConstants.VIEW_DBA_TAB_PRIVS);
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p.*\n" +
                    "FROM " + (hasDBA ? "DBA_TAB_PRIVS p" : "ALL_TAB_PRIVS p") + "\n" +
                    "WHERE p."+ (hasDBA ? "OWNER": "TABLE_SCHEMA") +"=? AND p.TABLE_NAME =?");
            dbStat.setString(1, tableBase.getSchema().getName());
            dbStat.setString(2, tableBase.getName());
            return dbStat;
        }

        @Override
        protected OraclePrivTable fetchObject(@NotNull JDBCSession session, @NotNull OracleTableBase tableBase, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OraclePrivTable(tableBase, resultSet);
        }
    }

}
