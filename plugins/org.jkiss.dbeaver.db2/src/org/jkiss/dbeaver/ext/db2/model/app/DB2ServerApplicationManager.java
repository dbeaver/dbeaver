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
package org.jkiss.dbeaver.ext.db2.model.app;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;

/**
 * DB2 Application Manager
 * 
 * @author Denis Forveille
 */
public class DB2ServerApplicationManager implements DBAServerSessionManager<DB2ServerApplication> {

   private final DB2DataSource dataSource;

   public DB2ServerApplicationManager(DB2DataSource dataSource) {
      this.dataSource = dataSource;
   }

   @Override
   public DBPDataSource getDataSource() {
      return dataSource;
   }

   @Override
   public Collection<DB2ServerApplication> getSessions(DBCExecutionContext context, Map<String, Object> options) throws DBException {
      try {
         return DB2Utils.readApplications(context.getProgressMonitor(), (JDBCExecutionContext) context);
      } catch (SQLException e) {
         throw new DBException(e);
      }
   }

   @Override
   public void alterSession(DBCExecutionContext context, DB2ServerApplication session, Map<String, Object> options) throws DBException {
      try {
         DB2Utils.forceApplication(context.getProgressMonitor(), dataSource, session.getAgentId());
      } catch (SQLException e) {
         throw new DBException(e);
      }
   }

}
