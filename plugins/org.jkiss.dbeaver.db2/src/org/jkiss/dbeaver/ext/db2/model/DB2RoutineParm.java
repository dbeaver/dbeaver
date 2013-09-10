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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2RoutineRowType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterType;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Routine Parameter
 * 
 * @author Denis Forveille
 * 
 */
public class DB2RoutineParm implements DBSProcedureParameter {

   private final DB2Routine  procedure;
   private String            name;
   private String            remarks;
   private Integer           scale;
   private Integer           length;
   private DB2DataType       dataType;
   private String            typeName;
   private DB2RoutineRowType rowType;

   // -----------------------
   // Constructors
   // -----------------------

   public DB2RoutineParm(DBRProgressMonitor monitor, DB2Routine procedure, ResultSet dbResult) throws DBException {

      super();

      this.procedure = procedure;

      this.name = JDBCUtils.safeGetString(dbResult, "PARMNAME");
      this.scale = JDBCUtils.safeGetInteger(dbResult, "SCALE");
      this.length = JDBCUtils.safeGetInteger(dbResult, "LENGTH");
      this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");
      this.rowType = CommonUtils.valueOf(DB2RoutineRowType.class, JDBCUtils.safeGetString(dbResult, "ROWTYPE"));

      // Search for DataType
      typeName = JDBCUtils.safeGetString(dbResult, "TYPENAME");
      // Look first in Standards type
      this.dataType = procedure.getDataSource().getDataTypeCache().getObject(monitor, procedure.getDataSource(), typeName);
      if (this.dataType == null) {
         // Then Look in UDT
         // TODO DF yet to be done
      }

   }

   @Override
   public String getDescription() {
      return remarks;
   }

   @Override
   public DB2DataSource getDataSource() {
      return procedure.getDataSource();
   }

   @Override
   public DB2Routine getParentObject() {
      return procedure;
   }

   @Override
   public boolean isPersisted() {
      return true;
   }

   @Override
   @Property(viewable = true, order = 1)
   public String getName() {
      return name;
   }

   @Override
   @Property(viewable = true, order = 3)
   public long getMaxLength() {
      return length;
   }

   @Override
   @Property(viewable = true, order = 4)
   public int getScale() {
      return scale;
   }

   @Override
   public int getPrecision() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   @Property(viewable = true, order = 5)
   public DBSProcedureParameterType getParameterType() {
      return rowType.getParameterType();
   }

   // DF: Strange typeName and typeId are attributes of DBSDataKind...
   @Override
   public String getTypeName() {
      return typeName;
   }

   @Override
   public int getTypeID() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public DBSDataKind getDataKind() {
      return dataType.getDataKind();
   }

   // -----------------------
   // Properties
   // -----------------------

   @Property(viewable = true, order = 2)
   public DB2DataType getDataType() {
      return dataType;
   }

   public DB2RoutineRowType getRowType() {
      return rowType;
   }

}
