/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.oracle.internal.OracleMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

/**
 * OracleTable
 */
public class OracleTable extends OracleTablePhysical implements DBPScriptObject, DBDPseudoAttributeContainer,
        DBPObjectStatistics, DBPImageProvider, DBPReferentialIntegrityController, DBPScriptObjectExt2 {
    private static final Log log = Log.getLog(OracleTable.class);

    private static final DBDPseudoAttribute ROWSCN_PSEUDO_ATTRIBUTE = new DBDPseudoAttribute(
        DBDPseudoAttributeType.OTHER,
        "ORA_ROWSCN",
        null,
        null,
        OracleMessages.pseudo_column_ora_rowscn_description,
        true,
        DBDPseudoAttribute.PropagationPolicy.TABLE_LOCAL
    );


    private static final CharSequence TABLE_NAME_PLACEHOLDER = "%table_name%";
    private static final CharSequence FOREIGN_KEY_NAME_PLACEHOLDER = "%foreign_key_name%";
    private static final String DISABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE " + TABLE_NAME_PLACEHOLDER + " MODIFY CONSTRAINT "
        + FOREIGN_KEY_NAME_PLACEHOLDER + " DISABLE";
    private static final String ENABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE " + TABLE_NAME_PLACEHOLDER + " MODIFY CONSTRAINT "
        + FOREIGN_KEY_NAME_PLACEHOLDER + " ENABLE";
    
    private static final String[] supportedOptions = new String[]{
        DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS,
        DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS
    };
    
    private OracleDataType tableType;
    private String iotType;
    private String iotName;
    private boolean temporary;
    private boolean secondary;
    private boolean nested;
    private transient volatile Long tableSize;

    public class AdditionalInfo extends TableAdditionalInfo {
        private int pctFree;
        private int pctUsed;
        private int iniTrans;
        private int maxTrans;
        private int initialExtent;
        private int nextExtent;
        private int minExtents;
        private int maxExtents;
        private int pctIncrease;
        private int freelists;
        private int freelistGroups;

        private int blocks;
        private int emptyBlocks;
        private int avgSpace;
        private int chainCount;

        private int avgRowLen;
        private int avgSpaceFreelistBlocks;
        private int numFreelistBlocks;
        private Date lastStatisticsUpdate;

        @Property(category = DBConstants.CAT_STATISTICS, order = 31)
        public int getPctFree() { return pctFree; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 32)
        public int getPctUsed() { return pctUsed; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 33)
        public int getIniTrans() { return iniTrans; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 34)
        public int getMaxTrans() { return maxTrans; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 35)
        public int getInitialExtent() { return initialExtent; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 36)
        public int getNextExtent() { return nextExtent; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 37)
        public int getMinExtents() { return minExtents; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 38)
        public int getMaxExtents() { return maxExtents; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 39)
        public int getPctIncrease() { return pctIncrease; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 40)
        public int getFreelists() { return freelists; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 41)
        public int getFreelistGroups() { return freelistGroups; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 42)
        public int getBlocks() { return blocks; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 43)
        public int getEmptyBlocks() { return emptyBlocks; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 44)
        public int getAvgSpace() { return avgSpace; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 45)
        public int getChainCount() { return chainCount; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 46)
        public int getAvgRowLen() { return avgRowLen; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 47)
        public int getAvgSpaceFreelistBlocks() { return avgSpaceFreelistBlocks; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 48)
        public int getNumFreelistBlocks() { return numFreelistBlocks; }
        @Property(category = DBConstants.CAT_STATISTICS, order = 29)
        public Date getLastStatisticsUpdate() {
            return lastStatisticsUpdate;
        }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();
    private DBDPseudoAttribute[] allPseudoAttributes = null;

    public OracleTable(OracleSchema schema, String name)
    {
        super(schema, name);
    }

    public OracleTable(
        DBRProgressMonitor monitor,
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
        String typeOwner = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE_OWNER");
        if (!CommonUtils.isEmpty(typeOwner)) {
            tableType = OracleDataType.resolveDataType(
                monitor,
                schema.getDataSource(),
                typeOwner,
                JDBCUtils.safeGetString(dbResult, "TABLE_TYPE"));
        }
        this.iotType = JDBCUtils.safeGetString(dbResult, "IOT_TYPE");
        this.iotName = JDBCUtils.safeGetString(dbResult, "IOT_NAME");
        this.temporary = JDBCUtils.safeGetBoolean(dbResult, OracleConstants.COLUMN_TEMPORARY, OracleConstants.RESULT_YES_VALUE);
        this.secondary = JDBCUtils.safeGetBoolean(dbResult, "SECONDARY", OracleConstants.RESULT_YES_VALUE);
        this.nested = JDBCUtils.safeGetBoolean(dbResult, "NESTED", OracleConstants.RESULT_YES_VALUE);
        if (!CommonUtils.isEmpty(iotName)) {
            //this.setName(iotName);
        }
    }

    @Override
    public TableAdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded && monitor != null) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    ///////////////////////////////////
    // Statistics

    @Override
    public boolean hasStatistics() {
        return tableSize != null;
    }

    @Override
    public long getStatObjectSize() {
        return tableSize == null ? 0 : tableSize;
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }


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

    private void loadSize(DBRProgressMonitor monitor) throws DBCException {
        tableSize = null;
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            boolean hasDBA = getDataSource().isViewAvailable(monitor, OracleConstants.SCHEMA_SYS, "DBA_SEGMENTS");
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT SUM(bytes) TABLE_SIZE\n" +
                    "FROM " + OracleUtils.getSysSchemaPrefix(getDataSource()) + (hasDBA ? "DBA_SEGMENTS" : "USER_SEGMENTS") + " s\n" +
                    "WHERE S.SEGMENT_TYPE='TABLE' AND s.SEGMENT_NAME = ?" + (hasDBA ? " AND s.OWNER = ?" : "")))
            {
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
            log.debug("Error reading table statistics", e);
        } finally {
            if (tableSize == null) {
                tableSize = 0L;
            }
        }
    }

    void fetchTableSize(JDBCResultSet dbResult) throws SQLException {
        tableSize = dbResult.getLong("TABLE_SIZE");
    }

    @Override
    protected String getTableTypeName()
    {
        return "TABLE";
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Property(viewable = false, order = 5)
    public OracleDataType getTableType()
    {
        return tableType;
    }

    @Property(viewable = false, order = 6)
    public String getIotType()
    {
        return iotType;
    }

    @Property(viewable = false, order = 7)
    public String getIotName()
    {
        return iotName;
    }

    @Property(viewable = false, order = 10)
    public boolean isTemporary()
    {
        return temporary;
    }

    @Property(viewable = false, order = 11)
    public boolean isSecondary()
    {
        return secondary;
    }

    @Property(viewable = false, order = 12)
    public boolean isNested()
    {
        return nested;
    }

    @Override
    public OracleTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
/*
        // Fake XML attribute handle
        if (tableType != null && tableType.getName().equals(OracleConstants.TYPE_NAME_XML) && OracleConstants.XML_COLUMN_NAME.equals(attributeName)) {
            OracleTableColumn col = getXMLColumn(monitor);
            if (col != null) return col;
        }
*/

        return super.getAttribute(monitor, attributeName);
    }

    @Nullable
    private OracleTableColumn getXMLColumn(DBRProgressMonitor monitor) throws DBException {
        for (OracleTableColumn col : CommonUtils.safeCollection(getAttributes(monitor))) {
            if (col.getDataType() == tableType) {
                return col;
            }
        }
        return null;
    }


    @Override
    public Collection<OracleTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        List<OracleTableForeignKey> refs = new ArrayList<>();
        // This is dummy implementation
        // Get references from this schema only
        final Collection<OracleTableForeignKey> allForeignKeys =
            getContainer().foreignKeyCache.getObjects(monitor, getContainer(), null);
        for (OracleTableForeignKey constraint : allForeignKeys) {
            if (constraint.getReferencedTable() == this) {
                refs.add(constraint);
            }
        }
        return refs;
    }

    @Override
    @Association
    public Collection<OracleTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().foreignKeyCache.clearObjectCache(this);
        if (tableSize != null) {
            tableSize = null;
            getTableSize(monitor);
        }
        additionalInfo.loaded = false;
        return super.refreshObject(monitor);
    }

    private boolean hasRowIdPseudoAttribute() {
        return CommonUtils.isEmpty(this.iotType)
            && getDataSource().getContainer().getPreferenceStore().getBoolean(OracleConstants.PREF_SUPPORT_ROWID);
    }

    @Override
    public DBDPseudoAttribute[] getPseudoAttributes() throws DBException {
        if (this.hasRowIdPseudoAttribute()) {
            // IOT tables have index id instead of ROWID
            return new DBDPseudoAttribute[] {
                OracleConstants.PSEUDO_ATTR_ROWID
            };
        } else {
            return null;
        }
    }

    @Override
    public DBDPseudoAttribute[] getAllPseudoAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (this.allPseudoAttributes == null) {
            // https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Pseudocolumns.html
            List<DBDPseudoAttribute> attrs = new ArrayList<>(2);
            if (this.hasRowIdPseudoAttribute()) {
                attrs.add(OracleConstants.PSEUDO_ATTR_ROWID);
            }
            attrs.add(ROWSCN_PSEUDO_ATTRIBUTE);
            this.allPseudoAttributes = attrs.toArray(DBDPseudoAttribute.EMPTY_ARRAY);
        }
        return this.allPseudoAttributes;
    }

    @Override
    protected void appendSelectSource(DBRProgressMonitor monitor, StringBuilder query, String tableAlias, DBDPseudoAttribute rowIdAttribute) throws DBCException {
        if (tableType != null && tableType.getName().equals(OracleConstants.TYPE_NAME_XML)) {
            try {
                OracleTableColumn xmlColumn = getXMLColumn(monitor);
                if (xmlColumn != null) {
                    query.append("XMLType(").append(tableAlias).append(".").append(xmlColumn.getName()).append(".getClobval()) as ").append(xmlColumn.getName());
                    if (rowIdAttribute != null) {
                        query.append(",").append(rowIdAttribute.translateExpression(tableAlias));
                    }
                    return;
                }
            } catch (DBException e) {
                log.warn(e);
            }
        }
        super.appendSelectSource(monitor, query, tableAlias, rowIdAttribute);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return getDDL(monitor, OracleDDLFormat.getCurrentFormat(getDataSource()), options);
    }


    @Nullable
    @Override
    public DBPImage getObjectImage() {
        if (CommonUtils.isEmpty(iotType)) {
            return DBIcon.TREE_TABLE;
        } else {
            return DBIcon.TREE_TABLE_INDEX;
        }
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT a.*, h.STATS_UPDATE_TIME FROM " +
                    OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "TABLES") +
                    " a, ALL_TAB_STATS_HISTORY h WHERE h.OWNER(+) = a.OWNER AND h.TABLE_NAME(+) = a.TABLE_NAME" +
                    " AND a.OWNER=? AND a.TABLE_NAME=?")) {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        additionalInfo.pctFree = JDBCUtils.safeGetInt(dbResult, "PCT_FREE");
                        additionalInfo.pctUsed = JDBCUtils.safeGetInt(dbResult, "PCT_USED");
                        additionalInfo.iniTrans = JDBCUtils.safeGetInt(dbResult, "INI_TRANS");
                        additionalInfo.maxTrans = JDBCUtils.safeGetInt(dbResult, "MAX_TRANS");
                        additionalInfo.initialExtent = JDBCUtils.safeGetInt(dbResult, "INITIAL_EXTENT");
                        additionalInfo.nextExtent = JDBCUtils.safeGetInt(dbResult, "NEXT_EXTENT");
                        additionalInfo.minExtents = JDBCUtils.safeGetInt(dbResult, "MIN_EXTENTS");
                        additionalInfo.maxExtents = JDBCUtils.safeGetInt(dbResult, "MAX_EXTENTS");
                        additionalInfo.pctIncrease = JDBCUtils.safeGetInt(dbResult, "PCT_INCREASE");
                        additionalInfo.freelists = JDBCUtils.safeGetInt(dbResult, "FREELISTS");
                        additionalInfo.freelistGroups = JDBCUtils.safeGetInt(dbResult, "FREELIST_GROUPS");

                        additionalInfo.blocks = JDBCUtils.safeGetInt(dbResult, "BLOCKS");
                        additionalInfo.emptyBlocks = JDBCUtils.safeGetInt(dbResult, "EMPTY_BLOCKS");
                        additionalInfo.avgSpace = JDBCUtils.safeGetInt(dbResult, "AVG_SPACE");
                        additionalInfo.chainCount = JDBCUtils.safeGetInt(dbResult, "CHAIN_CNT");

                        additionalInfo.avgRowLen = JDBCUtils.safeGetInt(dbResult, "AVG_ROW_LEN");
                        additionalInfo.avgSpaceFreelistBlocks = JDBCUtils.safeGetInt(dbResult, "AVG_SPACE_FREELIST_BLOCKS");
                        additionalInfo.numFreelistBlocks = JDBCUtils.safeGetInt(dbResult, "NUM_FREELIST_BLOCKS");
                        additionalInfo.lastStatisticsUpdate = JDBCUtils.safeGetTimestamp(dbResult, "STATS_UPDATE_TIME");
                    } else {
                        log.warn("Cannot find table '" + getFullyQualifiedName(DBPEvaluationContext.UI) + "' metadata");
                    }
                    additionalInfo.loaded = true;
                }
            }
            catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }

    }

    @Override
    public void enableReferentialIntegrity(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        Collection<OracleTableForeignKey> foreignKeys = getAssociations(monitor);
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
                for (DBPNamedObject fk: foreignKeys) {
                    String sql = template.replace(FOREIGN_KEY_NAME_PLACEHOLDER,  fk.getName());
                    statement.executeUpdate(sql);
                }
            } catch (SQLException e) {
                throw new DBException("Unable to change referential integrity", e);
            }
        }
    }

    @Override
    public boolean supportsChangingReferentialIntegrity(@NotNull DBRProgressMonitor monitor) throws DBException {
        return !CommonUtils.isEmpty(getAssociations(monitor));
    }

    @Nullable
    @Override
    public String getChangeReferentialIntegrityStatement(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        if (!supportsChangingReferentialIntegrity(monitor)) {
            return null;
        }
        if (enable) {
            return ENABLE_REFERENTIAL_INTEGRITY_STATEMENT;
        }
        return DISABLE_REFERENTIAL_INTEGRITY_STATEMENT;
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return ArrayUtils.contains(supportedOptions, option);
    }
}
