/*
 * Copyright (C) 2013-2015 Denis Forveille titou10.titou10@gmail.com
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexPageSplit;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2UniqueRule;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

/**
 * DB2 Index
 * 
 * @author Denis Forveille
 */
public class DB2Index extends JDBCTableIndex<DB2Schema, DB2Table> {

    private static final Log LOG = Log.getLog(DB2Index.class);

    // Structure
    private DB2UniqueRule uniqueRule;
    private Integer colCount;
    private Integer uniqueColCount;
    private DB2IndexType db2IndexType;
    private Integer pctFree;
    private Integer indexId;
    private Integer minPctUsed;
    private Boolean reverseScans;
    private Integer tablespaceId;
    private DB2IndexPageSplit pageSplit;
    private Boolean compression;
    private String remarks;

    // Derived
    private Timestamp createTime;
    private Boolean madeUnique;

    // Stats
    private Timestamp statsTime;
    private Long fullKeycard;
    private Long firstKeycard;
    private Long first2Keycard;
    private Long first3Keycard;
    private Long first4Keycard;
    private Integer clusterRatio;

    // -----------------
    // Constructors
    // -----------------
    public DB2Index(DBRProgressMonitor monitor, DB2Schema schema, DB2Table table, ResultSet dbResult)
    {
        super(schema, table, JDBCUtils.safeGetStringTrimmed(dbResult, "INDNAME"), null, true);

        DB2DataSource db2DataSource = (DB2DataSource) schema.getDataSource();

        this.uniqueRule = CommonUtils.valueOf(DB2UniqueRule.class, JDBCUtils.safeGetString(dbResult, "UNIQUERULE"));
        this.colCount = JDBCUtils.safeGetInteger(dbResult, "COLCOUNT");
        this.uniqueColCount = JDBCUtils.safeGetInteger(dbResult, "UNIQUE_COLCOUNT");
        this.pctFree = JDBCUtils.safeGetInteger(dbResult, "PCTFREE");
        this.indexId = JDBCUtils.safeGetInteger(dbResult, "IID");
        this.minPctUsed = JDBCUtils.safeGetInteger(dbResult, "MINPCTUSED");
        this.reverseScans = JDBCUtils.safeGetBoolean(dbResult, "REVERSE_SCANS", DB2YesNo.Y.name());
        this.tablespaceId = JDBCUtils.safeGetInteger(dbResult, "TBSPACEID");
        this.pageSplit = CommonUtils.valueOf(DB2IndexPageSplit.class, JDBCUtils.safeGetStringTrimmed(dbResult, "PAGESPLIT"));
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.madeUnique = JDBCUtils.safeGetBoolean(dbResult, "MADE_UNIQUE");

        this.statsTime = JDBCUtils.safeGetTimestamp(dbResult, "STATS_TIME");
        this.fullKeycard = JDBCUtils.safeGetLong(dbResult, "FULLKEYCARD");
        this.firstKeycard = JDBCUtils.safeGetLong(dbResult, "FIRSTKEYCARD");
        this.first2Keycard = JDBCUtils.safeGetLong(dbResult, "FIRST2KEYCARD");
        this.first3Keycard = JDBCUtils.safeGetLong(dbResult, "FIRST3KEYCARD");
        this.first4Keycard = JDBCUtils.safeGetLong(dbResult, "FIRST4KEYCARD");
        this.clusterRatio = JDBCUtils.safeGetInteger(dbResult, "CLUSTERRATIO");

        if (db2DataSource.isAtLeastV9_5()) {
            this.compression = JDBCUtils.safeGetBoolean(dbResult, "COMPRESSION", DB2YesNo.Y.name());
        }

        // DF: Could have been done in constructor. More "readable" to do it here
        this.db2IndexType = CommonUtils.valueOf(DB2IndexType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "INDEXTYPE"));
        this.indexType = db2IndexType.getDBSIndexType();
    }

    public DB2Index(DB2Table db2Table, String indexName, DBSIndexType indexType)
    {
        super(db2Table.getSchema(), db2Table, indexName, indexType, false);
    }

    // -----------------
    // Business Contract
    // -----------------
    @Override
    public boolean isUnique()
    {
        return (uniqueRule.isUnique());
    }

    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    public String getFullQualifiedName()
    {
        return getContainer().getName() + "." + getName();
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public List<DB2IndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        try {
            return getContainer().getIndexCache().getChildren(monitor, getContainer(), this);
        } catch (DBException e) {
            // DF: Don't know what to do with this exception except log it
            LOG.error("DBException swallowed during getAttributeReferences", e);
            return null;
        }
    }

    public void addColumn(DB2IndexColumn ixColumn)
    {
        DBSObjectCache<DB2Index, DB2IndexColumn> cols = getContainer().getIndexCache().getChildrenCache(this);
        cols.cacheObject(ixColumn);
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

    @Property(viewable = true, editable = false, order = 2)
    public DB2Schema getIndSchema()
    {
        return getContainer();
    }

    @Property(viewable = true, editable = false, order = 5)
    public DB2UniqueRule getUniqueRule()
    {
        return uniqueRule;
    }

    @Property(viewable = false, editable = false, order = 10)
    public Boolean getMadeUnique()
    {
        return madeUnique;
    }

    @Property(viewable = false, editable = false, order = 11)
    public Integer getColCount()
    {
        return colCount;
    }

    @Property(viewable = false, editable = false, order = 12)
    public Integer getUniqueColCount()
    {
        return uniqueColCount;
    }

    @Property(viewable = false, editable = false, order = 70)
    public Integer getIndexId()
    {
        return indexId;
    }

    @Property(viewable = false, editable = false, order = 71)
    public Integer getTablespaceId()
    {
        return tablespaceId;
    }

    @Property(viewable = false, order = 20, editable = false)
    public Integer getPctFree()
    {
        return pctFree;
    }

    @Property(viewable = false, order = 21, editable = false)
    public Integer getMinPctUsed()
    {
        return minPctUsed;
    }

    @Property(viewable = false, order = 22, editable = false)
    public Boolean getReverseScans()
    {
        return reverseScans;
    }

    @Property(viewable = false, order = 23, editable = false)
    public DB2IndexPageSplit getPageSplit()
    {
        return pageSplit;
    }

    @Property(viewable = false, order = 24, editable = false)
    public Boolean getCompression()
    {
        return compression;
    }

    @Nullable
    @Override
    @Property(viewable = false, editable = false)
    public String getDescription()
    {
        return remarks;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, editable = false, order = 30, category = DB2Constants.CAT_STATS)
    public Timestamp getStatsTime()
    {
        return statsTime;
    }

    @Property(viewable = true, editable = false, order = 31, category = DB2Constants.CAT_STATS)
    public Long getFullKeycard()
    {
        return fullKeycard;
    }

    @Property(viewable = false, editable = false, order = 32, category = DB2Constants.CAT_STATS)
    public Long getFirstKeycard()
    {
        return firstKeycard;
    }

    @Property(viewable = false, editable = false, order = 33, category = DB2Constants.CAT_STATS)
    public Long getFirst2Keycard()
    {
        return first2Keycard;
    }

    @Property(viewable = false, editable = false, order = 34, category = DB2Constants.CAT_STATS)
    public Long getFirst3Keycard()
    {
        return first3Keycard;
    }

    @Property(viewable = false, editable = false, order = 35, category = DB2Constants.CAT_STATS)
    public Long getFirst4Keycard()
    {
        return first4Keycard;
    }

    @Property(viewable = false, editable = false, order = 36, category = DB2Constants.CAT_STATS)
    public Integer getClusterRatio()
    {
        return clusterRatio;
    }

}
