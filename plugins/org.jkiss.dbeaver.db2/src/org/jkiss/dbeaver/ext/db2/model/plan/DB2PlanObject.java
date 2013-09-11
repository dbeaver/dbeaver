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
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * DB2 EXPLAIN_OBJECT table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanObject extends DB2PlanNode {

   private DB2PlanStatement db2Statement;

   private String           objectSchema;
   private String           objectName;
   private String           objectType;
   private Integer          rowCount;

   // TODO DF: and many many more

   // ------------
   // Constructors
   // ------------

   public DB2PlanObject(JDBCResultSet dbResult, DB2PlanStatement db2Statement) {
      this.db2Statement = db2Statement;

      this.objectSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "OBJECT_SCHEMA");
      this.objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
      this.objectType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
      this.rowCount = JDBCUtils.safeGetInteger(dbResult, "ROW_COUNT");
   }

   // ----------------
   // Properties
   // ----------------

   @Override
   @Property(editable = false, viewable = true, order = 1)
   public String getNodeName() {
      return objectSchema + "." + objectName;
   }

   @Property(editable = false, viewable = true, order = 2)
   public Double getEstimatedCardinality() {
      return Double.valueOf(rowCount);
   }

   // ----------------
   // Standard Getters
   // ----------------

   public String getObjectSchema() {
      return objectSchema;
   }

   public String getObjectName() {
      return objectName;
   }

   public String getObjectType() {
      return objectType;
   }

   public DB2PlanStatement getDb2Statement() {
      return db2Statement;
   }

}
