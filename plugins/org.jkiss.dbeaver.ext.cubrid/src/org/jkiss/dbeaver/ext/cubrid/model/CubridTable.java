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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.Collection;

public class CubridTable extends GenericTable
{
    private final PartitionCache partitionCache = new PartitionCache();
    public CubridTable(
            GenericStructContainer container,
            @Nullable String tableName,
            @Nullable String tableType,
            @Nullable JDBCResultSet dbResult)
    {
        super(container, tableName, tableType, dbResult);

        if (dbResult != null) {
            String type = JDBCUtils.safeGetString(dbResult, CubridConstants.IS_SYSTEM_CLASS);
            if (type != null) {
                this.setSystem(type.equals("YES"));
            }
        }
    }

    @Override
    public CubridDataSource getDataSource()
    {
        return (CubridDataSource) super.getDataSource();
    }

    public Collection<? extends GenericTableIndex> getIndexes(DBRProgressMonitor monitor)
            throws DBException
    {
        return this.getDataSource().getObjectContainer().getCubridIndexCache().getObjects(monitor, getContainer(), this);
    }
    
    public Collection<CubridPartition> getPartitions(DBRProgressMonitor monitor)
            throws DBException
    {
        return partitionCache.getAllObjects(monitor, this);
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, listProvider = OwnerListProvider.class, order = 2)
    public GenericSchema getSchema()
    {
        return super.getSchema();
    }

    public String getUniqueName()
    {
        if (getDataSource().getSupportMultiSchema()) {
            return this.getSchema().getName() + "." + this.getName();
        } else {
            return this.getName();
        }
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        if (this.isSystem()) {
            return DBUtils.getFullQualifiedName(getDataSource(), this);
        } else {
            return DBUtils.getFullQualifiedName(getDataSource(), this.getSchema(), this);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getDataSource().getObjectContainer().getCubridIndexCache().clearObjectCache(this);
        return super.refreshObject(monitor);
    }

    public static class OwnerListProvider implements IPropertyValueListProvider<CubridTable>
    {

        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(CubridTable object)
        {
            return object.getDataSource().getSchemas().toArray();
        }
    }
    
    static class PartitionCache extends JDBCObjectCache<CubridTable, CubridPartition> {


        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull CubridTable table) throws SQLException {
            String sql = "select * from db_partition where class_name = ? and owner_name = ?";
            final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
            dbStat.setString(1, table.getName());
            dbStat.setString(2, table.getSchema().getName());
            return dbStat;
        }
    
        @Override
        protected CubridPartition fetchObject(
            @NotNull JDBCSession session,
            @NotNull CubridTable table,
            @NotNull JDBCResultSet dbResult
        ) throws SQLException, DBException {
            String partition_class_name = JDBCUtils.safeGetString(dbResult, "partition_class_name");
            String type = JDBCUtils.safeGetString(dbResult, "partition_type");
            
            return new CubridPartition(table, partition_class_name, type, dbResult);
        }
            
    }

}
