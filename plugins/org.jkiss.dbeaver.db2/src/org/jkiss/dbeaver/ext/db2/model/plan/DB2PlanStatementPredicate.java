package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * DB2 EXPLAIN_PREDICATE table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanStatementPredicate {

   private DB2PlanStatement db2PlanStatement;

   private Integer          operatorId;
   private Integer          predicateId;
   private String           howApplied;
   private String           whenApplied;
   private String           predicateText;

   // TODO DF: and many many more

   // ------------
   // Constructors
   // ------------

   public DB2PlanStatementPredicate(JDBCResultSet dbResult, DB2PlanStatement db2PlanStatement) {
      this.db2PlanStatement = db2PlanStatement;

      this.operatorId = JDBCUtils.safeGetInteger(dbResult, "OPERATOR_ID");
      this.predicateId = JDBCUtils.safeGetInteger(dbResult, "PREDICATE_ID");
      this.howApplied = JDBCUtils.safeGetString(dbResult, "HOW_APPLIED");
      this.whenApplied = JDBCUtils.safeGetString(dbResult, "WHEN_APPLIED");

      // TODO DF: bad Clob..
      this.predicateText = JDBCUtils.safeGetString(dbResult, "PREDICATE_TEXT");
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

   public Integer getPredicateId() {
      return predicateId;
   }

   public String getHowApplied() {
      return howApplied;
   }

   public String getWhenApplied() {
      return whenApplied;
   }

   public String getPredicateText() {
      return predicateText;
   }

}
