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

   private static final Log    LOG                  = LogFactory.getLog(DB2Utils.class);

   private static final String CALL_DB2LK_GEN       = "CALL SYSPROC.DB2LK_GENERATE_DDL(?,?)";
   private static final String CALL_DB2LK_CLEAN     = "CALL SYSPROC.DB2LK_CLEAN_TABLE(?)";
   private static final String SEL_DB2LK            = "SELECT SQL_STMT FROM SYSTOOLS.DB2LOOK_INFO_V WHERE OP_TOKEN = ? ORDER BY OP_SEQUENCE WITH UR";
   private static final String DB2LK_COMMAND        = "-e -td %s -t %s";

   private static final String CALL_INST_OBJ        = "CALL SYSPROC.SYSINSTALLOBJECTS(?,?,?,?)";
   private static final String SYSTOOLS             = "SYSTOOLS";
   private static final int    CALL_INST_OBJ_BAD_RC = -438;

   private static final String LINE_SEP             = "\n";

   // Generate DDL
   // TODO DF: Tables in SYSTOOLS tables must exist first
   public static String generateDDLforTable(DBRProgressMonitor monitor,
                                            String statementDelimiter,
                                            DB2DataSource dataSource,
                                            DB2Table db2Table) throws DBException {
      LOG.debug("Generate DDL for " + db2Table.getFullQualifiedName());

      monitor.beginTask("Generating DDL", 1);

      // DF: Use "Undocumented" SYSPROC.DB2LK_GENERATE_DDL stored proc
      // Ref to db2look :
      // http://pic.dhe.ibm.com/infocenter/db2luw/v10r5/topic/com.ibm.db2.luw.admin.cmd.doc/doc/r0002051.html
      //
      // Option that do not seem to work: -dp -a ...

      Integer token = 0;
      StringBuilder sb = new StringBuilder(2048);
      String command = String.format(DB2LK_COMMAND, statementDelimiter, db2Table.getFullQualifiedName());

      JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Generate DDL");
      try {
         LOG.debug("Calling DB2LK_GENERATE_DDL with command : " + command);

         JDBCCallableStatement stmtSP = context.prepareCall(CALL_DB2LK_GEN);
         try {
            stmtSP.registerOutParameter(2, java.sql.Types.INTEGER);
            stmtSP.setString(1, command);
            stmtSP.executeUpdate();
            token = stmtSP.getInt(2);
         } finally {
            stmtSP.close();
         }

         LOG.debug("Token = " + token);

         // Read result
         JDBCPreparedStatement stmtSel = context.prepareStatement(SEL_DB2LK);
         try {
            stmtSel.setInt(1, token);
            JDBCResultSet dbResult = stmtSel.executeQuery();
            try {
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
            } finally {
               dbResult.close();
            }
         } finally {
            stmtSel.close();
         }

         // Clean
         JDBCCallableStatement stmtSPClean = context.prepareCall(CALL_DB2LK_CLEAN);
         try {
            stmtSPClean.setInt(1, token);
            stmtSPClean.executeUpdate();
         } finally {
            stmtSPClean.close();
         }

         LOG.debug("Terminated OK");

         return sb.toString();

      } catch (SQLException e) {
         throw new DBException(e);
      } finally {
         context.close();

         monitor.done();
      }
   }

   public static String checkExplainTables(DBRProgressMonitor monitor, DB2DataSource dataSource, String explainTableSchemaName) throws DBCException {
      LOG.debug("Check EXPLAIN tables in " + explainTableSchemaName);

      monitor.beginTask("Check EXPLAIN tables", 1);

      JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Verify EXPLAIN tables");

      try {
         // First Check with given schema
         JDBCCallableStatement stmtSP = context.prepareCall(CALL_INST_OBJ);
         try {
            stmtSP.setString(1, "EXPLAIN"); // EXPLAIN
            stmtSP.setString(2, "V"); // Verify
            stmtSP.setString(3, ""); // Tablespace
            stmtSP.setString(4, explainTableSchemaName); // Schema
            stmtSP.execute();
            LOG.debug("EXPLAIN tables with schema " + explainTableSchemaName + " found.");
            return explainTableSchemaName;

         } catch (SQLException e) {
            System.out.println(e.getErrorCode() + " " + e.getSQLState() + " " + e.getMessage());
            if (e.getErrorCode() == CALL_INST_OBJ_BAD_RC) {
               LOG.debug("No valid EXPLAIN tables found in schema " + explainTableSchemaName + ". Check within " + SYSTOOLS + ".");
               try {
                  stmtSP.setString(4, SYSTOOLS); // Schema
                  stmtSP.execute();
                  LOG.debug("EXPLAIN tables with schema " + SYSTOOLS + " found.");
                  return SYSTOOLS;
               } catch (SQLException e2) {
                  System.out.println("" + e.getErrorCode());
                  if (e.getErrorCode() == CALL_INST_OBJ_BAD_RC) {
                     LOG.warn("No valid EXPLAIN tables found in schema " + SYSTOOLS);
                     return null;
                  }
                  throw new DBCException(e);
               }
            }
            throw new DBCException(e);
         } finally {
            stmtSP.close();
         }
      } catch (SQLException e1) {
         throw new DBCException(e1);
      } finally {
         context.close();
         monitor.done();
      }
   }

   public static void createExplainTables(DBRProgressMonitor monitor, DB2DataSource dataSource, String explainTableSchemaName) throws DBCException {
      LOG.debug("Create EXPLAIN tables in " + explainTableSchemaName);

      monitor.beginTask("Create EXPLAIN Tables", 1);

      JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Create EXPLAIN tables");
      try {
         JDBCCallableStatement stmtSP = context.prepareCall(CALL_INST_OBJ);
         try {
            stmtSP.setString(1, "EXPLAIN"); // EXPLAIN
            stmtSP.setString(2, "C"); // Create
            stmtSP.setString(3, ""); // Tablespace
            stmtSP.setString(4, explainTableSchemaName); // Schema
            stmtSP.executeUpdate();

            LOG.debug("Creation EXPLAIN Tables : OK");
         } catch (SQLException e) {
            LOG.error("SQLException occured during EXPLAIN tables creation in schema " + explainTableSchemaName, e);
            throw new DBCException(e);
         } finally {
            stmtSP.close();
         }
      } catch (SQLException e1) {
         throw new DBCException(e1);
      } finally {
         context.close();
         monitor.done();
      }
   }

   private DB2Utils() {
      // Pure utility class, no instanciation allowed
   }

}
