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
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2IndexColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexColOrder;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;

/**
 * Cache for DB2 Indexes
 * 
 * @author Denis Forveille
 * 
 */
public final class DB2IndexCache extends JDBCCompositeCache<DB2Schema, DB2Table, DB2Index, DB2IndexColumn> {

   private static String SQL_IND_ALL;
   private static String SQL_IND_TAB;

   static {
      StringBuilder sb = new StringBuilder(512);
      sb.append(" SELECT I.*");
      sb.append("       ,ICU.COLNAME,ICU.COLSEQ,ICU.COLORDER,ICU.COLLATIONSCHEMA,ICU.COLLATIONNAME");
      sb.append("   FROM SYSCAT.INDEXES I");
      sb.append("       ,SYSCAT.INDEXCOLUSE ICU");
      sb.append("  WHERE I.INDSCHEMA = ?");
      sb.append("    AND ICU.INDNAME = I.INDNAME");
      sb.append("    AND ICU.INDSCHEMA = I.INDSCHEMA");
      sb.append("  ORDER BY I.INDNAME");
      sb.append("         , ICU.COLSEQ");
      sb.append(" WITH UR");
      SQL_IND_ALL = sb.toString();

      sb.setLength(0);

      sb.append(" SELECT I.*");
      sb.append("       ,ICU.COLNAME,ICU.COLSEQ,ICU.COLORDER,ICU.COLLATIONSCHEMA,ICU.COLLATIONNAME");
      sb.append("   FROM SYSCAT.INDEXES I");
      sb.append("       ,SYSCAT.INDEXCOLUSE ICU");
      sb.append("  WHERE I.TABSCHEMA = ?");
      sb.append("    AND I.TABNAME = ?");
      sb.append("    AND ICU.INDNAME = I.INDNAME");
      sb.append("    AND ICU.INDSCHEMA = I.INDSCHEMA");
      sb.append("  ORDER BY I.INDSCHEMA");
      sb.append("         , I.INDNAME");
      sb.append("         , ICU.COLSEQ");
      sb.append(" WITH UR");
      SQL_IND_TAB = sb.toString();
   }

   public DB2IndexCache(DB2TableCache tableCache) {
      // TODO DF: Bad Table "SCHEMA.TABLE" may have 2 indexes with same
      // "INDNAME": SCHEMA.IX1 and OTHERSCHEMA.IX1 ...
      super(tableCache, DB2Table.class, "TABNAME", "INDNAME");
   }

   @Override
   protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, DB2Schema db2Schema, DB2Table forTable) throws SQLException {

      String sql;
      if (forTable != null) {
         sql = SQL_IND_TAB;
      } else {
         sql = SQL_IND_ALL;
      }

      JDBCPreparedStatement dbStat = context.prepareStatement(sql);
      dbStat.setString(1, db2Schema.getName());
      if (forTable != null) {
         dbStat.setString(2, forTable.getName());
      }
      return dbStat;
   }

   @Override
   protected DB2Index fetchObject(JDBCExecutionContext context,
                                  DB2Schema db2Schema,
                                  DB2Table db2Table,
                                  String indexName,
                                  ResultSet dbResult) throws SQLException, DBException {

      // Index schema may be different from current(owner) schema..
      String indSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "INDSCHEMA");
      DB2Schema parentSchema = db2Schema.getDataSource().schemaLookup(context.getProgressMonitor(), db2Schema, indSchema);

      return new DB2Index(parentSchema, db2Table, indexName, dbResult);
   }

   @Override
   protected DB2IndexColumn fetchObjectRow(JDBCExecutionContext context, DB2Table db2Table, DB2Index db2Index, ResultSet dbResult) throws SQLException,
                                                                                                                                  DBException {

      String columnName = JDBCUtils.safeGetString(dbResult, "COLNAME");
      Integer colSeq = JDBCUtils.safeGetInteger(dbResult, "COLSEQ");
      DB2IndexColOrder colOrder = DB2IndexColOrder.valueOf(JDBCUtils.safeGetString(dbResult, "COLORDER"));
      String collationSchema = JDBCUtils.safeGetString(dbResult, "COLLATIONSCHEMA");
      String collationNane = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME");

      DB2TableColumn tableColumn = db2Table.getAttribute(context.getProgressMonitor(), columnName);
      if (tableColumn == null) {
         log.debug("Column '" + columnName + "' not found in table '" + db2Table.getName() + "' for index '" + db2Index.getName()
                  + "'");
         return null;
      }

      return new DB2IndexColumn(db2Index, tableColumn, colSeq, colOrder, collationSchema, collationNane);
   }

   @Override
   protected void cacheChildren(DB2Index db2Index, List<DB2IndexColumn> rows) {
      db2Index.setColumns(rows);
   }

}
