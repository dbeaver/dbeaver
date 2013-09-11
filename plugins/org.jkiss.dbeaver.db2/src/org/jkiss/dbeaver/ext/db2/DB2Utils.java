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
package org.jkiss.dbeaver.ext.db2;

import java.sql.Clob;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * 
 * DB2 Utils
 * 
 * @author Denis Forveille
 * 
 */
public class DB2Utils {

   private static final Log    LOG              = LogFactory.getLog(DB2Utils.class);

   private static final String CALL_DB2LK_GEN   = "CALL SYSPROC.DB2LK_GENERATE_DDL(?,?)";
   private static final String CALL_DB2LK_CLEAN = "CALL SYSPROC.DB2LK_CLEAN_TABLE(?)";
   private static final String SEL_DB2LK        = "SELECT SQL_STMT FROM SYSTOOLS.DB2LOOK_INFO_V WHERE OP_TOKEN = ? ORDER BY OP_SEQUENCE WITH UR";

   private static final String CALL_INST_OBJ    = "CALL SYSPROC.SYSINSTALLOBJECTS(?,?,?,?)";

   private static final String PLAN_TABLE_CHK   = "SELECT TABSCHEMA FROM SYSCAT.TABLES WHERE TABNAME = 'EXPLAIN_INSTANCE' AND TABSCHEMA IN('SYSTOOLS',?) WITH UR";

   private static final String LINE_SEP         = "\n";

   // Generate DDL
   public static String generateDDLforTable(DBRProgressMonitor monitor,
                                            String statementDelimiter,
                                            DB2DataSource dataSource,
                                            DB2Table db2Table) throws DBException {
      LOG.debug("generate DDL for " + db2Table.getFullQualifiedName());

      // TODO DF: Systools tables must exist first

      monitor.beginTask("Generating DDL", 1);

      // DF: Use "Undocumented" SYSPROC.DB2LK_GENERATE_DDL stored proc
      // Ref to db2look :
      // http://pic.dhe.ibm.com/infocenter/db2luw/v10r5/topic/com.ibm.db2.luw.admin.cmd.doc/doc/r0002051.html
      //
      // Option that do not seem to work: -dp -a

      StringBuilder sb = new StringBuilder(2048);

      JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Generate DDL");
      JDBCCallableStatement stmtSP = null;
      JDBCCallableStatement stmtSPClean = null;
      JDBCPreparedStatement stmtSel = null;
      JDBCResultSet dbResult = null;
      Integer token = 0;

      String command = "-e -td " + statementDelimiter + " -t " + db2Table.getFullQualifiedName();

      try {
         LOG.debug("Calling Stored Proc with command : " + command);

         stmtSP = context.prepareCall(CALL_DB2LK_GEN);
         stmtSP.registerOutParameter(2, java.sql.Types.INTEGER);
         stmtSP.setString(1, command);
         stmtSP.executeUpdate();
         token = stmtSP.getInt(2);

         LOG.debug("Token = " + token);

         // Read result
         stmtSel = context.prepareStatement(SEL_DB2LK);
         stmtSel.setInt(1, token);
         dbResult = stmtSel.executeQuery();

         Clob ddlStmt;
         Long ddlLength;
         Long ddlStart = 1L;
         while (dbResult.next()) {
            ddlStmt = dbResult.getClob(1);
            ddlLength = ddlStmt.length() + 1L;
            sb.append(ddlStmt.getSubString(ddlStart, ddlLength.intValue()));
            sb.append(LINE_SEP);
            ddlStmt.free();
         }

         // Clean
         stmtSPClean = context.prepareCall(CALL_DB2LK_CLEAN);
         stmtSPClean.setInt(1, token);
         stmtSPClean.executeUpdate();

         LOG.debug("Terminated OK");

         return sb.toString();

      } catch (SQLException e) {
         LOG.error("SQLException occured during DDL generation", e);
         throw new DBException(e);
      } finally {
         if (dbResult != null) {
            dbResult.close();
         }
         if (stmtSP != null) {
            stmtSP.close();
         }
         if (stmtSel != null) {
            stmtSel.close();
         }
         if (stmtSPClean != null) {
            stmtSPClean.close();
         }
         if (context != null) {
            context.close();
         }

         monitor.done();
      }
   }

   public static String checkExplainTables(DBRProgressMonitor monitor, DB2DataSource dataSource, String currentSchemaName) throws DBCException {
      LOG.debug("Check EXPLAIN tables existence in SYSTOOLS and " + currentSchemaName);

      // TODO DF: Systools tables can be created in different Tablespace/schema..

      monitor.beginTask("Check explain table existence", 1);

      JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Check explain table existence");
      JDBCPreparedStatement stmt = null;
      JDBCResultSet res = null;

      try {
         LOG.debug("Calling Stored Proc");

         stmt = context.prepareStatement(PLAN_TABLE_CHK);
         stmt.setString(1, currentSchemaName); //
         res = stmt.executeQuery();
         if (res.next()) {
            return res.getString(1);
         } else {
            LOG.debug("No explain tables found");
            return null;
         }
      } catch (SQLException e) {
         LOG.error("SQLException occured during EXPLAIN tables creation", e);
         throw new DBCException(e);
      } finally {
         if (res != null) {
            res.close();
         }
         if (stmt != null) {
            stmt.close();
         }
         if (context != null) {
            context.close();
         }

         monitor.done();
      }
   }

   public static void createExplainTables(DBRProgressMonitor monitor, DB2DataSource dataSource, String explainTableSchemaName) throws DBCException {
      LOG.debug("Create EXPLAIN tables in " + explainTableSchemaName);

      // TODO DF: Systools tables can be created in different Tablespace/schema..

      monitor.beginTask("Create SYSTOOLS Tables", 1);

      JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Create EXPLAIN tables");
      JDBCCallableStatement stmtSP = null;

      try {
         LOG.debug("Calling Stored Proc");

         stmtSP = context.prepareCall(CALL_INST_OBJ);
         stmtSP.setString(1, "EXPLAIN");
         stmtSP.setString(2, "C"); // Create
         stmtSP.setString(3, "SYSTOOLS"); // Tablespace
         stmtSP.setString(4, "SYSTOOLS"); // Schema
         stmtSP.executeUpdate();

         LOG.debug("Terminated OK");
      } catch (SQLException e) {
         LOG.error("SQLException occured during EXPLAIN tables creation in " + explainTableSchemaName, e);
         throw new DBCException(e);
      } finally {
         if (stmtSP != null) {
            stmtSP.close();
         }
         if (context != null) {
            context.close();
         }

         monitor.done();
      }
   }

   private DB2Utils() {
      // Pure utility class, no instanciation allowed
   }

}
