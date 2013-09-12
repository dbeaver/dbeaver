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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * DB2 EXPLAIN_OPERATOR table
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanOperator extends DB2PlanNode {

   private static String                  SEL_BASE_SELECT;

   static {
      StringBuilder sb = new StringBuilder(1024);
      sb.append("SELECT *");
      sb.append(" FROM %s.%s");
      sb.append(" WHERE EXPLAIN_REQUESTER = ?"); // 1
      sb.append("   AND EXPLAIN_TIME = ?"); // 2
      sb.append("   AND SOURCE_NAME = ?");// 3
      sb.append("   AND SOURCE_SCHEMA = ?");// 4
      sb.append("   AND SOURCE_VERSION = ?");// 5
      sb.append("   AND EXPLAIN_LEVEL = ?");// 6
      sb.append("   AND STMTNO = ?");// 7
      sb.append("   AND SECTNO = ?");// 8
      sb.append("   AND OPERATOR_ID = ?");// 9
      sb.append(" WITH UR");
      SEL_BASE_SELECT = sb.toString();
   }

   private DB2PlanStatement               db2Statement;
   private String                         planTableSchema;

   private List<DB2PlanOperatorArgument>  listArguments;
   private List<DB2PlanOperatorPredicate> listPredicates;

   private Integer                        operatorId;
   private String                         operatorType;
   private Double                         totalCost;

   private Double                         estimatedCardinality = -1d;

   private String                         details;

   // TODO DF: and many many more

   // ------------
   // Constructors
   // ------------

   public DB2PlanOperator(JDBCExecutionContext context,
                          JDBCResultSet dbResult,
                          DB2PlanStatement db2Statement,
                          String planTableSchema) throws SQLException {

      this.db2Statement = db2Statement;
      this.planTableSchema = planTableSchema;

      this.operatorId = JDBCUtils.safeGetInteger(dbResult, "OPERATOR_ID");
      this.operatorType = JDBCUtils.safeGetString(dbResult, "OPERATOR_TYPE");
      this.totalCost = JDBCUtils.safeGetDouble(dbResult, "TOTAL_COST");

      loadChildren(context);

      // Build details field once
      StringBuilder sb = new StringBuilder(256);
      for (DB2PlanOperatorArgument arg : listArguments) {
         sb.append(arg.toString());
         sb.append(";");
      }
      details = sb.toString();
   }

   @Override
   public void setEstimatedCardinality(Double estimatedCardinality) {
      // TODO DF: not sure of the rule here
      this.estimatedCardinality = Math.max(this.estimatedCardinality, estimatedCardinality);
   }

   public Integer getOperatorId() {
      return operatorId;
   }

   // ----------------
   // Pproperties
   // ----------------

   @Property(viewable = true, order = 1)
   public String getOperatorType() {
      return operatorType;
   }

   @Override
   @Property(viewable = true, order = 2)
   public String getNodeName() {
      return String.valueOf(operatorId);
   }

   @Property(viewable = true, order = 3)
   public Double getTotalCost() {
      return totalCost;
   }

   @Property(viewable = true, order = 4)
   public Double getEstimatedCardinality() {
      return estimatedCardinality;
   }

   @Override
   @Property(viewable = true, order = 5)
   public String getDetails() {
      return details;
   }

   // -------------
   // Load children
   // -------------
   private void loadChildren(JDBCExecutionContext context) throws SQLException {

      listArguments = new ArrayList<DB2PlanOperatorArgument>();
      JDBCPreparedStatement sqlStmt = context.prepareStatement(String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_ARGUMENT"));
      try {
         setQueryParameters(sqlStmt);
         JDBCResultSet res = sqlStmt.executeQuery();
         try {
            while (res.next()) {
               listArguments.add(new DB2PlanOperatorArgument(res, this));
            }
         } finally {
            res.close();
         }
      } finally {
         sqlStmt.close();
      }

      listPredicates = new ArrayList<DB2PlanOperatorPredicate>();
      sqlStmt = context.prepareStatement(String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_PREDICATE"));
      try {
         setQueryParameters(sqlStmt);
         JDBCResultSet res = sqlStmt.executeQuery();
         try {
            while (res.next()) {
               listPredicates.add(new DB2PlanOperatorPredicate(res, this));
            }
         } finally {
            res.close();
         }
      } finally {
         sqlStmt.close();
      }
   }

   private void setQueryParameters(JDBCPreparedStatement sqlStmt) throws SQLException {
      sqlStmt.setString(1, db2Statement.getExplainRequester());
      sqlStmt.setTimestamp(2, db2Statement.getExplainTime());
      sqlStmt.setString(3, db2Statement.getSourceName());
      sqlStmt.setString(4, db2Statement.getSourceSchema());
      sqlStmt.setString(5, db2Statement.getSourceVersion());
      sqlStmt.setString(6, db2Statement.getExplainLevel());
      sqlStmt.setInt(7, db2Statement.getStmtNo());
      sqlStmt.setInt(8, db2Statement.getSectNo());
      sqlStmt.setInt(9, operatorId);
   }

   // -------
   // Queries
   // -------

   // public DB2PlanStatement getDb2Statement() {
   // return db2Statement;
   // }

}
