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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.db2.info.DB2Application;
import org.jkiss.dbeaver.ext.db2.info.DB2Parameter;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * 
 * DB2 Util method to deal with specific database wide queries
 * 
 * @author Denis Forveille
 * 
 */
public class DB2InfoUtils {

   private static final Log    LOG            = LogFactory.getLog(DB2InfoUtils.class);

   private static final String SEL_APP        = "SELECT * FROM SYSIBMADM.APPLICATIONS WITH UR";
   private static final String SEL_DB_PARAMS  = "SELECT * FROM SYSIBMADM.DBCFG ORDER BY NAME  WITH UR";
   private static final String SEL_DBM_PARAMS = "SELECT * FROM SYSIBMADM.DBMCFG WITH UR";

   // TODO D: could propably be factorized or genreric-ified

   public static List<DB2Application> readApplications(DBRProgressMonitor monitor, JDBCExecutionContext context) throws SQLException {

      LOG.debug("readApplications");

      List<DB2Application> listApplications = new ArrayList<DB2Application>();
      JDBCPreparedStatement dbStat = context.prepareStatement(SEL_APP);
      try {
         JDBCResultSet dbResult = dbStat.executeQuery();
         try {
            while (dbResult.next()) {
               listApplications.add(new DB2Application((DB2DataSource) context.getDataSource(), dbResult));
            }
         } finally {
            dbResult.close();
         }
      } finally {
         dbStat.close();
      }
      return listApplications;
   }

   public static List<DB2Parameter> readDBCfg(DBRProgressMonitor monitor, JDBCExecutionContext context) throws SQLException {

      LOG.debug("readDBCfg");

      List<DB2Parameter> listDBParameters = new ArrayList<DB2Parameter>();
      JDBCPreparedStatement dbStat = context.prepareStatement(SEL_DB_PARAMS);
      try {
         JDBCResultSet dbResult = dbStat.executeQuery();
         try {
            while (dbResult.next()) {
               listDBParameters.add(new DB2Parameter((DB2DataSource) context.getDataSource(), dbResult));
            }
         } finally {
            dbResult.close();
         }
      } finally {
         dbStat.close();
      }
      return listDBParameters;
   }

   public static List<DB2Parameter> readDBMCfg(DBRProgressMonitor monitor, JDBCExecutionContext context) throws SQLException {

      LOG.debug("readDBMCfg");

      List<DB2Parameter> listDBMParameters = new ArrayList<DB2Parameter>();
      JDBCPreparedStatement dbStat = context.prepareStatement(SEL_DBM_PARAMS);
      try {
         JDBCResultSet dbResult = dbStat.executeQuery();
         try {
            while (dbResult.next()) {
               listDBMParameters.add(new DB2Parameter((DB2DataSource) context.getDataSource(), dbResult));
            }
         } finally {
            dbResult.close();
         }
      } finally {
         dbStat.close();
      }
      return listDBMParameters;
   }

   private DB2InfoUtils() {
      // Pure utility class, no instanciation allowed
   }

}
