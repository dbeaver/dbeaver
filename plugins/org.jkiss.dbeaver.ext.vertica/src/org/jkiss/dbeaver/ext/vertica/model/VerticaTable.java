/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.vertica.VerticaUtils;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBPObjectWithLazyDescription;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.Date;

/**
 * VerticaTable
 */
public class VerticaTable extends GenericTable implements DBPObjectStatistics, DBPSystemObject, DBPObjectWithLazyDescription {

    private long tableSize = -1;

    private String partitionExpression;
    private Date createTime;
    private boolean isTempTable;
    private boolean isSystemTable;
    private boolean hasAggregateProjection;
    private String description;

    public VerticaTable(VerticaSchema container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
        if (dbResult != null) {
            this.partitionExpression = JDBCUtils.safeGetString(dbResult, "partition_expression");
            this.createTime = JDBCUtils.safeGetDate(dbResult, "create_time");
            this.isTempTable = JDBCUtils.safeGetBoolean(dbResult, "is_temp_table");
            this.isSystemTable = JDBCUtils.safeGetBoolean(dbResult, "is_system_table");
            this.hasAggregateProjection = JDBCUtils.safeGetBoolean(dbResult, "has_aggregate_projection");
        }
    }

    @Property(viewable = true, order = 5)
    public String getPartitionExpression() {
        return partitionExpression;
    }

    @Property(viewable = true, order = 6)
    public Date getCreateTime() {
        return createTime;
    }

    @Property(viewable = true, order = 7)
    public boolean isTempTable() {
        return isTempTable;
    }

    public boolean isSystem() {
        return isSystemTable;
    }

    @Property(viewable = true, order = 8)
    public boolean isHasAggregateProjection() {
        return hasAggregateProjection;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    @LazyProperty(cacheValidator = CommentsValidator.class)
    public String getDescription(DBRProgressMonitor monitor) throws DBException {
        if (description == null) {
            if (!((VerticaDataSource) getDataSource()).avoidCommentsReading()) {
                VerticaUtils.readTableAndColumnsDescriptions(monitor, getDataSource(), this, false);
            }
            if (description == null) {
                description = "";
            }
        }
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isPhysicalTable() {
        return !isView();
    }

    @Override
    public boolean hasStatistics() {
        return tableSize != -1;
    }

    @Override
    public long getStatObjectSize() {
        return tableSize;
    }

    void fetchStatistics(JDBCResultSet dbResult) throws SQLException {
        tableSize = dbResult.getLong("used_bytes");
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (tableSize != -1) {
            tableSize = -1;
            ((VerticaSchema) getSchema()).resetStatistics();
        }
        return super.refreshObject(monitor);
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }

    public static class CommentsValidator implements IPropertyCacheValidator<VerticaTable> {

        @Override
        public boolean isPropertyCached(VerticaTable object, Object propertyId)
        {
            return object.description != null;
        }
    }
}
