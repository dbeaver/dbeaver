/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.nosql.cassandra.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * CassandraColumnFamily
 */
public class CassandraColumnFamily extends JDBCTable<CassandraDataSource, CassandraKeyspace> implements DBPRefreshableObject, DBPSystemObject
{
    private List<CassandraIndex> indexes;
    private JDBCTableConstraint<CassandraColumnFamily> primaryKey;
    private String description;
    private String keyAlias;
    private String columnFamilyType;
    private int columnFamilyId;
    private String caching;
    private String keyValidationClass;
    private String defaultValidationClass;
    private String compactionStrategy;
    private Object compactionStrategyOptions;
    private int maxCompactionThreshold;
    private int minCompactionThreshold;
    private String comparatorType;
    private String subcomparatorType;
    private Object compressionOptions;
    private int gcGraceSeconds;
    private double bloomFilterFpChance;
    private double dclocalReadRepairChance;
    private double readRepairChance;

    public CassandraColumnFamily(
        CassandraKeyspace container,
        ResultSet dbResult)
    {
        super(
            container,
            JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_NAME),
            true);
        columnFamilyType = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_TYPE);
        description = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

        columnFamilyId = JDBCUtils.safeGetInt(dbResult, "CF_ID");
        caching = JDBCUtils.safeGetStringTrimmed(dbResult, "CF_CACHING");
        keyAlias = JDBCUtils.safeGetStringTrimmed(dbResult, "CF_KEY_ALIAS");
        keyValidationClass = JDBCUtils.safeGetStringTrimmed(dbResult, "CF_KEY_VALIDATION_CLASS");
        defaultValidationClass = JDBCUtils.safeGetStringTrimmed(dbResult, "CF_DEFAULT_VALIDATION_CLASS");
        compactionStrategy = JDBCUtils.safeGetStringTrimmed(dbResult, "CF_COMPACTION_STRATEGY");
        compactionStrategyOptions = JDBCUtils.safeGetObject(dbResult, "CF_COMPACTION_STRATEGY_OPTIONS");
        maxCompactionThreshold = JDBCUtils.safeGetInt(dbResult, "CF_MAX_COMPACTION_THRESHOLD");
        minCompactionThreshold = JDBCUtils.safeGetInt(dbResult, "CF_MIN_COMPACTION_THRESHOLD");
        comparatorType = JDBCUtils.safeGetStringTrimmed(dbResult, "CF_COMPARATOR_TYPE");
        subcomparatorType = JDBCUtils.safeGetStringTrimmed(dbResult, "CF_SUBCOMPARATOR_TYPE");
        compressionOptions = JDBCUtils.safeGetObject(dbResult, "CF_COMPRESSION_OPTIONS");
        gcGraceSeconds = JDBCUtils.safeGetInt(dbResult, "CF_GC_GRACE_SECONDS");
        bloomFilterFpChance = JDBCUtils.safeGetDouble(dbResult, "CF_BLOOM_FILTER_FP_CHANCE");
        dclocalReadRepairChance = JDBCUtils.safeGetDouble(dbResult, "CF_DCLOCAL_READ_REPAIR_CHANCE");
        readRepairChance = JDBCUtils.safeGetDouble(dbResult, "CF_READ_REPAIR_CHANCE");
    }

    @Override
    public CassandraKeyspace.TableCache getCache()
    {
        return getContainer().getTableCache();
    }

    @Override
    public DBSObject getParentObject()
    {
        return getContainer();
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Override
    public boolean isSystem()
    {
        return false;
    }

    @Property(viewable = true, order = 4)
    public CassandraKeyspace getSchema()
    {
        return getContainer();
    }

    @Override
    public synchronized Collection<CassandraColumn> getAttributes(DBRProgressMonitor monitor)
        throws DBException
    {
        return this.getContainer().getTableCache().getChildren(monitor, getContainer(), this);
    }

    @Override
    public CassandraColumn getAttribute(DBRProgressMonitor monitor, String attributeName)
        throws DBException
    {
        return this.getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    @Override
    public synchronized Collection<CassandraIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        if (indexes == null) {
            indexes = new ArrayList<CassandraIndex>();
            for (CassandraColumn column : getAttributes(monitor)) {
                if (column.getKeyType() == CassandraColumn.KeyType.SECONDARY) {
                    indexes.add(new CassandraIndex(column));
                }
            }
        }
        // Read indexes using cache
        return indexes;
    }

    @Nullable
    @Override
    public synchronized Collection<? extends DBSTableConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        if (primaryKey == null) {
            primaryKey = new JDBCTableConstraint<CassandraColumnFamily>(CassandraColumnFamily.this, CassandraConstants.PRIMARY_KEY, null, DBSEntityConstraintType.PRIMARY_KEY, true) {
                @Override
                public String getFullQualifiedName()
                {
                    return CassandraConstants.PRIMARY_KEY;
                }
                @Nullable
                @Override
                public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor) throws DBException
                {
                    final CassandraColumn keyColumn = getKeyColumn(monitor);
                    if (keyColumn == null) {
                        return null;
                    }
                    return Collections.singletonList(new DBSEntityAttributeRef() {
                        @NotNull
                        @Override
                        public DBSEntityAttribute getAttribute()
                        {
                            return keyColumn;
                        }
                    });
                }
                @NotNull
                @Override
                public DBPDataSource getDataSource()
                {
                    return CassandraColumnFamily.this.getDataSource();
                }
            };
        }
        return Collections.singletonList(primaryKey);
    }

    private CassandraColumn getKeyColumn(DBRProgressMonitor monitor) throws DBException
    {
        for (CassandraColumn column : getAttributes(monitor)) {
            if (column.getName().equals(keyAlias)) {
                return column;
            }
        }
        return null;
    }

    @Override
    public Collection<? extends DBSTableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public Collection<? extends DBSTableForeignKey> getReferences(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    @Override
    public synchronized boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        this.getContainer().getTableCache().clearChildrenCache(this);
        indexes = null;
        return true;
    }

    public String getKeyAlias()
    {
        return keyAlias;
    }

    @Property(viewable = true, order = 30)
    public String getColumnFamilyType()
    {
        return columnFamilyType;
    }

    @Property(viewable = false, order = 31)
    public int getColumnFamilyId()
    {
        return columnFamilyId;
    }

    @Property(viewable = false, order = 32)
    public String getCaching()
    {
        return caching;
    }

    @Property(viewable = false, order = 33)
    public String getKeyValidationClass()
    {
        return keyValidationClass;
    }

    @Property(viewable = false, order = 34)
    public String getDefaultValidationClass()
    {
        return defaultValidationClass;
    }

    @Property(viewable = false, order = 35)
    public String getCompactionStrategy()
    {
        return compactionStrategy;
    }

    @Property(viewable = false, order = 36)
    public Object getCompactionStrategyOptions()
    {
        return compactionStrategyOptions;
    }

    @Property(viewable = false, order = 37)
    public int getMaxCompactionThreshold()
    {
        return maxCompactionThreshold;
    }

    @Property(viewable = false, order = 38)
    public int getMinCompactionThreshold()
    {
        return minCompactionThreshold;
    }

    @Property(viewable = false, order = 39)
    public String getComparatorType()
    {
        return comparatorType;
    }

    @Property(viewable = false, order = 40)
    public String getSubcomparatorType()
    {
        return subcomparatorType;
    }

    @Property(viewable = false, order = 41)
    public Object getCompressionOptions()
    {
        return compressionOptions;
    }

    @Property(viewable = false, order = 42)
    public int getGcGraceSeconds()
    {
        return gcGraceSeconds;
    }

    @Property(viewable = false, order = 43)
    public double getBloomFilterFpChance()
    {
        return bloomFilterFpChance;
    }

    @Property(viewable = false, order = 44)
    public double getDclocalReadRepairChance()
    {
        return dclocalReadRepairChance;
    }

    @Property(viewable = false, order = 45)
    public double getReadRepairChance()
    {
        return readRepairChance;
    }
}
