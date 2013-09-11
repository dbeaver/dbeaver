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
 */package org.jkiss.dbeaver.ext.db2.model.plan;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

/**
 * DB2 execution plan analyser
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanAnalyser implements DBCPlan {

   private static final Log     LOG         = LogFactory.getLog(DB2DataSource.class);

   private static final String  PT_DELETE   = "DELETE FROM %s.EXPLAIN_STATEMENT WHERE QUERYNO = ?";
   private static final String  PT_EXPLAIN  = "EXPLAIN PLAN SET QUERYNO = %d FOR %s";
   private static final String  SEL_STMT    = "SELECT * FROM %s.EXPLAIN_STATEMENT WHERE QUERYNO = ? AND EXPLAIN_LEVEL = 'P' WITH UR";

   private static AtomicInteger STMT_NO_GEN = new AtomicInteger(Long.valueOf(System.currentTimeMillis() / 10000000L).intValue());

   // TODO DF: most everything to be done here

   private DB2DataSource        dataSource;
   private String               query;

   private List<DB2PlanNode>    listNodes;

   private DB2PlanStatement     db2PlanStatement;

   // ------------
   // Constructors
   // ------------

   public DB2PlanAnalyser(DB2DataSource dataSource, String query) {
      this.dataSource = dataSource;
      this.query = query;
   }

   // ----------------
   // Standard Getters
   // ----------------

   @Override
   public String getQueryString() {
      return query;
   }

   @Override
   public Collection<? extends DBCPlanNode> getPlanNodes() {
      return listNodes;
   }

   // ----------------
   // Business Methods
   // ----------------

   public void explain(JDBCExecutionContext context) throws DBCException {
      Integer stmtNo = STMT_NO_GEN.incrementAndGet();
      String planTableSchema = dataSource.getPlanTableSchemaName(context);

      LOG.error("schema=" + planTableSchema + " explain " + stmtNo + " for " + query);

      try {
         // Detect plan table

         if (planTableSchema == null) {
            throw new DBCException("Plan table not found - query can't be explained");
         }

         // Delete previous statement rows
         JDBCPreparedStatement dbStat = context.prepareStatement(String.format(PT_DELETE, planTableSchema));
         try {
            dbStat.setInt(1, stmtNo);
            dbStat.execute();
         } finally {
            if (dbStat != null) {
               dbStat.close();
            }
         }

         // Explain plan
         System.out.println("no=" + stmtNo + "q=" + query);

         dbStat = context.prepareStatement(String.format(PT_EXPLAIN, stmtNo, query));
         // dbStat.setInt(1, stmtNo);
         // dbStat.setString(2, query);
         try {
            dbStat.execute();
         } finally {
            if (dbStat != null) {
               dbStat.close();
            }
         }

         // Build Node Structure
         dbStat = context.prepareStatement(String.format(SEL_STMT, planTableSchema));
         JDBCResultSet dbResult = null;
         try {
            dbStat.setInt(1, stmtNo);
            dbResult = dbStat.executeQuery();
            dbResult.next();

            db2PlanStatement = new DB2PlanStatement(context, dbResult, planTableSchema);

         } finally {
            if (dbResult != null) {
               dbResult.close();
            }
            if (dbStat != null) {
               dbStat.close();
            }
         }

         listNodes = db2PlanStatement.buildNodes();

      } catch (SQLException e) {
         throw new DBCException(e);
      }
   }
}
