/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2Trigger;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Cache for DB2 Triggers
 * 
 * @author Denis Forveille
 * 
 */
// TODO DF: Correct? Triggers have no "children".. Just relationships. What Kind of cache to use here?
public final class DB2TriggerCache extends JDBCCompositeCache<DB2Schema, DB2Table, DB2Trigger, DBSObject> {

   private static String SQL_TRIG_ALL = "SELECT * FROM SYSCAT.TRIGGERS WHERE TRIGSCHEMA = ? ORDER BY TRIGNAME WITH UR";
   private static String SQL_TRIG_TAB = "SELECT * FROM SYSCAT.TRIGGERS WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY TRIGSCHEMA,TRIGNAME WITH UR";

   public DB2TriggerCache(DB2TableCache tableCache) {
      // TODO DF: Bad should be TRIGSCHEMA+TRIGNAME
      super(tableCache, DB2Table.class, "TABNAME", "TRIGNAME");
   }

   @Override
   protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, DB2Schema db2Schema, DB2Table forTable) throws SQLException {
      String sql;
      if (forTable != null) {
         sql = SQL_TRIG_TAB;
      } else {
         sql = SQL_TRIG_ALL;
      }
      JDBCPreparedStatement dbStat = context.prepareStatement(sql);
      dbStat.setString(1, db2Schema.getName());
      if (forTable != null) {
         dbStat.setString(2, forTable.getName());
      }
      return dbStat;
   }

   @Override
   protected DB2Trigger fetchObject(JDBCExecutionContext context,
                                    DB2Schema db2Schema,
                                    DB2Table db2Table,
                                    String triggerName,
                                    ResultSet dbResult) throws SQLException, DBException {

      // Trigger schema may be different from current(owner) schema..
      String trigSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "TRIGSCHEMA");
      DB2Schema parentSchema = db2Schema.getDataSource().schemaLookup(context.getProgressMonitor(), db2Schema, trigSchema);

      return new DB2Trigger(parentSchema, db2Table, dbResult);
   }

   // ----------------------------
   // Triggers don't have children
   // ----------------------------
   @Override
   protected DBSObject fetchObjectRow(JDBCExecutionContext context, DB2Table db2Table, DB2Trigger forObject, ResultSet resultSet) throws SQLException,
                                                                                                                                 DBException {
      return null;
   }

   @Override
   protected void cacheChildren(DB2Trigger db2Trigger, List<DBSObject> children) {
      // NOP
   }

}
