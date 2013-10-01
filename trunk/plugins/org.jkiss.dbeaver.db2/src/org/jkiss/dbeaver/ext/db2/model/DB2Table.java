/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.edit.DB2TableTablespaceListProvider;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableIndexCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableTriggerCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableAccessMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableCompressionMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableDropRule;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableLockSize;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablePartitionMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableRefreshMode;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableStatus;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.ext.db2.model.source.DB2StatefulObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2 Table
 * 
 * @author Denis Forveille
 */
public class DB2Table extends DB2TableBase implements DBPNamedObject2, DBPRefreshableObject, DB2StatefulObject {

    private static final Log log = LogFactory.getLog(DB2Table.class);

    private static final String C_PT =
        "SELECT * FROM SYSCAT.DATAPARTITIONS  WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY SEQNO WITH UR";

    private DB2TableIndexCache tableIndexCache = new DB2TableIndexCache();
    private DB2TableTriggerCache tableTriggerCache = new DB2TableTriggerCache();
    private DBSObjectCache<DB2Table, DB2TablePartition> partitionCache;

    private DB2TableStatus status;
    private DB2TableType type;
    private Timestamp createTime;
    private Timestamp alterTime;
    private Timestamp invalidateTime;

    private DB2Tablespace tablespace;
    private DB2Tablespace indexTablespace;
    private DB2Tablespace longTablespace;

    private String dataCapture;
    private String constChecked;
    private DB2TablePartitionMode partitionMode;
    private Boolean append;
    private DB2TableRefreshMode refreshMode;
    private Timestamp refreshTime;
    private DB2TableLockSize lockSize;
    private String volatileMode;
    private DB2TableCompressionMode compression;
    private DB2TableAccessMode accessMode;
    private Boolean mdcClustered;
    private DB2TableDropRule dropRule;

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

        setName(JDBCUtils.safeGetString(dbResult, "TABNAME"));

        this.status = CommonUtils.valueOf(DB2TableStatus.class, JDBCUtils.safeGetString(dbResult, "STATUS"));
        this.type = CommonUtils.valueOf(DB2TableType.class, JDBCUtils.safeGetString(dbResult, "TYPE"));
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
        this.invalidateTime = JDBCUtils.safeGetTimestamp(dbResult, "INVALIDATE_TIME");
        this.statsTime = JDBCUtils.safeGetTimestamp(dbResult, "STATS_TIME");

        this.dataCapture = JDBCUtils.safeGetString(dbResult, "DATACAPTURE");
        this.constChecked = JDBCUtils.safeGetString(dbResult, "CONST_CHECKED");
        this.partitionMode = CommonUtils.valueOf(DB2TablePartitionMode.class, JDBCUtils.safeGetString(dbResult, "PARTITION_MODE"));
        this.append = JDBCUtils.safeGetBoolean(dbResult, "APPEND_MODE", DB2YesNo.Y.name());
        this.refreshTime = JDBCUtils.safeGetTimestamp(dbResult, "REFRESH_TIME");
        this.volatileMode = JDBCUtils.safeGetString(dbResult, "VOLATILE");
        this.compression = CommonUtils.valueOf(DB2TableCompressionMode.class, JDBCUtils.safeGetString(dbResult, "COMPRESSION"));
        this.accessMode = CommonUtils.valueOf(DB2TableAccessMode.class, JDBCUtils.safeGetString(dbResult, "ACCESS_MODE"));
        this.mdcClustered = JDBCUtils.safeGetBoolean(dbResult, "CLUSTERED", DB2YesNo.Y.name());
        this.dropRule = CommonUtils.valueOf(DB2TableDropRule.class, JDBCUtils.safeGetString(dbResult, "DROPRULE"));

        this.card = JDBCUtils.safeGetLongNullable(dbResult, "CARD");
        this.nPages = JDBCUtils.safeGetLongNullable(dbResult, "NPAGES");
        this.fPages = JDBCUtils.safeGetLongNullable(dbResult, "FPAGES");
        this.overFLow = JDBCUtils.safeGetLongNullable(dbResult, "OVERFLOW");

        String refreshModeString = JDBCUtils.safeGetString(dbResult, "REFRESH");
        if (CommonUtils.isNotEmpty(refreshModeString)) {
            this.refreshMode = CommonUtils.valueOf(DB2TableRefreshMode.class, refreshModeString);
        }
        String lockSizeString = JDBCUtils.safeGetString(dbResult, "LOCKSIZE");
        if (CommonUtils.isNotEmpty(lockSizeString)) {
            this.lockSize = CommonUtils.valueOf(DB2TableLockSize.class, lockSizeString);
        }

        String tablespaceName = JDBCUtils.safeGetString(dbResult, "TBSPACE");
        this.tablespace = getDataSource().getTablespace(monitor, tablespaceName);
        String indexTablespaceName = JDBCUtils.safeGetString(dbResult, "INDEX_TBSPACE");
        if (indexTablespaceName != null) {
            this.indexTablespace = getDataSource().getTablespace(monitor, indexTablespaceName);
        }
        String longTablespaceName = JDBCUtils.safeGetString(dbResult, "LONG_TBSPACE");
        if (longTablespaceName != null) {
            this.longTablespace = getDataSource().getTablespace(monitor, longTablespaceName);
        }

        this.partitionCache =
            new JDBCObjectSimpleCache<DB2Table, DB2TablePartition>(DB2TablePartition.class, C_PT, schema.getName(), getName());

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
    public DBSObjectState getObjectState()
    {
        return status.getState();
    }

    @Override
    public JDBCStructCache<DB2Schema, DB2Table, DB2TableColumn> getCache()
    {
        return getContainer().getTableCache();
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        getContainer().getTableCache().clearChildrenCache(this);

        // DF: Refresh parent cache ie cache for schemaof indexes/triggers different from table schema?
        tableIndexCache.clearCache();
        tableTriggerCache.clearCache();
        partitionCache.clearCache();

        getContainer().getConstraintCache().clearObjectCache(this);
        getContainer().getAssociationCache().clearObjectCache(this);
        getContainer().getReferenceCache().clearObjectCache(this);

        return true;
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException
    {
        // TODO DF: What to do here?
    }

    public String getDDL(DBRProgressMonitor monitor) throws DBException
    {
        // TODO DF: How to get line separator ?
        return DB2Utils.generateDDLforTable(monitor, ";", getDataSource(), this);
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public Collection<DB2TableColumn> getAttributes(DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().getTableCache().getChildren(monitor, getContainer(), this);
    }

    @Override
    public DB2TableColumn getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException
    {
        return getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    // -----------------
    // Associations
    // -----------------

    @Override
    @Association
    public Collection<DB2Index> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return tableIndexCache.getObjects(monitor, this);
    }

    @Association
    public Collection<DB2Trigger> getTriggers(DBRProgressMonitor monitor) throws DBException
    {
        return tableTriggerCache.getObjects(monitor, this);
    }

    @Association
    public Collection<DB2TablePartition> getPartitions(DBRProgressMonitor monitor) throws DBException
    {
        // TODO DF: beurk: Consequences of "Integrated cache" that can not be created in class def= NPE with managers
        if (partitionCache == null) {
            return null;
        } else {
            return partitionCache.getObjects(monitor, this);
        }
    }

    @Override
    @Association
    public Collection<DB2TableUniqueKey> getConstraints(DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().getConstraintCache().getObjects(monitor, getContainer(), this);
    }

    public DB2TableUniqueKey getConstraint(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return getContainer().getConstraintCache().getObject(monitor, getContainer(), this, ukName);
    }

    @Override
    @Association
    public Collection<DB2TableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException
    {
        return getContainer().getAssociationCache().getObjects(monitor, getContainer(), this);
    }

    public DBSTableForeignKey getAssociation(DBRProgressMonitor monitor, String ukName) throws DBException
    {
        return getContainer().getAssociationCache().getObject(monitor, getContainer(), this, ukName);
    }

    @Override
    @Association
    public Collection<DB2TableReference> getReferences(DBRProgressMonitor monitor) throws DBException
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
    public DB2Tablespace getTablespace()
    {
        return tablespace;
    }

    public void setTablespace(DB2Tablespace tablespace)
    {
        this.tablespace = tablespace;
    }

    @Property(viewable = false, editable = true, order = 11, category = DB2Constants.CAT_TABLESPACE, listProvider = DB2TableTablespaceListProvider.class)
    public DB2Tablespace getIndexTablespace()
    {
        return indexTablespace;
    }

    public void setIndexTablespace(DB2Tablespace indexTablespace)
    {
        this.indexTablespace = indexTablespace;
    }

    @Property(viewable = false, editable = true, order = 12, category = DB2Constants.CAT_TABLESPACE, listProvider = DB2TableTablespaceListProvider.class)
    public DB2Tablespace getLongTablespace()
    {
        return longTablespace;
    }

    public void setLongTablespace(DB2Tablespace longTablespace)
    {
        this.longTablespace = longTablespace;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getAlterTime()
    {
        return alterTime;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getInvalidateTime()
    {
        return invalidateTime;
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

    @Property(viewable = false, editable = false, order = 102)
    public DB2TableRefreshMode getRefreshMode()
    {
        return refreshMode;
    }

    @Property(viewable = false, editable = false, order = 103)
    public Timestamp getRefreshTime()
    {
        return refreshTime;
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

}
