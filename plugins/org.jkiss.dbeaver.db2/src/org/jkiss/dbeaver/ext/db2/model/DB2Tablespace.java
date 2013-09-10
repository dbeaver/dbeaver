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
import java.util.Collection;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TablesaceContainerCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablespaceDataType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TablespaceType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DB2 Tablespace
 * 
 * @author Denis Forveille
 * 
 */
public class DB2Tablespace extends DB2GlobalObject implements DBPRefreshableObject {

   private final DB2TablesaceContainerCache containerCache = new DB2TablesaceContainerCache();

   private String                           name;
   private String                           owner;
   private DB2OwnerType                     ownerType;
   private Timestamp                        createTime;
   private Integer                          tbspaceId;
   private DB2TablespaceType                tbspaceType;
   private DB2TablespaceDataType            dataType;
   private Integer                          extentSize;
   private Integer                          prefetchSize;
   private Double                           overHead;
   private Double                           transferRate;
   private Double                           writeOverHead;
   private Double                           writeTransferRate;
   private Integer                          pageSize;
   private String                           dbpgName;
   private Boolean                          dropRecovery;
   private Integer                          dataTag;
   private String                           sgName;
   private Integer                          sgId;
   private Integer                          effectivePrefetchSize;
   private String                           remarks;

   private Integer                          bufferPoolId;

   // -----------------------
   // Constructors
   // -----------------------

   public DB2Tablespace(DB2DataSource dataSource, ResultSet dbResult) {
      super(dataSource, dbResult != null);
      this.name = JDBCUtils.safeGetString(dbResult, "TBSPACE");
      this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
      this.ownerType = DB2OwnerType.valueOf(JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
      this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
      this.tbspaceId = JDBCUtils.safeGetInteger(dbResult, "TBSPACEID");
      this.tbspaceType = DB2TablespaceType.valueOf(JDBCUtils.safeGetString(dbResult, "TBSPACETYPE"));
      this.dataType = DB2TablespaceDataType.valueOf(JDBCUtils.safeGetString(dbResult, "DATATYPE"));
      this.extentSize = JDBCUtils.safeGetInteger(dbResult, "EXTENTSIZE");
      this.prefetchSize = JDBCUtils.safeGetInteger(dbResult, "PREFETCHSIZE");
      this.overHead = JDBCUtils.safeGetDouble(dbResult, "OVERHEAD");
      this.transferRate = JDBCUtils.safeGetDouble(dbResult, "TRANSFERRATE");
      this.writeOverHead = JDBCUtils.safeGetDouble(dbResult, "WRITEOVERHEAD");
      this.writeTransferRate = JDBCUtils.safeGetDouble(dbResult, "WRITETRANSFERRATE");
      this.pageSize = JDBCUtils.safeGetInteger(dbResult, "PAGESIZE");
      this.dbpgName = JDBCUtils.safeGetString(dbResult, "DBPGNAME");
      this.bufferPoolId = JDBCUtils.safeGetInteger(dbResult, "BUFFERPOOLID");
      this.dropRecovery = JDBCUtils.safeGetBoolean(dbResult, "DROP_RECOVERY", DB2YesNo.Y.name());
      this.dataTag = JDBCUtils.safeGetInteger(dbResult, "DATATAG");
      this.sgName = JDBCUtils.safeGetString(dbResult, "SGNAME");
      this.sgId = JDBCUtils.safeGetInteger(dbResult, "SGID");
      this.effectivePrefetchSize = JDBCUtils.safeGetInteger(dbResult, "EFFECTIVEPREFETCHSIZE");
      this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");
   }

   @Override
   public boolean refreshObject(DBRProgressMonitor monitor) throws DBException {
      containerCache.clearCache();
      return true;
   }

   // -----------------
   // Properties
   // -----------------

   @Override
   @Property(viewable = true, editable = false, order = 1)
   public String getName() {
      return name;
   }

   @Property(viewable = true, editable = false, order = 2)
   public Integer getTbspaceId() {
      return tbspaceId;
   }

   @Property(viewable = true, editable = false, order = 3)
   public Integer getBufferPoolId() {
      return bufferPoolId;
   }

   @Property(viewable = true, editable = false, order = 4)
   public Integer getPageSize() {
      return pageSize;
   }

   @Property(viewable = true, editable = false, order = 5)
   public String getTbspaceTypeDescription() {
      return tbspaceType.getDescription();
   }

   @Property(viewable = true, editable = false, order = 6)
   public String getDataTypeDescription() {
      return dataType.getDescription();
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwner() {
      return owner;
   }

   public DB2OwnerType getOwnerType() {
      return ownerType;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwnerTypeDescription() {
      return ownerType.getDescription();
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getCreateTime() {
      return createTime;
   }

   public DB2TablespaceType getTbspaceType() {
      return tbspaceType;
   }

   public DB2TablespaceDataType getDataType() {
      return dataType;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_METRICS)
   public Integer getExtentSize() {
      return extentSize;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_METRICS)
   public Integer getPrefetchSize() {
      return prefetchSize;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_METRICS)
   public Double getOverHead() {
      return overHead;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_METRICS)
   public Double getTransferRate() {
      return transferRate;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_METRICS)
   public Double getWriteOverHead() {
      return writeOverHead;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_METRICS)
   public Double getWriteTransferRate() {
      return writeTransferRate;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_METRICS)
   public Integer getEffectivePrefetchSize() {
      return effectivePrefetchSize;
   }

   @Property(viewable = false, editable = false)
   public String getDbpgName() {
      return dbpgName;
   }

   @Property(viewable = false, editable = false)
   public Boolean getDropRecovery() {
      return dropRecovery;
   }

   @Property(viewable = false, editable = false)
   public Integer getDataTag() {
      return dataTag;
   }

   @Property(viewable = false, editable = false)
   public String getSgName() {
      return sgName;
   }

   @Property(viewable = false, editable = false)
   public Integer getSgId() {
      return sgId;
   }

   @Override
   @Property(viewable = false, editable = false)
   public String getDescription() {
      return remarks;
   }

   // -----------------
   // Associations
   // -----------------

   @Association
   public Collection<DB2TablespaceContainer> getContainers(DBRProgressMonitor monitor) throws DBException {
      return containerCache.getObjects(monitor, this);
   }

   public DB2TablespaceContainer getContainer(DBRProgressMonitor monitor, long containerId) throws DBException {
      for (DB2TablespaceContainer container : containerCache.getObjects(monitor, this)) {
         if (container.getContainerId() == containerId) {
            return container;
         }
      }
      return null;
   }

}
