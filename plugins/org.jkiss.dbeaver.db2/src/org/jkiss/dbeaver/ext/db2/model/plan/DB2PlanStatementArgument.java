package org.jkiss.dbeaver.ext.db2.model.plan;

import java.sql.Clob;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * DB2 EXPLAIN_ARGUMENT table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanStatementArgument {

   private DB2PlanStatement db2PlanStatement;

   private Integer          operatorId;
   private String           argumentType;
   private String           argumentValue;
   private String           longArgumentValue;

   // ------------
   // Constructors
   // ------------

   public DB2PlanStatementArgument(JDBCResultSet dbResult, DB2PlanStatement db2PlanStatement) {
      this.db2PlanStatement = db2PlanStatement;

      this.operatorId = JDBCUtils.safeGetInteger(dbResult, "OPERATOR_ID");
      this.argumentType = JDBCUtils.safeGetString(dbResult, "ARGUMENT_TYPE");
      this.argumentValue = JDBCUtils.safeGetString(dbResult, "ARGUMENT_VALUE");
      // TODO DF: bad. this is a Clob!
      this.longArgumentValue = JDBCUtils.safeGetString(dbResult, "LONG_ARGUMENT_VALUE");
   }

   // ----------------
   // Standard Getters
   // ----------------
   public DB2PlanStatement getDb2PlanStatement() {
      return db2PlanStatement;
   }

   public Integer getOperatorId() {
      return operatorId;
   }

   public String getArgumentValue() {
      return argumentValue;
   }

   public String getArgumentType() {
      return argumentType;
   }

   public String getLongArgumentValue() {
      return longArgumentValue;
   }

}
