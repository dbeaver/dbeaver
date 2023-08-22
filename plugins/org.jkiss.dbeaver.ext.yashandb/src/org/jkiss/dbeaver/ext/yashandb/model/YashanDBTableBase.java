package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBStatefulObject;
import org.jkiss.dbeaver.model.*;
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
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public abstract class YashanDBTableBase extends JDBCTable<YashanDBDataSource, YashanDBSchema>
        implements DBPNamedObject2, DBPRefreshableObject, YashanDBStatefulObject, DBPObjectWithLazyDescription {
    private static final Log log = Log.getLog(YashanDBTableBase.class);

    protected boolean valid;
    private String comment;

    public static class TableAdditionalInfo {
        volatile boolean loaded = false;

        boolean isLoaded() {
            return loaded;
        }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<YashanDBTableBase> {
        @Override
        public boolean isPropertyCached(YashanDBTableBase object, Object propertyId) {
            return object.getAdditionalInfo().isLoaded();
        }
    }

    public static class CommentsValidator implements IPropertyCacheValidator<YashanDBTableBase> {
        @Override
        public boolean isPropertyCached(YashanDBTableBase object, Object propertyId) {
            return object.comment != null;
        }
    }

    protected abstract String getTableTypeName();

    private final TablePrivCache tablePrivCache = new TablePrivCache();

    public abstract TableAdditionalInfo getAdditionalInfo();

    protected YashanDBTableBase(YashanDBSchema schema, String name, boolean persisted) {
        super(schema, name, persisted);
    }

    /**
     * set tableName property from dbResult,which will be showed in right UI.
     */
    protected YashanDBTableBase(YashanDBSchema schema, ResultSet dbResult) {
        super(schema, true);
        setName(JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"));
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
    }

    /**
     * ???.
     */
    @Override
    public JDBCStructCache<YashanDBSchema, ? extends JDBCTable, ? extends JDBCTableColumn> getCache() {

        return getContainer().tableCache;
    }

    @Override
    public YashanDBSchema getSchema() {
        return super.getContainer();
    }


    @Nullable
    @Override
    public String getDescription() {
        return getComment();
    }

    /**
     * getComment by queryTableComment.
     */
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    @LazyProperty(cacheValidator = CommentsValidator.class)
    public String getComment(DBRProgressMonitor monitor) {
        if (comment == null) {
            comment = "";
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table comments")) {
                comment = queryTableComment(session);
                if (comment == null) {
                    comment = "";
                }
            } catch (Exception e) {
                log.error("Can't fetch table '" + getName() + "' comment", e);
            }
        }
        return comment;
    }


    @Nullable
    @Override
    public String getDescription(DBRProgressMonitor monitor) {
        return getComment(monitor);
    }

    /**
     * getComment by search sql.
     */
    protected String queryTableComment(JDBCSession session) throws SQLException {
        return JDBCUtils.queryString(
                session,
                "SELECT COMMENTS FROM " + YashanDBUtils.isAdminPriv((YashanDBDataSource) session.getDataSource(), "TAB_COMMENTS") + " WHERE OWNER = ? and TABLE_NAME = ? AND TABLE_TYPE = ?"
                ,
                getSchema().getName(),
                getName(),
                getTableTypeName());
    }


    /**
     * get column comments searching (ALL/DBA)_COL_COMMENTS.
     */
    void loadColumnComments(DBRProgressMonitor monitor) {
        try {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table column comments")) {
                try (JDBCPreparedStatement stat = session.prepareStatement("SELECT distinct (COLUMN_NAME),COMMENTS FROM " +
                        YashanDBUtils.isAdminPriv((YashanDBDataSource) session.getDataSource(), "COL_COMMENTS") + " CC " +
                        "WHERE CC.OWNER=? AND CC.TABLE_NAME=?")) {
                    stat.setString(1, getSchema().getName());
                    stat.setString(2, getName());
                    try (JDBCResultSet resultSet = stat.executeQuery()) {
                        while (resultSet.next()) {
                            String colName = resultSet.getString(1);
                            String colComment = resultSet.getString(2);
                            YashanDBTableColumn col = getAttribute(monitor, colName);
                            if (col == null) {
                                log.warn("Column '" + colName + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            } else {
                                col.setComment(CommonUtils.notEmpty(colComment));
                            }
                        }
                    }
                }
            }
            for (YashanDBTableColumn col : CommonUtils.safeCollection(getAttributes(monitor))) {
                col.cacheComment();
            }
        } catch (Exception e) {
            log.warn("Error fetching table '" + getName() + "' column comments", e);
        }
    }

    public static YashanDBTableBase findTable(DBRProgressMonitor monitor, YashanDBDataSource dataSource, String ownerName, String tableName) throws DBException {
        YashanDBSchema refSchema = dataSource.getSchema(monitor, ownerName);
        if (refSchema == null) {
            log.warn("Referenced schema '" + ownerName + "' not found");
            return null;
        } else {
            YashanDBTableBase refTable = refSchema.tableCache.getObject(monitor, refSchema, tableName);
            if (refTable == null) {
                log.warn("Referenced table '" + tableName + "' not found in schema '" + ownerName + "'");
            }
            return refTable;
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return super.getName();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
                getContainer(),
                this);
    }

    /**
     * The DDL context source.
     */
    public String getDDL(DBRProgressMonitor monitor, YashanDBDDLFormat ddlFormat, Map<String, Object> options)
            throws DBException {
        // TODO: Complete getDDL function.
        return YashanDBUtils.getTableOrViewDDL(monitor, getTableTypeName(), this, options);
//        return "The text is coming soon!";
    }

    @Override
    public List<YashanDBTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
            throws DBException {
        return getContainer().tableCache.getChildren(monitor, getContainer(), this);
    }

    @Override
    public YashanDBTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
            throws DBException {
        return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        getContainer().constraintCache.clearObjectCache(this);
        getContainer().tableTriggerCache.clearObjectCache(this);
        return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
    }

    @Nullable
    @Override
    @Association
    public Collection<YashanDBTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor)
            throws DBException {
        return getContainer().constraintCache.getObjects(monitor, getContainer(), this);
    }

    public YashanDBTableConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
            throws DBException {
        return getContainer().constraintCache.getObject(monitor, getContainer(), this, ukName);
    }

    public DBSTableForeignKey getForeignKey(DBRProgressMonitor monitor, String ukName) throws DBException {
        return DBUtils.findObject(getAssociations(monitor), ukName);
    }

    @Override
    public Collection<YashanDBTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<YashanDBTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Association
    public List<YashanDBTableTrigger> getTriggers(@NotNull DBRProgressMonitor monitor)
            throws DBException {
        return getSchema().tableTriggerCache.getObjects(monitor, getSchema(), this);
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    /**
     * the association return into UI.
     */
    @Association
    public Collection<YashanDBDependencyGroup> getDependencies(DBRProgressMonitor monitor) {
        return YashanDBDependencyGroup.of(this);
    }

    @Association
    public Collection<YashanDBPrivTable> getTablePrivs(DBRProgressMonitor monitor) throws DBException {
        return tablePrivCache.getAllObjects(monitor, this);
    }

    /**
     * TablePrivileges cache.
     */
    static class TablePrivCache extends JDBCObjectCache<YashanDBTableBase, YashanDBPrivTable> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBTableBase tableBase) throws SQLException {
            boolean hasDBA = tableBase.getDataSource().isViewAvailable(session.getProgressMonitor(), YashanDBConstants.SCHEMA_SYS, YashanDBConstants.VIEW_DBA_TAB_PRIVS);
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT p.*\n" +
                            "FROM " + (hasDBA ? "DBA_TAB_PRIVS p" : "ALL_TAB_PRIVS p") + "\n" +
                            "WHERE p." + (hasDBA ? "OWNER" : "TABLE_SCHEMA") + "=? AND p.TABLE_NAME =?");
            dbStat.setString(1, tableBase.getSchema().getName());
            dbStat.setString(2, tableBase.getName());
            return dbStat;
        }

        @Override
        protected YashanDBPrivTable fetchObject(@NotNull JDBCSession session, @NotNull YashanDBTableBase tableBase, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBPrivTable(tableBase, resultSet);
        }
    }

}
