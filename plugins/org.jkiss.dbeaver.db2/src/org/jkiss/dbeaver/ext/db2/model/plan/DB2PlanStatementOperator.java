package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * DB2 EXPLAIN_OPERATOR table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanStatementOperator {

   private DB2PlanStatement db2PlanStatement;

   private Integer          operatorId;
   private String           operatorType;
   private Double           totalCost;

   // TODO DF: and many many more

   // ------------
   // Constructors
   // ------------

   public DB2PlanStatementOperator(JDBCResultSet dbResult, DB2PlanStatement db2PlanStatement) {
      this.db2PlanStatement = db2PlanStatement;

      this.operatorId = JDBCUtils.safeGetInteger(dbResult, "OPERATOR_ID");
      this.operatorType = JDBCUtils.safeGetString(dbResult, "OPERATOR_TYPE");
      this.totalCost = JDBCUtils.safeGetDouble(dbResult, "TOTAL_COST");
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

   public String getOperatorType() {
      return operatorType;
   }

   public Double getTotalCost() {
      return totalCost;
   }

}
