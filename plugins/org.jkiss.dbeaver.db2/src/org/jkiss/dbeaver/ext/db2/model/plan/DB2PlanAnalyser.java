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

import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.utils.SecurityUtils;

/**
 * DB2 execution plan analyser
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanAnalyser implements DBCPlan {

   // TODO DF: most everything to be done here

   private DB2DataSource   dataSource;
   private String          query;

   private DB2PlanInstance db2PlanInstance;

   public DB2PlanAnalyser(DB2DataSource dataSource, String query) {
      this.dataSource = dataSource;
      this.query = query;
   }

   @Override
   public String getQueryString() {
      return query;
   }

   @Override
   public Collection<? extends DBCPlanNode> getPlanNodes() {
      // TODO DF: don'T understand whan a DBCPlanNode is ...
      return null;
   }

   public void explain(JDBCExecutionContext context) throws DBCException {
      String planStmtId = SecurityUtils.generateUniqueId();
      try {
         // Detect plan table
         String planTableName = dataSource.getPlanTableName(context);
         if (planTableName == null) {
            throw new DBCException("Plan table not found - query can't be explained");
         }

         // Delete previous statement rows
         // (actually there should be no statement with this id -
         // but let's do it, just in case)
         JDBCPreparedStatement dbStat = context.prepareStatement("DELETE FROM " + planTableName + " WHERE STATEMENT_ID=? ");
         try {
            dbStat.setString(1, planStmtId);
            dbStat.execute();
         } finally {
            dbStat.close();
         }

         // Explain plan
         StringBuilder explainSQL = new StringBuilder();
         explainSQL.append("EXPLAIN PLAN ").append("\n").append("SET STATEMENT_ID = '").append(planStmtId).append("'\n")
                  .append("INTO ").append(planTableName).append("\n").append("FOR ").append(query);
         dbStat = context.prepareStatement(explainSQL.toString());
         try {
            dbStat.execute();
         } finally {
            dbStat.close();
         }

         // Read explained plan
         dbStat = context.prepareStatement("SELECT * FROM " + planTableName + " WHERE STATEMENT_ID=? ORDER BY ID");
         JDBCResultSet dbResult = null;
         try {
            dbStat.setString(1, planStmtId);
            dbResult = dbStat.executeQuery();

            db2PlanInstance = new DB2PlanInstance(dataSource, context, dbResult);

         } finally {
            if (dbResult != null) {
               dbResult.close();
            }
            if (dbStat != null) {
               dbStat.close();
            }
         }
      } catch (SQLException e) {
         throw new DBCException(e);
      }
   }

}
