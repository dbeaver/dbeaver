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

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2SequencePrecision;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2SequenceType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 sequence
 * 
 * @author Denis Forveille
 * 
 */
public class DB2Sequence extends DB2SchemaObject {

   private String               owner;
   private DB2OwnerType         ownerType;
   private Integer              seqId;
   private DB2SequenceType      seqType;
   private String               baseSchema;
   private String               baseSequence;
   private Long                 increment;
   private Long                 start;
   private Long                 maxValue;
   private Long                 minValue;
   private Long                 nextCacheFirstValue;
   private Boolean              cycle;
   private Integer              cache;
   private Boolean              order;
   private Integer              dataTypeId;
   private Integer              sourceTypeId;
   private Timestamp            createTime;
   private Timestamp            alterTime;
   private DB2SequencePrecision precision;
   private DB2OwnerType         origin;
   private String               remarks;

   // -----------------------
   // Constructors
   // -----------------------
   public DB2Sequence(DB2Schema schema, ResultSet dbResult) {
      super(schema, JDBCUtils.safeGetString(dbResult, "SEQNAME"), true);

      this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
      this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
      this.seqId = JDBCUtils.safeGetInteger(dbResult, "SEQID");
      this.seqType = CommonUtils.valueOf(DB2SequenceType.class, JDBCUtils.safeGetString(dbResult, "SEQTYPE"));
      this.baseSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "BASE_SEQSCHEMA");
      this.baseSequence = JDBCUtils.safeGetString(dbResult, "BASE_SEQNAME");
      this.increment = JDBCUtils.safeGetLong(dbResult, "INCREMENT");
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
   }

   // -----------------
   // Properties
   // -----------------

   @Override
   @Property(viewable = true, editable = false, order = 1)
   public String getName() {
      return super.getName();
   }

   @Property(viewable = true, editable = false, order = 2)
   public DB2Schema getSchema() {
      return super.getSchema();
   }

   @Property(viewable = true, editable = false, order = 3)
   public Long getNextCacheFirstValue() {
      return nextCacheFirstValue;
   }

   @Property(viewable = true, editable = false, order = 4)
   public Long getMinValue() {
      return minValue;
   }

   @Property(viewable = true, editable = false, order = 5)
   public Long getMaxValue() {
      return maxValue;
   }

   @Property(viewable = true, editable = false, order = 6)
   public Long getIncrement() {
      return increment;
   }

   @Property(viewable = true, editable = false, order = 7)
   public Long getStart() {
      return start;
   }

   @Property(viewable = true, editable = false, order = 8)
   public Integer getCache() {
      return cache;
   }

   @Property(viewable = true, editable = false, order = 9)
   public Boolean getCycle() {
      return cycle;
   }

   @Property(viewable = true, editable = false, order = 10)
   public Boolean getOrder() {
      return order;
   }

   public DB2SequencePrecision getPrecision() {
      return precision;
   }

   @Property(viewable = true, editable = false, order = 11)
   public String getPrecisionDescription() {
      return precision.getDescription();
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwner() {
      return owner;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwnerTypeDescription() {
      return ownerType.getDescription();
   }

   @Property(viewable = false, editable = false)
   public Integer getSeqId() {
      return seqId;
   }

   public DB2SequenceType getSeqType() {
      return seqType;
   }

   @Property(viewable = false, editable = false)
   public String getSeqTypeDescription() {
      return seqType.getDescription();
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_BASEBJECT)
   public String getBaseSchema() {
      return baseSchema;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_BASEBJECT)
   public String getBaseSequence() {
      return baseSequence;
   }

   @Property(viewable = false, editable = false)
   public Integer getDataTypeId() {
      return dataTypeId;
   }

   @Property(viewable = false, editable = false)
   public Integer getSourceTypeId() {
      return sourceTypeId;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getCreateTime() {
      return createTime;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getAlterTime() {
      return alterTime;
   }

   public DB2OwnerType getOrigin() {
      return origin;
   }

   @Property(viewable = false, editable = false, order = 23)
   public String getOriginDescription() {
      return origin.getDescription();
   }

   @Override
   @Property(viewable = false, editable = false)
   public String getDescription() {
      return remarks;
   }

}
