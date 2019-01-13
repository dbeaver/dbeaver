/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2SequencePrecision;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2SequenceType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * DB2 sequence
 * 
 * @author Denis Forveille
 */
public class DB2Sequence extends DB2SchemaObject implements DBSSequence, DBPRefreshableObject {

    private String owner;
    private DB2OwnerType ownerType;
    private Integer seqId;
    private DB2SequenceType seqType;
    private String baseSchema;
    private String baseSequence;
    private Long incrementBy;
    private Long start;
    private Long maxValue;
    private Long minValue;
    private Long nextCacheFirstValue;
    private Boolean cycle;
    private Integer cache;
    private Boolean order;
    private Integer dataTypeId;
    private Integer sourceTypeId;
    private Timestamp createTime;
    private Timestamp alterTime;
    private DB2SequencePrecision precision;
    private DB2OwnerType origin;
    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2Sequence(DB2Schema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "SEQNAME"), true);

        DB2DataSource db2DataSource = schema.getDataSource();

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.seqId = JDBCUtils.safeGetInteger(dbResult, "SEQID");
        this.seqType = CommonUtils.valueOf(DB2SequenceType.class, JDBCUtils.safeGetString(dbResult, "SEQTYPE"));
        this.incrementBy = JDBCUtils.safeGetLong(dbResult, "INCREMENT");
        this.start = JDBCUtils.safeGetLong(dbResult, "START");
        this.maxValue = JDBCUtils.safeGetLong(dbResult, "MAXVALUE");
        this.minValue = JDBCUtils.safeGetLong(dbResult, "MINVALUE");
        this.nextCacheFirstValue = JDBCUtils.safeGetLong(dbResult, "NEXTCACHEFIRSTVALUE");
        this.cycle = JDBCUtils.safeGetBoolean(dbResult, "CYCLE", DB2YesNo.Y.name());
        this.cache = JDBCUtils.safeGetInteger(dbResult, "CACHE");
        this.order = JDBCUtils.safeGetBoolean(dbResult, "ORDER", DB2YesNo.Y.name());
        this.dataTypeId = JDBCUtils.safeGetInteger(dbResult, "DATATYPEID");
        this.sourceTypeId = JDBCUtils.safeGetInteger(dbResult, "SOURCETYPEID");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
        this.precision = DB2SequencePrecision.getFromDataType(JDBCUtils.safeGetInteger(dbResult, "PRECISION"));
        this.origin = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "ORIGIN"));
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (db2DataSource.isAtLeastV9_5()) {
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        }
        if (db2DataSource.isAtLeastV9_7()) {
            this.baseSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "BASE_SEQSCHEMA");
            this.baseSequence = JDBCUtils.safeGetString(dbResult, "BASE_SEQNAME");
        }
    }

    public DB2Sequence(DB2Schema schema, String name)
    {
        super(schema, name, false);
        seqType = DB2SequenceType.S;
        origin = DB2OwnerType.U;
        ownerType = DB2OwnerType.U;
        // DB2 Default
        precision = DB2SequencePrecision.P10;
        order = false;
        cycle = false;
        cache = 20;
        incrementBy = 1L;
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = false, order = 2)
    public DB2Schema getSchema()
    {
        return super.getSchema();
    }

    @Property(viewable = true, editable = false, order = 4)
    public DB2SequenceType getSeqType()
    {
        return seqType;
    }

    @Override
    public Number getLastValue() {
        return getNextCacheFirstValue();
    }

    @Property(viewable = true, editable = false, order = 5)
    public Long getNextCacheFirstValue()
    {
        return nextCacheFirstValue;
    }

    public void setNextCacheFirstValue(Long nextCacheFirstValue)
    {
        this.nextCacheFirstValue = nextCacheFirstValue;
    }

    @Property(viewable = false, editable = true, updatable = true, order = 6)
    public Long getMinValue()
    {
        return minValue;
    }

    public void setMinValue(Long minValue)
    {
        this.minValue = minValue;
    }

    @Property(viewable = false, editable = true, updatable = true, order = 6)
    public Long getMaxValue()
    {
        return maxValue;
    }

    public void setMaxValue(Long maxValue)
    {
        this.maxValue = maxValue;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 7)
    public Long getStart()
    {
        return start;
    }

    public void setStart(Long start)
    {
        this.start = start;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 8)
    public Long getIncrementBy()
    {
        return incrementBy;
    }

    public void setIncrementBy(Long incrementBy)
    {
        this.incrementBy = incrementBy;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 9)
    public Integer getCache()
    {
        return cache;
    }

    public void setCache(Integer cache)
    {
        this.cache = cache;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 10)
    public Boolean getCycle()
    {
        return cycle;
    }

    public void setCycle(Boolean cycle)
    {
        this.cycle = cycle;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 11)
    public Boolean getOrder()
    {
        return order;
    }

    public void setOrder(Boolean order)
    {
        this.order = order;
    }

    @Property(viewable = true, editable = true, order = 12)
    public DB2SequencePrecision getPrecision()
    {
        return precision;
    }

    public void setPrecision(DB2SequencePrecision precision)
    {
        this.precision = precision;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = false, editable = false)
    public Integer getSeqId()
    {
        return seqId;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_BASEBJECT)
    public String getBaseSchema()
    {
        return baseSchema;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_BASEBJECT)
    public String getBaseSequence()
    {
        return baseSequence;
    }

    @Property(viewable = false, editable = false)
    public Integer getDataTypeId()
    {
        return dataTypeId;
    }

    @Property(viewable = false, editable = false)
    public Integer getSourceTypeId()
    {
        return sourceTypeId;
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

    @Property(viewable = false, editable = false, order = 23)
    public DB2OwnerType getOrigin()
    {
        return origin;
    }

    @Nullable
    @Override
    @Property(viewable = false, editable = true, updatable = true, multiline = true)
    public String getDescription()
    {
        return remarks;
    }

    public void setDescription(String remarks)
    {
        this.remarks = remarks;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return this;
    }

}
