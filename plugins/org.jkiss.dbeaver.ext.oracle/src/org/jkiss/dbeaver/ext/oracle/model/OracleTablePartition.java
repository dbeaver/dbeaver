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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Oracle abstract partition
 */
public class OracleTablePartition extends OracleTablePhysical implements DBSTablePartition, DBPImageProvider {

    private static final Log log = Log.getLog(OracleTablePartition.class);

    public enum PartitionType {
        NONE,
        RANGE,
        HASH,
        SYSTEM,
        LIST,
    }

    private static final String CAT_PARTITIONING = "Partitioning";

    public static class PartitionInfoBase {
        private PartitionType partitionType;
        private PartitionType subpartitionType;
        private String partitionInterval;
        private long partitionCount;
        private Object partitionTablespace;

        @Property(category = CAT_PARTITIONING, order = 120)
        public PartitionType getPartitionType() {
            return partitionType;
        }

        @Property(category = CAT_PARTITIONING, order = 121)
        public PartitionType getSubpartitionType() {
            return subpartitionType;
        }

        @Property(category = CAT_PARTITIONING, order = 122)
        public String getPartitionInterval() {
            return partitionInterval;
        }

        @Property(category = CAT_PARTITIONING, order = 123)
        public long getPartitionCount() {
            return partitionCount;
        }

        @Property(category = CAT_PARTITIONING, order = 124, updatable = true)
        public Object getPartitionTablespace() {
            return partitionTablespace;
        }

        public PartitionInfoBase(DBRProgressMonitor monitor, OracleDataSource dataSource, ResultSet dbResult) {
            this.partitionType = CommonUtils.valueOf(PartitionType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "PARTITIONING_TYPE"));
            this.subpartitionType = CommonUtils.valueOf(PartitionType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "SUBPARTITIONING_TYPE"));
            String partitionTablespaceName = JDBCUtils.safeGetStringTrimmed(dbResult, "DEF_TABLESPACE_NAME");
            this.partitionInterval = JDBCUtils.safeGetString(dbResult, "INTERVAL");
            this.partitionCount = JDBCUtils.safeGetLong(dbResult, "PARTITION_COUNT");
            if (dataSource.isAdmin() && CommonUtils.isNotEmpty(partitionTablespaceName)) {
                try {
                    this.partitionTablespace = dataSource.tablespaceCache.getObject(monitor, dataSource, partitionTablespaceName);
                } catch (DBException e) {
                    log.debug("Can not find tablespace " + partitionTablespaceName, e);
                }
            }
        }

        public void setPartitionTablespace(Object partitionTablespace) {
            this.partitionTablespace = partitionTablespace;
        }
    }

    private OracleTablePhysical parent;
    private OracleTablePartition partitionParent;
    private int position;
    private String highValue;
    private boolean usable;
    private long sampleSize;
    private Timestamp lastAnalyzed;
    private List<OracleTablePartition> subPartitions;
    private boolean partitionByRange;
    private String valuesForCreating;

    OracleTablePartition(
        @NotNull OracleTablePhysical parent,
        @NotNull String name,
        @NotNull ResultSet dbResult,
        @Nullable OracleTablePartition partitionParent
    ) {
        super(parent.getSchema(), dbResult, name);
        this.parent = parent;
        this.partitionParent = partitionParent;
        this.highValue = JDBCUtils.safeGetString(dbResult, "HIGH_VALUE");
        this.position = partitionParent != null ?
            JDBCUtils.safeGetInt(dbResult, "SUBPARTITION_POSITION") :
            JDBCUtils.safeGetInt(dbResult, "PARTITION_POSITION");
        this.usable = "USABLE".equals(JDBCUtils.safeGetString(dbResult, OracleConstants.COLUMN_STATUS));
        this.sampleSize = JDBCUtils.safeGetLong(dbResult, "SAMPLE_SIZE");
        this.lastAnalyzed = JDBCUtils.safeGetTimestamp(dbResult, "LAST_ANALYZED");
    }

    public OracleTablePartition(
        @NotNull OracleSchema schema,
        @NotNull String name,
        @NotNull OracleTablePhysical parent,
        @Nullable OracleTablePartition partitionParent
    ) {
        super(schema, name);
        this.parent = parent;
        this.partitionParent = partitionParent;
    }

    @NotNull
    @Override
    public DBSTable getParentTable() {
        return parent;
    }

    @Property(viewable = true, order = 10)
    public int getPosition() {
        return position;
    }

    @Property(viewable = true, order = 11)
    public boolean isUsable() {
        return usable;
    }

    @Property(viewable = true, order = 30)
    public String getHighValue() {
        return highValue;
    }

    @Property(viewable = true, order = 41)
    public long getSampleSize() {
        return sampleSize;
    }

    @Property(viewable = true, order = 42)
    public Timestamp getLastAnalyzed() {
        return lastAnalyzed;
    }

    @Override
    @Property(viewable = true, order = 13)
    public boolean isPartitioned() {
        return !CommonUtils.isEmpty(subPartitions);
    }

    @Association
    public List<OracleTablePartition> getSubPartitions(DBRProgressMonitor monitor) throws DBException {
        if (partitionParent != null) {
            return Collections.emptyList();
        }
        if (subPartitions == null) {
            readSubPartitions(monitor);
        }
        return subPartitions;
    }

    private List<OracleTablePartition> readSubPartitions(@NotNull DBRProgressMonitor monitor) throws DBException {
        subPartitions = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read subpartitions")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM " +
                OracleUtils.getSysSchemaPrefix(getDataSource()) + "ALL_TAB_SUBPARTITIONS " +
                "\nWHERE TABLE_OWNER=? AND TABLE_NAME=? AND PARTITION_NAME=? " +
                "\nORDER BY SUBPARTITION_POSITION")) {
                dbStat.setString(1, parent.getSchema().getName());
                dbStat.setString(2, parent.getName());
                dbStat.setString(3, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String subpartitionName = JDBCUtils.safeGetString(dbResult, "SUBPARTITION_NAME");
                        if (CommonUtils.isEmpty(subpartitionName)) {
                            return null;
                        }
                        subPartitions.add(new OracleTablePartition(parent, subpartitionName, dbResult, this));
                    }
                }
            } catch (SQLException e) {
                throw new DBException(e, getDataSource());
            }
        }
        return subPartitions;
    }

    public void addSubPartition(@NotNull OracleTablePartition partition) {
        if (subPartitions == null) {
            subPartitions = new ArrayList<>();
        }
        subPartitions.add(partition);
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        return DBIcon.TREE_PARTITION;
    }

    @Override
    public TableAdditionalInfo getAdditionalInfo() {
        return new TableAdditionalInfo();
    }

    @Override
    protected String getTableTypeName() {
        return "TABLE PARTITION";
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Override
    protected boolean needAliasInSelect(
        @Nullable DBDDataFilter dataFilter,
        @Nullable DBDPseudoAttribute rowIdAttribute,
        @NotNull DBPDataSource dataSource
    ) {
        return false;
    }

    public OracleTablePhysical getParent() {
        return parent;
    }

    public OracleTablePartition getPartitionParent() {
        return partitionParent;
    }

    public boolean isSubPartition() {
        return partitionParent != null;
    }

    public boolean isPartitionByRange() {
        return partitionByRange;
    }

    public void setPartitionByRange(boolean partitionByRange) {
        this.partitionByRange = partitionByRange;
    }

    public String getValuesForCreating() {
        return valuesForCreating;
    }

    public void setValuesForCreating(String valuesForCreating) {
        this.valuesForCreating = valuesForCreating;
    }

    @NotNull
    @Override
    protected String getTableName() {
        return parent.getFullyQualifiedName(DBPEvaluationContext.DML);
    }

    @Override
    protected void appendExtraSelectParameters(@NotNull StringBuilder query) {
        query.append(" ")
            .append(partitionParent != null ? "SUB" : "")
            .append("PARTITION (")
            .append(DBUtils.getQuotedIdentifier(this))
            .append(")");
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        subPartitions = null;
        return super.refreshObject(monitor);
    }
}
