package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class  YashanDBTable extends YashanDBTablePhysical implements DBPScriptObject, DBDPseudoAttributeContainer,
        DBPObjectStatistics, DBPImageProvider, DBPReferentialIntegrityController, DBPScriptObjectExt2 {

    private static final Log log = Log.getLog(YashanDBTable.class);

    private static final CharSequence TABLE_NAME_PLACEHOLDER = "%table_name%";
    private static final CharSequence FOREIGN_KEY_NAME_PLACEHOLDER = "%foreign_key_name%";
    private static final String DISABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE " + TABLE_NAME_PLACEHOLDER + " MODIFY CONSTRAINT "
            + FOREIGN_KEY_NAME_PLACEHOLDER + " DISABLE";
    private static final String ENABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE " + TABLE_NAME_PLACEHOLDER + " MODIFY CONSTRAINT "
            + FOREIGN_KEY_NAME_PLACEHOLDER + " ENABLE";

    // the options which relates to DDL on foreign keys.
    private static final String[] supportedOptions = new String[]{
            DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS,
            DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS
    };

    // the priorities of table that will be showed on right UI.
    private String shardedKey;
    private String tableType;
    private String editTableType;
    private boolean temporary;
    private boolean editTemporary;
    private boolean secondary;
    private boolean sharded;
    private transient volatile Long tableSize;

    /**
     * the definition of items in statistics.
     */
    public class AdditionalInfo extends TableAdditionalInfo {
        private int pctFree;
        private int iniTrans;
        private int maxTrans;
        private int blocks;
        private int emptyBlocks;

        @Property(category = DBConstants.CAT_STATISTICS, order = 31)
        public int getPctFree() {
            return pctFree;
        }

        @Property(category = DBConstants.CAT_STATISTICS, order = 33)
        public int getIniTrans() {
            return iniTrans;
        }

        @Property(category = DBConstants.CAT_STATISTICS, order = 34)
        public int getMaxTrans() {
            return maxTrans;
        }

        @Property(category = DBConstants.CAT_STATISTICS, order = 42)
        public int getBlocks() {
            return blocks;
        }

        @Property(category = DBConstants.CAT_STATISTICS, order = 43)
        public int getEmptyBlocks() {
            return emptyBlocks;
        }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public YashanDBTable(YashanDBSchema schema, String name) {
        super(schema, name);
    }

    public YashanDBTable(DBRProgressMonitor monitor, YashanDBSchema schema, ResultSet dbResult) {
        super(schema, dbResult);

        String objectType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
        if(objectType == null){
            log.error("Can't Get Object Type, query Yashan table failed");
        }
        if("TABLE".equals(objectType)){
            String tType = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE");
            this.tableType = Objects.isNull(tType) ? "N/A" : tType;
            this.temporary = JDBCUtils.safeGetBoolean(dbResult, "TEMPORARY", "Y");
            this.secondary = JDBCUtils.safeGetBoolean(dbResult, "SECONDARY", "Y");
            this.sharded = JDBCUtils.safeGetBoolean(dbResult, "SHARDED", "Y");

            // if table is partition, need to get partition keys.
            if(this.sharded){
                String objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
                assert objectName != null;
                List<String> distKeyColumns = getDistKeyColumns(monitor, schema.getName(), objectName);
                shardedKey = String.join(",", distKeyColumns);
            }
        }
    }

    /**
     * viewable equals false, this priority will not be showed in right UI.
     */
    @Property(viewable = true, visibleIf = ListTablePropertyValidator.class, order = 10)
    public boolean isTemporary() {
        return temporary;
    }

    @Property(viewable = true, editable = true, updatable = false, visibleIf = EditTablePropertyValidator.class, order = 11)
    public boolean isEditTemporary() {
        return editTemporary;
    }

    public void setEditTemporary(boolean editTemporary) {
        this.editTemporary = editTemporary;
    }

    @Property(hidden = true, order = 11)
    public boolean isSecondary() {
        return secondary;
    }

    public boolean isSharded() {
        return sharded;
    }

    void fetchTableSize(JDBCResultSet dbResult) throws SQLException {
        tableSize = dbResult.getLong("TABLE_SIZE");
    }

    @Override
    protected String getTableTypeName() {
        return "TABLE";
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Property(viewable = true, visibleIf = ListTablePropertyValidator.class  , length = PropertyLength.TINY, order = 5)
    public String getTableType() {
        return this.tableType;
    }

    @Property(viewable = true, editable = true, updatable = false, length = PropertyLength.MULTILINE, visibleIf = EditTablePropertyValidator.class, listProvider = TableTypeListProvider.class, order = 6)
    public String getEditTableType() {
        return editTableType;
    }

    public void setEditTableType(String editTableType) {
        this.editTableType = editTableType;
    }

    public static class TableTypeListProvider implements IPropertyValueListProvider<YashanDBTable> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(YashanDBTable object) {
            if(object.isEditTemporary()){
                return List.of( "HEAP").toArray(new String[0]);
            }
            return (object.getDataSource().isDistributed() ? List.of("LSC") : List.of("HEAP", "LSC")).toArray(new String[0]);
        }
    }

    @Property(viewable = false, hidden = true, visibleIf = LSCTablePropertyValidator.class, order = 6)
    public String getShardedKey() {
        return this.shardedKey;
    }

    private List<String> getDistKeyColumns(DBRProgressMonitor monitor, String schema, String tableName){
        final String distKeyView = getDataSource().isAdminVisible() ? "DBA_DIST_KEY_COLUMNS" : "ALL_DIST_KEY_COLUMNS";
        final String sql = String.format("SELECT OWNER, NAME, COLUMN_NAME, COLUMN_POSITION  FROM %s WHERE OWNER = ? AND NAME = ?", distKeyView);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Get dist key columns")){
            try (JDBCPreparedStatement statement = session.prepareStatement(sql)) {
                statement.setString(1, schema);
                statement.setString(2, tableName);
                JDBCResultSet resultSet = statement.executeQuery();
                int row = resultSet.getRow();
                List<String> shardedKeys = new ArrayList<>(row);
                while (resultSet.next()){
                    shardedKeys.add(resultSet.getString("COlUMN_NAME"));
                }
                return shardedKeys;
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        } catch (DBCException e) {
            log.error("Get dist key columns failed", e);
        }
        return Collections.emptyList();
    }

    /**
     * it relates to iotType(index-organized table) priorities.
     */
    @Override
    public DBPImage getObjectImage() {
        return null;
    }

    /**
     * get DDL.
     */
    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return getDDL(monitor, YashanDBDDLFormat.getCurrentFormat(getDataSource()), options);
    }

    /**
     * it relates to iotType and ROWID priorities.
     */
    @Override
    public DBDPseudoAttribute[] getPseudoAttributes() throws DBException {
        return new DBDPseudoAttribute[0];
    }

    /**
     * refresh Table Object which call the refreshObject function in YashanDBTableBase.
     */
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        getContainer().foreignKeyCache.clearObjectCache(this);
        if (tableSize != null) {
            tableSize = null;
            getTableSize(monitor);
        }
        return super.refreshObject(monitor);
    }

    /**
     * Get columns of current table.
     */
    @Override
    public YashanDBTableColumn getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException {
        return super.getAttribute(monitor, attributeName);
    }

    /**
     * Get Foreign Key of current table.
     */
    @Override
    public Collection<YashanDBTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
            throws DBException {
        List<YashanDBTableForeignKey> refs = new ArrayList<>();
        // This is dummy implementation
        // Get references from this schema only
        final Collection<YashanDBTableForeignKey> allForeignKeys =
                getContainer().foreignKeyCache.getObjects(monitor, getContainer(), null);
        for (YashanDBTableForeignKey constraint : allForeignKeys) {
            if (constraint.getReferencedTable() == this) {
                refs.add(constraint);
            }
        }
        return refs;
    }

    /**
     * associate with foreign key in table.
     */
    @Override
    @Association
    public Collection<YashanDBTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
            throws DBException {
        return getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
    }

    ///////////////////////////////////

    /**
     * Statistics.
     */

    @Override
    public TableAdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    /**
     * getAdditionalInfo(Statistics).
     */
    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded && monitor != null) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    /**
     * getTableSize function.
     */

    @Property(viewable = false, category = DBConstants.CAT_STATISTICS, formatter = ByteNumberFormat.class)
    public Long getTableSize(DBRProgressMonitor monitor) throws DBCException {
        if (tableSize == null) {
            loadSize(monitor);
        }
        return tableSize;
    }

    public void setTableSize(Long tableSize) {
        this.tableSize = tableSize;
    }

    /** get table size from DBA_SEGMENTS.*/
    private void loadSize(DBRProgressMonitor monitor) throws DBCException {
        tableSize = null;
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            boolean hasDBA = getDataSource().isViewAvailable(monitor, YashanDBConstants.SCHEMA_SYS, "DBA_SEGMENTS");
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT SUM(bytes) TABLE_SIZE\n" +
                            "FROM " + YashanDBUtils.getSysSchemaPrefix(getDataSource()) + (hasDBA ? "DBA_SEGMENTS" : "USER_SEGMENTS") + " s\n" +
                            "WHERE S.SEGMENT_TYPE='TABLE' AND s.SEGMENT_NAME = ?" + (hasDBA ? " AND s.OWNER = ?" : ""))) {
                dbStat.setString(1, getName());
                if (hasDBA) {
                    dbStat.setString(2, getSchema().getName());
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {

                        fetchTableSize(dbResult);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error reading table statistics", e);
        } finally {
            if (tableSize == null) {
                tableSize = 0L;
            }
        }
    }

    /**
     * get additionalInfo from view(DBA_/ALL_).
     */
    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM " + YashanDBUtils.isAdminPriv(getDataSource(),"TABLES") + " WHERE OWNER=? AND TABLE_NAME=?")) {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        additionalInfo.pctFree = JDBCUtils.safeGetInt(dbResult, "PCT_FREE");
                        additionalInfo.iniTrans = JDBCUtils.safeGetInt(dbResult, "INI_TRANS");
                        additionalInfo.maxTrans = JDBCUtils.safeGetInt(dbResult, "MAX_TRANS");
                        additionalInfo.blocks = JDBCUtils.safeGetInt(dbResult, "BLOCKS");
                        additionalInfo.emptyBlocks = JDBCUtils.safeGetInt(dbResult, "EMPTY_BLOCKS");
                    } else {
                        log.warn("Cannot find table '" + getFullyQualifiedName(DBPEvaluationContext.UI) + "' metadata");
                    }
                    additionalInfo.loaded = true;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }

    }

    @Override
    public boolean hasStatistics() {
        return tableSize != null;
    }

    @Override
    public long getStatObjectSize() {
        return tableSize == null ? 0 : tableSize;
    }

    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return ArrayUtils.contains(supportedOptions, option);
    }

    @Override
    public void enableReferentialIntegrity(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        Collection<YashanDBTableForeignKey> foreignKeys = getAssociations(monitor);
        if (CommonUtils.isEmpty(foreignKeys)) {
            return;
        }

        String template;
        if (enable) {
            template = ENABLE_REFERENTIAL_INTEGRITY_STATEMENT;
        } else {
            template = DISABLE_REFERENTIAL_INTEGRITY_STATEMENT;
        }
        template = template.replace(TABLE_NAME_PLACEHOLDER, getFullyQualifiedName(DBPEvaluationContext.DDL));

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Changing referential integrity")) {
            try (JDBCStatement statement = session.createStatement()) {
                for (DBPNamedObject fk : foreignKeys) {
                    String sql = template.replace(FOREIGN_KEY_NAME_PLACEHOLDER, fk.getName());
                    statement.executeUpdate(sql);
                }
            } catch (SQLException e) {
                throw new DBException("Unable to change referential integrity", e);
            }
        }
    }

    @Override
    public boolean supportsChangingReferentialIntegrity(DBRProgressMonitor monitor) throws DBException {
        return !CommonUtils.isEmpty(getAssociations(monitor));
    }

    @Override
    public String getChangeReferentialIntegrityStatement(DBRProgressMonitor monitor, boolean enable) throws DBException {
        if (!supportsChangingReferentialIntegrity(monitor)) {
            return null;
        }
        if (enable) {
            return ENABLE_REFERENTIAL_INTEGRITY_STATEMENT;
        }
        return DISABLE_REFERENTIAL_INTEGRITY_STATEMENT;
    }

    public static class LSCTablePropertyValidator implements IPropertyValueValidator<YashanDBTable, Object> {
        @Override
        public boolean isValidValue(YashanDBTable object, Object value) throws IllegalArgumentException {
            return object.isSharded();
        }
    }

    public static class ListTablePropertyValidator implements IPropertyValueValidator<YashanDBTable, Object> {
        @Override
        public boolean isValidValue(YashanDBTable object, Object value) throws IllegalArgumentException {
            return object.getTableType() != null && !object.getDataSource().isDistributed();
        }
    }

    public static class EditTablePropertyValidator implements IPropertyValueValidator<YashanDBTable, Object> {
        @Override
        public boolean isValidValue(YashanDBTable object, Object value) throws IllegalArgumentException {
            return object.getTableType() == null && !object.getDataSource().isDistributed();
        }
    }
}
