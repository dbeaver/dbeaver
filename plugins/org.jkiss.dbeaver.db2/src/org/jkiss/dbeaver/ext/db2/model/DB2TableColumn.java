/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.ext.db2.model;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ColumnHiddenState;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

/**
 * DB2 Table Column
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableColumn extends JDBCTableColumn<DB2TableBase> implements DBSTableColumn, DBPHiddenObject {

   private DB2DataType dataType;
   private String      remarks;
   private boolean     hidden;

   // -----------------
   // Constructors
   // -----------------

   public DB2TableColumn(DBRProgressMonitor monitor, DB2TableBase tableBase, ResultSet dbResult) throws DBException {
      super(tableBase, true);

      setName(JDBCUtils.safeGetString(dbResult, "COLNAME"));
      setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "COLNO"));
      setRequired(JDBCUtils.safeGetBoolean(dbResult, "NULLS", DB2YesNo.N.name()));
      setDefaultValue(JDBCUtils.safeGetString(dbResult, "DEFAULT"));
      setMaxLength(JDBCUtils.safeGetInteger(dbResult, "LENGTH"));
      setScale(JDBCUtils.safeGetInteger(dbResult, "SCALE"));

      this.hidden = DB2ColumnHiddenState.isHidden(JDBCUtils.safeGetString(dbResult, "HIDDEN"));
      this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

      // Set DataTypes data
      // Search for DataType
      String typeName = JDBCUtils.safeGetString(dbResult, "TYPENAME");
      // Look first in Standards type
      this.dataType = tableBase.getDataSource().getDataTypeCache().getObject(monitor, getTable().getDataSource(), typeName);
      if (this.dataType == null) {
         // Then Look in UDT
         // TODO DF yet to be done
      }
      setTypeName(dataType.getFullQualifiedName());
      setValueType(dataType.getTypeID());

   }

   @Override
   public DB2DataSource getDataSource() {
      return getTable().getDataSource();
   }

   @Override
   @Property(viewable = true, order = 41)
   public int getPrecision() {
      return super.getPrecision();
   }

   @Override
   public boolean isSequence() {
      return false;
   }

   @Override
   public boolean isHidden() {
      return hidden;
   }

   @Override
   public DBSDataKind getDataKind() {
      return dataType.getDataKind();
   }

   // -----------------
   // Direct Attributes
   // -----------------
   // @Property(viewable = true, editable = false,order=2)
   @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnDataTypeListProvider.class)
   public DBSDataType getType() {
      return dataType;
   }

   @Override
   public String getTypeName() {
      return super.getTypeName();
   }

   @Property(viewable = true, editable = false)
   @Override
   public boolean isRequired() {
      return super.isRequired();
   }

   @Property(viewable = true, editable = false)
   public String getComment() {
      return remarks;
   }

   public static class ColumnDataTypeListProvider implements IPropertyValueListProvider<DB2TableColumn> {

      @Override
      public boolean allowCustomValue() {
         return false;
      }

      @Override
      public Object[] getPossibleValues(DB2TableColumn column) {
         List<DBSDataType> dataTypes = new ArrayList<DBSDataType>(column.getTable().getDataSource().getDataTypes());
         if (!dataTypes.contains(column.getType())) {
            dataTypes.add(column.getType());
         }
         return dataTypes.toArray(new DBSDataType[dataTypes.size()]);
      }
   }

}
