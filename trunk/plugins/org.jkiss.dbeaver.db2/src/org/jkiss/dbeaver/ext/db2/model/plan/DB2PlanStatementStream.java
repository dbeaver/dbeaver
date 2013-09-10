package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * DB2 EXPLAIN_STREAM table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanStatementStream {

   private DB2PlanStatement db2PlanStatement;

   private Integer          streamId;
   private String           sourceType;
   private Integer          sourceId;
   private String           targetType;
   private Integer          targetId;
   private String           objectSchema;
   private String           objectname;

   // TODO DF: and many many more

   // ------------
   // Constructors
   // ------------

   public DB2PlanStatementStream(JDBCResultSet dbResult, DB2PlanStatement db2PlanStatement) {
      this.db2PlanStatement = db2PlanStatement;

      this.streamId = JDBCUtils.safeGetInteger(dbResult, "STREAM_ID");
      this.sourceType = JDBCUtils.safeGetString(dbResult, "SOURCE_TYPE");
      this.sourceId = JDBCUtils.safeGetInteger(dbResult, "SOURCE_ID");
      this.targetType = JDBCUtils.safeGetString(dbResult, "TARGET_TYPE");
      this.targetId = JDBCUtils.safeGetInteger(dbResult, "TARGET_ID");
      this.objectSchema = JDBCUtils.safeGetString(dbResult, "OBJECT_SCHEMA");
      this.objectname = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
   }

   // ----------------
   // Standard Getters
   // ----------------
   public DB2PlanStatement getDb2PlanStatement() {
      return db2PlanStatement;
   }

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

   public String getObjectname() {
      return objectname;
   }

}
