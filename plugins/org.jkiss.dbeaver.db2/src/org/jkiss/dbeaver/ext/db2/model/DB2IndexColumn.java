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

import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexColOrder;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * DB2 Index Column
 * 
 * @author Denis Forveille
 * 
 * */
public class DB2IndexColumn extends AbstractTableIndexColumn {
   private DB2Index         index;
   private DB2TableColumn   tableColumn;

   private Integer          colSeq;
   private DB2IndexColOrder colOrder;
   private String           collationSchema;
   private String           collationNane;

   // -----------------
   // Constructors
   // -----------------

   public DB2IndexColumn(DB2Index index,
                         DB2TableColumn tableColumn,
                         Integer colSeq,
                         DB2IndexColOrder colOrder,
                         String collationSchema,
                         String collationNane) {
      this.index = index;
      this.tableColumn = tableColumn;

      this.colSeq = colSeq;
      this.colOrder = colOrder;
      this.collationSchema = collationSchema;
      this.collationNane = collationNane;
   }

   @Override
   public DB2DataSource getDataSource() {
      return index.getDataSource();
   }

   @Override
   public DB2Index getParentObject() {
      return index;
   }

   @Override
   public DB2Index getIndex() {
      return index;
   }

   @Override
   public String getDescription() {
      return tableColumn.getDescription();
   }

   @Override
   public int getOrdinalPosition() {
      return colSeq;
   }

   @Override
   public boolean isAscending() {
      return colOrder.isAscending();
   }

   @Override
   public String getName() {
      return tableColumn.getName();
   }

   // -----------------
   // Properties
   // -----------------

   @Override
   @Property(id = "name", viewable = true)
   public DB2TableColumn getTableColumn() {
      return tableColumn;
   }

   @Property(viewable = true, editable = false, order = 2, id = "Ordering")
   public String getColOrderDescription() {
      return colOrder.getDescription();
   }

   @Property(viewable = true, editable = false, order = 3, id = "Sequence")
   public Integer getColSeq() {
      return colSeq;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
   public String getCollationSchema() {
      return collationSchema;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
   public String getCollationNane() {
      return collationNane;
   }
}
