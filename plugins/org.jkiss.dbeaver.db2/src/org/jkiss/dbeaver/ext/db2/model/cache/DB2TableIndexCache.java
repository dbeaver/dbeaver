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
package org.jkiss.dbeaver.ext.db2.model.cache;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

/**
 * Cache for DB2 Sequences
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableIndexCache extends JDBCObjectCache<DB2Table, DB2Index> {

   private static final String SQL_INDS_TAB = "SELECT * FROM SYSCAT.INDEXES WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY INDNAME WITH UR";

   @Override
   protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, DB2Table db2Table) throws SQLException {
      final JDBCPreparedStatement dbStat = context.prepareStatement(SQL_INDS_TAB);
      dbStat.setString(1, db2Table.getSchema().getName());
      dbStat.setString(2, db2Table.getName());
      return dbStat;
   }

   @Override
   protected DB2Index fetchObject(JDBCExecutionContext context, DB2Table db2Table, ResultSet dbResult) throws SQLException,
                                                                                                      DBException {

      // Lookup for indexes in right cache..
      String indexSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "INDSCHEMA");
      String indexName = JDBCUtils.safeGetStringTrimmed(dbResult, "INDNAME");
      DB2Schema tableSchema = db2Table.getSchema();
      DB2Schema indexSchema = db2Table.getDataSource().schemaLookup(context.getProgressMonitor(), tableSchema, indexSchemaName);
      return indexSchema.getIndex(context.getProgressMonitor(), indexName);
   }
}
