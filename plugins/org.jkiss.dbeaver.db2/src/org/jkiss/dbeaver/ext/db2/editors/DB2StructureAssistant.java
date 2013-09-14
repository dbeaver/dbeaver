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
package org.jkiss.dbeaver.ext.db2.editors;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableType;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Structure Assistant
 * 
 * @author Denis Forveille
 * 
 */
public class DB2StructureAssistant implements DBSStructureAssistant {
   private static final Log             LOG               = LogFactory.getLog(DB2StructureAssistant.class);

   // TODO DF: Work in progess
   // For now only support Search/Autocomplete on Aliases, Tables and Views..

   private static final DBSObjectType[] HYPER_LINKS_TYPES = { DB2ObjectType.ALIAS, DB2ObjectType.TABLE, DB2ObjectType.VIEW, };
   private static final DBSObjectType[] AUTOC_OBJ_TYPES   = { DB2ObjectType.ALIAS, DB2ObjectType.TABLE, DB2ObjectType.VIEW, };
   private static final DBSObjectType[] SUPP_OBJ_TYPES    = { DB2ObjectType.ALIAS, DB2ObjectType.TABLE, DB2ObjectType.VIEW, };

   private static String                SQL_ALL;
   private static String                SQL_TAB;
   static {
      StringBuilder sb = new StringBuilder(1024);
      sb.append("SELECT TABSCHEMA,TABNAME,TYPE");
      sb.append("  FROM SYSCAT.TABLES");
      sb.append(" WHERE TABSCHEMA = ?");
      sb.append("   AND TABNAME LIKE ?");
      sb.append("   AND TYPE IN ('A','G','N','S','T','U','V','W')"); // DF : Temp
      sb.append(" WITH UR");
      SQL_TAB = sb.toString();

      sb.setLength(0);

      sb.append("SELECT TABSCHEMA,TABNAME,TYPE");
      sb.append("  FROM SYSCAT.TABLES");
      sb.append(" WHERE TABNAME LIKE ?");
      sb.append("   AND TYPE IN ('A','G','N','S','T','U','V','W')");// DF : Temp
      sb.append(" WITH UR");

      SQL_ALL = sb.toString();
   }

   private final DB2DataSource          dataSource;

   // -----------------
   // Constructors
   // -----------------
   public DB2StructureAssistant(DB2DataSource dataSource) {
      this.dataSource = dataSource;
   }

   // -----------------
   // Method Interface
   // -----------------

   @Override
   public DBSObjectType[] getSupportedObjectTypes() {
      return SUPP_OBJ_TYPES;
   }

   @Override
   public DBSObjectType[] getHyperlinkObjectTypes() {
      return HYPER_LINKS_TYPES;
   }

   @Override
   public DBSObjectType[] getAutoCompleteObjectTypes() {
      return AUTOC_OBJ_TYPES;
   }

   @Override
   public Collection<DBSObjectReference> findObjectsByMask(DBRProgressMonitor monitor,
                                                           DBSObject parentObject,
                                                           DBSObjectType[] objectTypes,
                                                           String objectNameMask,
                                                           boolean caseSensitive,
                                                           int maxResults) throws DBException {

      DB2Schema schema = parentObject instanceof DB2Schema ? (DB2Schema) parentObject : null;
      JDBCExecutionContext context = dataSource.openContext(monitor, DBCExecutionPurpose.META, "Find objects by name");

      try {
         return searchAllObjects(context, schema, objectNameMask, objectTypes, caseSensitive, maxResults);
      } catch (SQLException ex) {
         throw new DBException(ex);
      } finally {
         context.close();
      }
   }

   // -----------------
   // Helpers
   // -----------------

   private List<DBSObjectReference> searchAllObjects(final JDBCExecutionContext context,
                                                     final DB2Schema schema,
                                                     String objectNameMask,
                                                     DBSObjectType[] objectTypes,
                                                     boolean caseSensitive,
                                                     int maxResults) throws SQLException, DBException {

      List<DBSObjectReference> objects = new ArrayList<DBSObjectReference>();

      String searchObjectNameMask = objectNameMask;
      if (!caseSensitive) {
         searchObjectNameMask = searchObjectNameMask.toUpperCase();
      }

      String sql;
      if (schema != null) {
         sql = SQL_TAB;
      } else {
         sql = SQL_ALL;
      }
      JDBCPreparedStatement dbStat = context.prepareStatement(sql);

      int n = 1;
      try {
         if (schema != null) {
            dbStat.setString(n++, schema.getName());
         }
         dbStat.setString(n++, searchObjectNameMask);

         dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
         dbStat.setMaxRows(maxResults); // TODO DF: not exact as object may be filtered later

         String schemaName;
         String objectName;
         DB2Schema db2Schema;
         DB2TableType tableType;
         DB2ObjectType objectType;

         JDBCResultSet dbResult = dbStat.executeQuery();
         try {
            while (dbResult.next()) {
               if (context.getProgressMonitor().isCanceled()) {
                  break;
               }

               schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
               objectName = JDBCUtils.safeGetString(dbResult, "TABNAME");
               tableType = CommonUtils.valueOf(DB2TableType.class, JDBCUtils.safeGetString(dbResult, "TYPE"));

               db2Schema = dataSource.getSchema(context.getProgressMonitor(), schemaName);
               if (db2Schema == null) {
                  LOG.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                  continue;
               }

               objectType = tableType.getDb2ObjectType();
               objects.add(new DB2ObjectReference(objectName, db2Schema, objectType));
            }
         } finally {
            dbResult.close();
         }
      } finally {
         dbStat.close();
      }
      return objects;
   }

   // --------------
   // Helper Classes
   // --------------
   private class DB2ObjectReference extends AbstractObjectReference {

      private DB2ObjectReference(String objectName, DB2Schema db2Schema, DB2ObjectType objectType) {
         super(objectName, db2Schema, null, objectType);
      }

      @Override
      public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {

         DB2ObjectType db2ObjectType = (DB2ObjectType) getObjectType();
         DB2Schema db2Schema = (DB2Schema) getContainer();

         DBSObject object = db2ObjectType.findObject(monitor, db2Schema, getName());
         if (object == null) {
            throw new DBException(db2ObjectType + " '" + getName() + "' not found in schema '" + db2Schema.getName() + "'");
         }
         return object;
      }

   }
}
