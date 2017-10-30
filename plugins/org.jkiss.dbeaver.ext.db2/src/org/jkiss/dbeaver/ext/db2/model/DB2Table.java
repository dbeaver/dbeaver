/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.editors.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.editors.DB2TableTablespaceListProvider;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableTriggerCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableAccessMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableCompressionMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableDropRule;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableLockSize;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablePartitionMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableStatus;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableTemporalType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Table
 * 
 * @author Denis Forveille
 */
public class DB2Table extends DB2TableBase
    implements DBPNamedObject2, DBPRefreshableObject, DB2SourceObject, DBDPseudoAttributeContainer {

    private static final String LINE_SEPARATOR = GeneralUtils.getDefaultLineSeparator();

    private static final String C_PT = "SELECT * FROM SYSCAT.DATAPARTITIONS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY SEQNO WITH UR";
    private static final String C_PE = "SELECT * FROM SYSCAT.PERIODS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY PERIODNAME WITH UR";

    private DB2TableTriggerCache tableTriggerCache = new DB2TableTriggerCache();

    // Dependent of DB2 Version. OK because the folder is hidden in plugin.xml
    private DBSObjectCache<DB2Table, DB2TablePartition> partitionCache;
    private DBSObjectCache<DB2Table, DB2TablePeriod> periodCache;

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
        this.mdcClustered = JDBCUtils.safeGetBoolean(dbResult, "CLUSTERED", DB2YesNo.Y.name());
        this.dropRule = CommonUtils.valueOf(DB2TableDropRule.class, JDBCUtils.safeGetString(dbResult, "DROPRULE"));

        this.card = JDBCUtils.safeGetLongNullable(dbResult, "CARD");
        this.nPages = JDBCUtils.safeGetLongNullable(dbResult, "NPAGES");
        this.fPages = JDBCUtils.safeGetLongNullable(dbResult, "FPAGES");
        this.overFLow = JDBCUtils.safeGetLongNullable(dbResult, "OVERFLOW");

        this.invalidateTime = JDBCUtils.safeGetTimestamp(dbResult, "INVALIDATE_TIME");
        this.lastRegenTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REGEN_TIME");
        if (getDataSource().isAtLeastV9_5()) {
            this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
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

        super.refreshObject(monitor);

        return getContainer().getTableCache().refreshObject(monitor, getContainer(), this);
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
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        return DB2Utils.generateDDLforTable(monitor, LINE_SEPARATOR, getDataSource(), this);
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2Trigger> getTriggers(DBRProgressMonitor monitor) throws DBException
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

    public DBSTableForeignKey getAssociation(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return getContainer().getAssociationCache().getObject(monitor, getContainer(), this, ukName);
    }

    @Override
    @Association
    public Collection<DB2TableReference> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().getReferenceCache().getObjects(monitor, getContainer(), this);
    }

    public DBSTableForeignKey getReference(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return getContainer().getReferenceCache().getObject(monitor, getContainer(), this, ukName);
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

    @Property(viewable = true, editable = false, order = 3, category = DB2Constants.CAT_STATS)
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

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Timestamp getStatsTime()
    {
        return statsTime;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Long getnPages()
    {
        return nPages;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Long getfPages()
    {
        return fPages;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Long getOverFLow()
    {
        return overFLow;
    }

    @Property(viewable = false, editable = false, order = 100)
    public Boolean getAppend()
    {
        return append;
    }

    @Property(viewable = false, editable = false, order = 101)
    public String getVolatileMode()
    {
        return volatileMode;
    }

    @Property(viewable = false, editable = false, order = 104)
    public DB2TableLockSize getLockSize()
    {
        return lockSize;
    }

    @Property(viewable = false, editable = false, order = 105)
    public DB2TableCompressionMode getCompression()
    {
        return compression;
    }

    @Property(viewable = false, editable = false, order = 106)
    public DB2TableAccessMode getAccessMode()
    {
        return accessMode;
    }

    @Property(viewable = false, editable = false, order = 107)
    public Boolean getMdcClustered()
    {
        return mdcClustered;
    }

    @Property(viewable = false, editable = false, order = 108)
    public DB2TableDropRule getDropRule()
    {
        return dropRule;
    }

    @Property(viewable = false, editable = false, order = 109)
    public String getDataCapture()
    {
        return dataCapture;
    }

    @Property(viewable = false, editable = false, order = 110)
    public DB2TablePartitionMode getPartitionMode()
    {
        return partitionMode;
    }

    @Property(viewable = false, editable = false, order = 111)
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
        if (getDataSource().isAtLeastV9_5()) {
            return new DBDPseudoAttribute[] { DB2Constants.PSEUDO_ATTR_RID_BIT };
        } else {
            return null;
        }
    }
}
