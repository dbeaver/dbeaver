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
import java.util.Collection;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2UniqueRule;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Index
 * 
 * @author Denis Forveille
 */
public class DB2Index extends JDBCTableIndex<DB2Schema, DB2Table> {

   // private String indSchema;
   // private String indName;
   private DB2UniqueRule        uniqueRule;
   private Boolean              madeUnique;
   private Integer              colCount;
   private Integer              uniqueColCount;
   private DB2IndexType         db2IndexType;
   private String               remarks;

   private List<DB2IndexColumn> columns;

   // -----------------
   // Constructors
   // -----------------
   public DB2Index(DBRProgressMonitor monitor, DB2Schema schema, DB2Table table, ResultSet dbResult) {
      super(schema, table, JDBCUtils.safeGetStringTrimmed(dbResult, "INDNAME"), null, true);

      // this.indSchema = JDBCUtils.safeGetString(dbResult, "INDSCHEMA");
      // this.indName = JDBCUtils.safeGetString(dbResult, "INDNAME");
      this.uniqueRule = CommonUtils.valueOf(DB2UniqueRule.class, JDBCUtils.safeGetString(dbResult, "UNIQUERULE"));
      this.madeUnique = JDBCUtils.safeGetBoolean(dbResult, "MADE_UNIQUE");
      this.colCount = JDBCUtils.safeGetInteger(dbResult, "COLCOUNT");
      this.uniqueColCount = JDBCUtils.safeGetInteger(dbResult, "UNIQUE_COLCOUNT");
      this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

      // DF: Could have been done in constructor. More "readable" to do it here
      this.db2IndexType = CommonUtils.valueOf(DB2IndexType.class, JDBCUtils.safeGetStringTrimmed(dbResult, "INDEXTYPE"));
      this.indexType = db2IndexType.getDBSIndexType();
   }

   @Override
   public boolean isUnique() {
      return (uniqueRule.isUnique());
   }

   @Override
   public String getDescription() {
      return remarks;
   }

   @Override
   public DB2DataSource getDataSource() {
      return getTable().getDataSource();
   }

   @Override
   public String getFullQualifiedName() {
      return getContainer().getName() + "." + getName();
   }

   // -----------------
   // Columns
   // -----------------

   @Override
   public Collection<DB2IndexColumn> getAttributeReferences(DBRProgressMonitor monitor) {
      try {
         return getContainer().getIndexCache().getChildren(monitor, getContainer(), this);
      } catch (DBException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         return null;
      }
   }

   // -----------------
   // Properties
   // -----------------

   @Override
   @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
   public String getName() {
      return super.getName();
   }

   @Property(viewable = true, editable = false, order = 2)
   public DB2Schema getIndSchema() {
      return getContainer();
   }

   public DB2UniqueRule getUniqueRule() {
      return uniqueRule;
   }

   @Property(viewable = true, editable = false)
   public String getUniqueRuleDescription() {
      return uniqueRule.getDescription();
   }

   @Property(viewable = false, editable = false)
   public Boolean getMadeUnique() {
      return madeUnique;
   }

   @Property(viewable = false, editable = false)
   public Integer getColCount() {
      return colCount;
   }

   @Property(viewable = true, editable = false)
   public Integer getUniqueColCount() {
      return uniqueColCount;
   }

}
