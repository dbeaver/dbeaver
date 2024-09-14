/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.editors.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.editors.DB2TableTablespaceListProvider;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableTriggerCache;
import org.jkiss.dbeaver.ext.db2.model.dict.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSPartitionContainer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * DB2 Table
 * 
 * @author Denis Forveille
 */
public class DB2Table extends DB2TableBase
    implements DBPRefreshableObject, DB2SourceObject, DBDPseudoAttributeContainer, DBSPartitionContainer, DBSEntityConstrainable {

    protected static final Log log = Log.getLog(DB2Table.class);

    private static final String LINE_SEPARATOR = GeneralUtils.getDefaultLineSeparator();

    private static final String C_PT = "SELECT * FROM SYSCAT.DATAPARTITIONS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY SEQNO WITH UR";
    private static final String C_PE = "SELECT * FROM SYSCAT.PERIODS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY PERIODNAME WITH UR";

    private static final DBDPseudoAttribute[] PRESENTED_PSEUDO_ATTRS = new DBDPseudoAttribute[] {
        new DBDPseudoAttribute(
            DBDPseudoAttributeType.OTHER,
            "DATASLICEID",
            null,
            null,
            DB2Messages.pseudo_column_datasliceid_description,
            true,
            DBDPseudoAttribute.PropagationPolicy.TABLE_NORMAL
        )
    };

    private DB2TableTriggerCache tableTriggerCache = new DB2TableTriggerCache();

    // Dependent of DB2 Version. OK because the folder is hidden in plugin.xml
    private DBSObjectCache<DB2Table, DB2TablePartition> partitionCache;
    private DBSObjectCache<DB2Table, DB2TablePeriod> periodCache;
    private volatile List<DB2TableForeignKey> referenceCache = null;
    private ColumnMaskCache columnMaskCache = new ColumnMaskCache();

    private DB2TableStatus status;
    private DB2TableType type;

    private Object tablespace;
    private Object indexTablespace;
    private Object longTablespace;

    private String dataCapture;
    private String constChecked;
    private DB2TablePartitionMode partitionMode;
    private Boolean append;
    private DB2TableLockSize lockSize;
    private String volatileMode;
    private DB2TableCompressionMode compression;
    private DB2TableAccessMode accessMode;
    private DB2TableOrganizationMode organizationMode;
    private Boolean mdcClustered;
    private DB2TableDropRule dropRule;
    private DB2TableTemporalType temporalType;

    private Timestamp alterTime;
    private Timestamp invalidateTime;
    private Timestamp lastRegenTime;

    private Timestamp statsTime;
    private Long card;
    private Long nPages;
    private Long fPages;
    private Long overFLow;

    // -----------------
    // Constructors
    // -----------------
    public DB2Table(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult) throws DBException
    {
        super(monitor, schema, dbResult);

        this.status = CommonUtils.valueOf(DB2TableStatus.class, JDBCUtils.safeGetString(dbResult, "STATUS"));
        this.type = CommonUtils.valueOf(DB2TableType.class, JDBCUtils.safeGetString(dbResult, "TYPE"));
        this.statsTime = JDBCUtils.safeGetTimestamp(dbResult, "STATS_TIME");

        this.dataCapture = JDBCUtils.safeGetString(dbResult, "DATACAPTURE");
        this.constChecked = JDBCUtils.safeGetString(dbResult, "CONST_CHECKED");
        this.partitionMode = CommonUtils.valueOf(DB2TablePartitionMode.class, JDBCUtils.safeGetString(dbResult, "PARTITION_MODE"));
        this.append = JDBCUtils.safeGetBoolean(dbResult, "APPEND_MODE", DB2YesNo.Y.name());
        this.volatileMode = JDBCUtils.safeGetString(dbResult, "VOLATILE");
        this.compression = CommonUtils.valueOf(DB2TableCompressionMode.class, JDBCUtils.safeGetString(dbResult, "COMPRESSION"));
        this.accessMode = CommonUtils.valueOf(DB2TableAccessMode.class, JDBCUtils.safeGetString(dbResult, "ACCESS_MODE"));
        this.organizationMode = CommonUtils.valueOf(DB2TableOrganizationMode.class, JDBCUtils.safeGetString(dbResult, "TABLEORG"));
        this.mdcClustered = JDBCUtils.safeGetBoolean(dbResult, "CLUSTERED", DB2YesNo.Y.name());
        this.dropRule = CommonUtils.valueOf(DB2TableDropRule.class, JDBCUtils.safeGetString(dbResult, "DROPRULE"));

        this.card = JDBCUtils.safeGetLongNullable(dbResult, "CARD");
        this.nPages = JDBCUtils.safeGetLongNullable(dbResult, "NPAGES");
        this.fPages = JDBCUtils.safeGetLongNullable(dbResult, "FPAGES");
        this.overFLow = JDBCUtils.safeGetLongNullable(dbResult, "OVERFLOW");

        this.invalidateTime = JDBCUtils.safeGetTimestamp(dbResult, "INVALIDATE_TIME");
        this.lastRegenTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REGEN_TIME");
        if (getDataSource().isAtLeastV9_5()) {
            this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, DB2Constants.SYSCOLUMN_ALTER_TIME);
        }
        if (getDataSource().isAtLeastV10_1()) {
            this.temporalType = CommonUtils.valueOf(DB2TableTemporalType.class, JDBCUtils.safeGetString(dbResult, "TEMPORALTYPE"));
        }

        String lockSizeString = JDBCUtils.safeGetString(dbResult, "LOCKSIZE");
        if (CommonUtils.isNotEmpty(lockSizeString)) {
            this.lockSize = CommonUtils.valueOf(DB2TableLockSize.class, lockSizeString);
        }

        this.tablespace = JDBCUtils.safeGetString(dbResult, "TBSPACE");
        this.indexTablespace = JDBCUtils.safeGetString(dbResult, "INDEX_TBSPACE");
        this.longTablespace = JDBCUtils.safeGetString(dbResult, "LONG_TBSPACE");

        this.partitionCache = new JDBCObjectSimpleCache<>(DB2TablePartition.class, C_PT, schema.getName(), getName());
        this.periodCache = new JDBCObjectSimpleCache<>(DB2TablePeriod.class, C_PE, schema.getName(), getName());

    }

    public DB2Table(DB2Schema schema, String name)
    {
        super(schema, name, false);

        this.type = DB2TableType.T;
        this.status = DB2TableStatus.N;
    }

    // -----------------
    // Business Contract
    // -----------------
    @Override
    public boolean isView()
    {
        return false;
    }

    @Override
    public JDBCStructCache<DB2Schema, DB2Table, DB2TableColumn> getCache()
    {
        return getContainer().getTableCache();
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().getConstraintCache().clearObjectCache(this);
        getContainer().getAssociationCache().clearObjectCache(this);
        getContainer().getReferenceCache().clearObjectCache(this);
        this.referenceCache = null;

        super.refreshObject(monitor);

        return getContainer().getTableCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public DB2TableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public List<DB2TableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().getTableCache().getChildren(monitor, getContainer(), this);
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return status.getState();
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        boolean includeViews = CommonUtils.getOption(options, OPTION_SCRIPT_INCLUDE_VIEWS);
        return DB2Utils.generateDDLforTable(monitor, LINE_SEPARATOR, getDataSource(), this, includeViews);
    }

    // -----------------
    // Associations
    // -----------------

    @Nullable
    @Association
    public List<DB2Trigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return tableTriggerCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<DB2TablePartition> getPartitions(DBRProgressMonitor monitor) throws DBException
    {
        // TODO DF: beurk: Consequences of "Integrated cache" that can not be created in class def= NPE with managers
        if (partitionCache == null) {
            return null;
        } else {
            return partitionCache.getAllObjects(monitor, this);
        }
    }

    @Association
    public Collection<DB2TablePeriod> getPeriods(DBRProgressMonitor monitor) throws DBException
    {
        // TODO DF: beurk: Consequences of "Integrated cache" that can not be created in class def= NPE with managers
        if (periodCache == null) {
            return null;
        } else {
            return periodCache.getAllObjects(monitor, this);
        }
    }

    @Nullable
    @Override
    @Association
    public Collection<DB2TableUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().getConstraintCache().getObjects(monitor, getContainer(), this);
    }

    public DB2TableUniqueKey getConstraint(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return getContainer().getConstraintCache().getObject(monitor, getContainer(), this, ukName);
    }

    @Override
    @Association
    public Collection<DB2TableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().getAssociationCache().getObjects(monitor, getContainer(), this);
    }

    public DB2TableForeignKey getAssociation(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return getContainer().getAssociationCache().getObject(monitor, getContainer(), this, ukName);
    }

    @Override
    @Association
    public List<DB2TableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (referenceCache != null) {
            return new ArrayList<>(referenceCache);
        }
        if (monitor == null) {
            return null;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Find table references")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT R.* FROM SYSCAT.REFERENCES R\n" +
                    "WHERE R.REFTABSCHEMA = ? AND R.REFTABNAME = ?\n" +
                    "ORDER BY R.REFKEYNAME\n" +
                    "WITH UR")) {
                dbStat.setString(1, this.getSchema().getName());
                dbStat.setString(2, this.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<DB2TableForeignKey> result = new ArrayList<>();
                    while (dbResult.nextRow()) {
                        String ownerSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
                        String ownerTableName = JDBCUtils.safeGetString(dbResult, "TABNAME");
                        String fkName = JDBCUtils.safeGetStringTrimmed(dbResult, "CONSTNAME");
                        DB2Table ownerTable = DB2Utils.findTableBySchemaNameAndName(
                            session.getProgressMonitor(), this.getDataSource(), ownerSchemaName, ownerTableName);
                        if (ownerTable == null) {
                            log.error("Cannot find reference owner table " + ownerSchemaName + "." + ownerTableName);
                            continue;
                        }
                        DB2TableForeignKey fk = ownerTable.getAssociation(monitor, fkName);
                        result.add(fk);
                    }
                    referenceCache = result;
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading table references", e);
        }
        return referenceCache;
    }

    @Association
    public Collection<DB2TableCheckConstraint> getCheckConstraints(DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().getCheckCache().getObjects(monitor, getContainer(), this);
    }

    public DB2TableCheckConstraint getCheckConstraint(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return getContainer().getCheckCache().getObject(monitor, getContainer(), this, ukName);
    }

    @Association
    public Collection<DB2ColumnMask> getColumnMasks(@NotNull DBRProgressMonitor monitor) throws DBException {
        return columnMaskCache.getAllObjects(monitor, this);
    }

    @Association
    public DB2ColumnMask getColumnMask(@NotNull DBRProgressMonitor monitor, @NotNull String maskName) throws DBException {
        return columnMaskCache.getObject(monitor, this, maskName);
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = false, order = 3, category = DBConstants.CAT_STATISTICS)
    public Long getCard()
    {
        return card;
    }

    @Property(viewable = true, editable = false, order = 4)
    public DB2TableStatus getStatus()
    {
        return status;
    }

    @Property(viewable = true, editable = false, order = 5)
    public DB2TableType getType()
    {
        return type;
    }

    @Property(viewable = true, editable = true, order = 10, category = DB2Constants.CAT_TABLESPACE, listProvider = DB2TableTablespaceListProvider.class)
    public DB2Tablespace getTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return DB2Tablespace.resolveTablespaceReference(monitor, getDataSource(), tablespace);
    }

    public void setTablespace(DB2Tablespace tablespace)
    {
        this.tablespace = tablespace;
    }

    @Property(viewable = false, editable = true, order = 11, category = DB2Constants.CAT_TABLESPACE, listProvider = DB2TableTablespaceListProvider.class)
    public DB2Tablespace getIndexTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return DB2Tablespace.resolveTablespaceReference(monitor, getDataSource(), indexTablespace);
    }

    public void setIndexTablespace(DB2Tablespace indexTablespace)
    {
        this.indexTablespace = indexTablespace;
    }

    @Property(viewable = false, editable = true, order = 12, category = DB2Constants.CAT_TABLESPACE, listProvider = DB2TableTablespaceListProvider.class)
    public DB2Tablespace getLongTablespace(DBRProgressMonitor monitor) throws DBException
    {
        return DB2Tablespace.resolveTablespaceReference(monitor, getDataSource(), longTablespace);
    }

    public void setLongTablespace(DB2Tablespace longTablespace)
    {
        this.longTablespace = longTablespace;
    }

    @Property(viewable = false, editable = false, category = DBConstants.CAT_STATISTICS)
    public Timestamp getStatsTime()
    {
        return statsTime;
    }

    @Property(viewable = false, editable = false, category = DBConstants.CAT_STATISTICS)
    public Long getnPages()
    {
        return nPages;
    }

    @Property(viewable = false, editable = false, category = DBConstants.CAT_STATISTICS)
    public Long getfPages()
    {
        return fPages;
    }

    @Property(viewable = false, editable = false, category = DBConstants.CAT_STATISTICS)
    public Long getOverFLow()
    {
        return overFLow;
    }

    @Property(viewable = false, editable = false, order = 100)
    public Boolean getAppend()
    {
        return append;
    }

    @Property(viewable = false, editable = false, specific = true, order = 101)
    public String getVolatileMode()
    {
        return volatileMode;
    }

    @Property(viewable = false, editable = false, specific = true, order = 104)
    public DB2TableLockSize getLockSize()
    {
        return lockSize;
    }

    @Property(viewable = false, editable = false, specific = true, order = 105)
    public DB2TableCompressionMode getCompression()
    {
        return compression;
    }

    @Property(viewable = false, editable = false, specific = true, order = 106)
    public DB2TableAccessMode getAccessMode()
    {
        return accessMode;
    }

    @Property(specific = true, order = 107)
    public DB2TableOrganizationMode getOrganizationMode() {
        return organizationMode;
    }

    @Property(viewable = false, editable = false, order = 108)
    public Boolean getMdcClustered()
    {
        return mdcClustered;
    }

    @Property(viewable = false, editable = false, order = 109)
    public DB2TableDropRule getDropRule()
    {
        return dropRule;
    }

    @Property(viewable = false, editable = false, specific = true, order = 110)
    public String getDataCapture()
    {
        return dataCapture;
    }

    @Property(viewable = false, editable = false, specific = true, order = 111)
    public DB2TablePartitionMode getPartitionMode()
    {
        return partitionMode;
    }

    @Property(viewable = false, editable = false, order = 112)
    public String getConstChecked()
    {
        return constChecked;
    }

    @Property(viewable = false, editable = false, order = 120, category = DB2Constants.CAT_TEMPORAL)
    public DB2TableTemporalType getTemporalType()
    {
        return temporalType;
    }

    @Property(viewable = false, editable = false, order = 101, category = DB2Constants.CAT_DATETIME)
    public Timestamp getAlterTime()
    {
        return alterTime;
    }

    @Property(viewable = false, editable = false, order = 102, category = DB2Constants.CAT_DATETIME)
    public Timestamp getInvalidateTime()
    {
        return invalidateTime;
    }

    @Property(viewable = false, editable = false, order = 103, category = DB2Constants.CAT_DATETIME)
    public Timestamp getLastRegenTime()
    {
        return lastRegenTime;
    }

    @Override
    public DBDPseudoAttribute[] getPseudoAttributes() throws DBException
    {
        // In BigSQL, calling RID_BIT results in a results in an error message indicating that
        // RID_BIT is not supported.
        //
        //   The command or statement was not executed because the following functionality is not
        //   supported in the current environment: "RID functions".. SQLCODE=-5115, 
        //   SQLSTATE=56038, DRIVER=4.31.10

        if (getDataSource().isAtLeastV9_5() && !getDataSource().isBigSQL()) {
            return new DBDPseudoAttribute[] { DB2Constants.PSEUDO_ATTR_RID_BIT };
        } else {
            return null;
        }
    }

    @Override
    public DBDPseudoAttribute[] getAllPseudoAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return PRESENTED_PSEUDO_ATTRS;
    }

    @NotNull
    @Override
    public List<DBSEntityConstraintInfo> getSupportedConstraints() {
        return List.of(
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.PRIMARY_KEY, DB2TableUniqueKey.class),
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.UNIQUE_KEY, DB2TableUniqueKey.class)
        );
    }

    public class ColumnMaskCache extends JDBCObjectLookupCache<DB2Table, DB2ColumnMask> {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(
            @NotNull JDBCSession session,
            @NotNull DB2Table db2Table,
            @Nullable DB2ColumnMask object,
            @Nullable String objectName
        ) throws SQLException {
            String sql = "SELECT * FROM SYSCAT.CONTROLS\n" +
                "WHERE CONTROLTYPE = 'C' AND TABSCHEMA = ? AND TABNAME = ?" +
                (object != null || objectName != null ? " AND CONTROLNAME = ?" : "");
            JDBCPreparedStatement statement = session.prepareStatement(sql);
            statement.setString(1, db2Table.getSchema().getName());
            statement.setString(2, db2Table.getName());
            if (object != null || objectName != null) {
                statement.setString(3, object != null ? object.getName() : objectName);
            }
            return statement;
        }

        @Nullable
        @Override
        protected DB2ColumnMask fetchObject(
            @NotNull JDBCSession session,
            @NotNull DB2Table db2Table,
            @NotNull JDBCResultSet resultSet
        ) throws SQLException, DBException {
            String maskName = JDBCUtils.safeGetString(resultSet, "CONTROLNAME");
            if (CommonUtils.isEmpty(maskName)) {
                log.debug("Skip column mask without name in table " + db2Table.getName());
                return null;
            }
            String colName = JDBCUtils.safeGetString(resultSet, "COLNAME");
            if (CommonUtils.isEmpty(colName)) {
                log.debug("Skip column mask without column name in table " + db2Table.getName());
                return null;
            }
            DB2TableColumn attribute = db2Table.getAttribute(session.getProgressMonitor(), colName);
            if (attribute == null) {
                log.debug("Can't find column " + colName + " in table " + db2Table.getName());
                return null;
            }
            return new DB2ColumnMask(db2Table, attribute, maskName, resultSet);
        }
    }
}
