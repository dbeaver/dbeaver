package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * DB2 EXPLAIN_OBJECT table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanStatementObject {

   private DB2PlanStatement db2PlanStatement;

   private String           objectSchema;
   private String           objectName;
   private String           objectType;

   // TODO DF: and many many more

   // ------------
   // Constructors
   // ------------

   public DB2PlanStatementObject(JDBCResultSet dbResult, DB2PlanStatement db2PlanStatement) {
      this.db2PlanStatement = db2PlanStatement;

      this.objectSchema = JDBCUtils.safeGetString(dbResult, "OBJECT_SCHEMA");
      this.objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
      this.objectType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
   }

   // ----------------
   // Standard Getters
   // ----------------
   public DB2PlanStatement getDb2PlanStatement() {
      return db2PlanStatement;
   }

   public String getObjectSchema() {
      return objectSchema;
   }

   public String getObjectName() {
      return objectName;
   }

   public String getObjectType() {
      return objectType;
   }

}
