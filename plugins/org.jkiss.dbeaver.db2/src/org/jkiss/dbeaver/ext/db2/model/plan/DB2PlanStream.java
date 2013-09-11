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

/**
 * DB2 EXPLAIN_STREAM table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanStream {

   private DB2PlanStatement db2Statement;

   private Integer          streamId;

   private String           sourceType;
   private Integer          sourceId;

   private String           targetType;
   private Integer          targetId;

   private String           objectSchema;
   private String           objectName;

   private Double           streamCount;

   // TODO DF: and many many more

   // ------------
   // Constructors
   // ------------

   public DB2PlanStream(JDBCResultSet dbResult, DB2PlanStatement db2Statement) {
      this.db2Statement = db2Statement;

      this.streamId = JDBCUtils.safeGetInteger(dbResult, "STREAM_ID");
      this.sourceType = JDBCUtils.safeGetString(dbResult, "SOURCE_TYPE");
      this.sourceId = JDBCUtils.safeGetInteger(dbResult, "SOURCE_ID");
      this.targetType = JDBCUtils.safeGetString(dbResult, "TARGET_TYPE");
      this.targetId = JDBCUtils.safeGetInteger(dbResult, "TARGET_ID");
      this.objectSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "OBJECT_SCHEMA");
      this.objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
      this.streamCount = JDBCUtils.safeGetDouble(dbResult, "STREAM_COUNT");
   }

   public String getSourceName() {
      if (sourceType.equals("O")) {
         // Operator
         return String.valueOf(sourceId);
      } else {
         // Data Object
         return objectSchema + "." + objectName;
      }
   }

   public String getTargetName() {
      if (targetType.equals("O")) {
         // Operator
         return String.valueOf(targetId);
      } else {
         // D: Data Object
         return objectSchema + "." + objectName;
      }
   }

   // ----------------
   // Standard Getters
   // ----------------

   public Integer getStreamId() {
      return streamId;
   }

   public String getSourceType() {
      return sourceType;
   }

   public Integer getSourceId() {
      return sourceId;
   }

   public String getTargetType() {
      return targetType;
   }

   public Integer getTargetId() {
      return targetId;
   }

   public String getObjectSchema() {
      return objectSchema;
   }

   public String getObjectName() {
      return objectName;
   }

   public DB2PlanStatement getDb2Statement() {
      return db2Statement;
   }

   public Double getStreamCount() {
      return streamCount;
   }

}
