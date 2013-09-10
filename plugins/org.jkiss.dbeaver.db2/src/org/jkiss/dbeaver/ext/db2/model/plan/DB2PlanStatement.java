package org.jkiss.dbeaver.ext.db2.model.plan;

import java.sql.SQLException;
import java.util.List;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * DB2 EXPLAIN_STATEMENT table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanStatement {

   private static String                   SEL_EXP_STMT_ARGS;      // See init below
   private static String                   SEL_EXP_STMT_OBJECT;    // See init below
   private static String                   SEL_EXP_STMT_OPERATOR;  // See init below
   private static String                   SEL_EXP_STMT_PREDICATE; // See init below
   private static String                   SEL_EXP_STMT_STREAM;    // See init below

   private List<DB2PlanStatementArgument>  listStatementArguments;
   private List<DB2PlanStatementObject>    listStatementObjects;
   private List<DB2PlanStatementOperator>  listStatementOperators;
   private List<DB2PlanStatementPredicate> listStatementPredicates;
   private List<DB2PlanStatementStream>    listStatementStreams;

   private DB2PlanInstance                 planInstance;

   private String                          explainLevel;
   private Integer                         stmtNo;
   private Integer                         sectNo;
   private Double                          totalCost;
   private String                          statementText;
   private byte[]                          snapshot;
   private Integer                         queryDegree;

   // ------------
   // Constructors
   // ------------
   public DB2PlanStatement(JDBCExecutionContext context, JDBCResultSet dbResult, DB2PlanInstance planInstance) throws SQLException {

      this.planInstance = planInstance;

      this.explainLevel = JDBCUtils.safeGetStringTrimmed(dbResult, "EXPLAIN_LEVEL");
      this.stmtNo = JDBCUtils.safeGetInteger(dbResult, "STMTNO");
      this.sectNo = JDBCUtils.safeGetInteger(dbResult, "SECTNO");
      this.totalCost = JDBCUtils.safeGetDouble(dbResult, "TOTAL_COST");
      this.queryDegree = JDBCUtils.safeGetInteger(dbResult, "QUERY_DEGREE");

      // TODO DF: bad: clob + blob
      this.statementText = JDBCUtils.safeGetString(dbResult, "STATEMENT_TEXT");
      // this.snapshot = JDBCUtils
      // .safeGetString(dbResult, "SNAPSHOT");

      loadChildren(context);
   }

   // -------------
   // Load children
   // -------------
   private void loadChildren(JDBCExecutionContext context) throws SQLException {

      JDBCPreparedStatement sqlStmt = null;
      JDBCResultSet res = executeQuery(context, sqlStmt, SEL_EXP_STMT_ARGS);
      try {
         while (res.next()) {
            listStatementArguments.add(new DB2PlanStatementArgument(res, this));
         }
      } finally {
         if (res != null) {
            res.close();
         }
         if (sqlStmt != null) {
            sqlStmt.close();
         }
      }

      res = executeQuery(context, sqlStmt, SEL_EXP_STMT_OBJECT);
      try {
         while (res.next()) {
            listStatementObjects.add(new DB2PlanStatementObject(res, this));
         }
      } finally {
         if (res != null) {
            res.close();
         }
         if (sqlStmt != null) {
            sqlStmt.close();
         }
      }

      res = executeQuery(context, sqlStmt, SEL_EXP_STMT_OPERATOR);
      try {
         while (res.next()) {
            listStatementOperators.add(new DB2PlanStatementOperator(res, this));
         }
      } finally {
         if (res != null) {
            res.close();
         }
         if (sqlStmt != null) {
            sqlStmt.close();
         }
      }

      res = executeQuery(context, sqlStmt, SEL_EXP_STMT_PREDICATE);
      try {
         while (res.next()) {
            listStatementPredicates.add(new DB2PlanStatementPredicate(res, this));
         }
      } finally {
         if (res != null) {
            res.close();
         }
         if (sqlStmt != null) {
            sqlStmt.close();
         }
      }

      res = executeQuery(context, sqlStmt, SEL_EXP_STMT_STREAM);
      try {
         while (res.next()) {
            listStatementStreams.add(new DB2PlanStatementStream(res, this));
         }
      } finally {
         if (res != null) {
            res.close();
         }
         if (sqlStmt != null) {
            sqlStmt.close();
         }
      }

   }

   private JDBCResultSet executeQuery(JDBCExecutionContext context, JDBCPreparedStatement sqlStmt, String sql) throws SQLException {

      sqlStmt = context.prepareStatement(sql);
      sqlStmt.setString(1, planInstance.getExplainRequester());
      sqlStmt.setTimestamp(2, planInstance.getExplainTime());
      sqlStmt.setString(3, planInstance.getSourceName());
      sqlStmt.setString(4, planInstance.getSourceSchema());
      sqlStmt.setString(5, planInstance.getSourceSchema());
      sqlStmt.setString(6, explainLevel);
      sqlStmt.setInt(7, stmtNo);
      sqlStmt.setInt(8, sectNo);

      return sqlStmt.executeQuery();
   }

   // -------
   // Queries
   // -------
   static {
      StringBuilder sb = new StringBuilder(1024);
      sb.append("SELECT *");
      sb.append(" FROM %s");
      sb.append(" WHERE EXPLAIN_REQUESTER = ?");
      sb.append("   AND EXPLAIN_TIME = ?");
      sb.append("   AND SOURCE_NAME = ?");
      sb.append("   AND SOURCE_SCHEMA = ?");
      sb.append("   AND SOURCE_VERSION = ?");
      sb.append("   AND EXPLAIN_LEVEL = ?");
      sb.append("   AND STMTNO = ?");
      sb.append("   AND SECTNO = ?");
      String sql = sb.toString();

      SEL_EXP_STMT_ARGS = String.format(sql, "EXPLAIN_ARGUMENT");
      SEL_EXP_STMT_OBJECT = String.format(sql, "EXPLAIN_OBJECT");
      SEL_EXP_STMT_OPERATOR = String.format(sql, "EXPLAIN_OPERATOR");
      SEL_EXP_STMT_PREDICATE = String.format(sql, "EXPLAIN_PREDICATE");
      SEL_EXP_STMT_STREAM = String.format(sql, "EXPLAIN_STREAM");
   }

   // ----------------
   // Standard Getters
   // ----------------

   public List<DB2PlanStatementArgument> getListStatementArguments() {
      return listStatementArguments;
   }

   public List<DB2PlanStatementObject> getListStatementObjects() {
      return listStatementObjects;
   }

   public List<DB2PlanStatementOperator> getListStatementOperators() {
      return listStatementOperators;
   }

   public List<DB2PlanStatementPredicate> getListStatementPredicates() {
      return listStatementPredicates;
   }

   public List<DB2PlanStatementStream> getListStatementStreams() {
      return listStatementStreams;
   }

   public DB2PlanInstance getPlanInstance() {
      return planInstance;
   }

   public String getExplainLevel() {
      return explainLevel;
   }

   public Integer getStmtNo() {
      return stmtNo;
   }

   public Integer getSectNo() {
      return sectNo;
   }

   public Double getTotalCost() {
      return totalCost;
   }

   public String getStatementText() {
      return statementText;
   }

   public byte[] getSnapshot() {
      return snapshot;
   }

   public Integer getQueryDegree() {
      return queryDegree;
   }

}
