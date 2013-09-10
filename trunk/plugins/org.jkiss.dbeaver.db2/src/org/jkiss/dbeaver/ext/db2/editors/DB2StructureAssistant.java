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
package org.jkiss.dbeaver.ext.db2.editors;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ConstraintType;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;

/**
 * DB2StructureAssistant
 */
public class DB2StructureAssistant implements DBSStructureAssistant {
   static protected final Log  log = LogFactory.getLog(DB2StructureAssistant.class);

   private final DB2DataSource dataSource;

   public DB2StructureAssistant(DB2DataSource dataSource) {
      this.dataSource = dataSource;
   }

   @Override
   public DBSObjectType[] getSupportedObjectTypes() {
      return new DBSObjectType[] { DB2ObjectType.TABLE, DB2ObjectType.CONSTRAINT, DB2ObjectType.FOREIGN_KEY, DB2ObjectType.INDEX,
               DB2ObjectType.PROCEDURE, DB2ObjectType.TRIGGER, };
   }

   @Override
   public DBSObjectType[] getHyperlinkObjectTypes() {
      return new DBSObjectType[] { DB2ObjectType.TABLE, DB2ObjectType.VIEW, };
   }

   @Override
   public DBSObjectType[] getAutoCompleteObjectTypes() {
      return new DBSObjectType[] { DB2ObjectType.TABLE, DB2ObjectType.VIEW, };
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
         List<DBSObjectReference> objects = new ArrayList<DBSObjectReference>();

         // Search all objects
         searchAllObjects(context, schema, objectNameMask, objectTypes, caseSensitive, maxResults, objects);

         if (CommonUtils.contains(objectTypes, DB2ObjectType.CONSTRAINT, DB2ObjectType.FOREIGN_KEY) && objects.size() < maxResults) {
            // Search constraints
            findConstraintsByMask(context, schema, objectNameMask, objectTypes, maxResults, objects);
         }

         return objects;
      } catch (SQLException ex) {
         throw new DBException(ex);
      } finally {
         context.close();
      }
   }

   private void findConstraintsByMask(JDBCExecutionContext context,
                                      final DB2Schema schema,
                                      String constrNameMask,
                                      DBSObjectType[] objectTypes,
                                      int maxResults,
                                      List<DBSObjectReference> objects) throws SQLException, DBException {
      DBRProgressMonitor monitor = context.getProgressMonitor();

      List<DBSObjectType> objectTypesList = Arrays.asList(objectTypes);
      final boolean hasFK = objectTypesList.contains(DB2ObjectType.FOREIGN_KEY);
      final boolean hasConstraints = objectTypesList.contains(DB2ObjectType.CONSTRAINT);

      // Load tables
      JDBCPreparedStatement dbStat = context.prepareStatement("SELECT \n"
               + "OWNER, TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE, SEARCH_CONDITION, STATUS\n" + "FROM SYS.ALL_CONSTRAINTS\n"
               + "WHERE CONSTRAINT_NAME like ?" + (!hasFK ? " AND CONSTRAINT_TYPE<>'R'" : "")
               + (schema != null ? " AND OWNER=?" : ""));
      try {
         dbStat.setString(1, constrNameMask);
         if (schema != null) {
            dbStat.setString(2, schema.getName());
         }
         JDBCResultSet dbResult = dbStat.executeQuery();
         try {
            int tableNum = maxResults;
            while (dbResult.next() && tableNum-- > 0) {
               if (monitor.isCanceled()) {
                  break;
               }
               final String schemaName = JDBCUtils.safeGetString(dbResult, DB2Constants.COL_OWNER);
               final String tableName = JDBCUtils.safeGetString(dbResult, DB2Constants.COL_TABLE_NAME);
               final String constrName = JDBCUtils.safeGetString(dbResult, DB2Constants.COL_CONSTRAINT_NAME);
               final String constrType = JDBCUtils.safeGetString(dbResult, DB2Constants.COL_CONSTRAINT_TYPE);
               final DBSEntityConstraintType type = DB2ConstraintType.getConstraintType(constrType);
               objects.add(new AbstractObjectReference(constrName,
                                                       dataSource.getSchema(context.getProgressMonitor(), schemaName),
                                                       null,
                                                       type == DBSEntityConstraintType.FOREIGN_KEY ? DB2ObjectType.FOREIGN_KEY
                                                                : DB2ObjectType.CONSTRAINT) {
                  @Override
                  public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                     DB2Schema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                     if (tableSchema == null) {
                        throw new DBException("Constraint schema '" + schemaName + "' not found");
                     }
                     DB2Table table = tableSchema.getTable(monitor, tableName);
                     if (table == null) {
                        throw new DBException("Constraint table '" + tableName + "' not found in catalog '" + tableSchema.getName()
                                 + "'");
                     }
                     DBSObject constraint = null;
                     if (hasFK && type == DBSEntityConstraintType.FOREIGN_KEY) {
                        constraint = table.getAssociation(monitor, constrName);
                     }
                     if (hasConstraints && type != DBSEntityConstraintType.FOREIGN_KEY) {
                        constraint = table.getConstraint(monitor, constrName);
                     }
                     if (constraint == null) {
                        throw new DBException("Constraint '" + constrName + "' not found in table '" + table.getFullQualifiedName()
                                 + "'");
                     }
                     return constraint;
                  }
               });
            }
         } finally {
            dbResult.close();
         }
      } finally {
         dbStat.close();
      }
   }

   private void searchAllObjects(final JDBCExecutionContext context,
                                 final DB2Schema schema,
                                 String objectNameMask,
                                 DBSObjectType[] objectTypes,
                                 boolean caseSensitive,
                                 int maxResults,
                                 List<DBSObjectReference> objects) throws SQLException, DBException {
      StringBuilder objectTypeClause = new StringBuilder(100);
      final List<DB2ObjectType> db2ObjectTypes = new ArrayList<DB2ObjectType>(objectTypes.length + 2);
      for (DBSObjectType objectType : objectTypes) {
         if (objectType instanceof DB2ObjectType) {
            db2ObjectTypes.add((DB2ObjectType) objectType);
            if (objectType == DB2ObjectType.PROCEDURE) {
               db2ObjectTypes.add(DB2ObjectType.FUNCTION);
            } else if (objectType == DB2ObjectType.TABLE) {
               db2ObjectTypes.add(DB2ObjectType.VIEW);
               db2ObjectTypes.add(DB2ObjectType.MATERIALIZED_VIEW);
            }
         }
      }
      for (DB2ObjectType objectType : db2ObjectTypes) {
         if (objectTypeClause.length() > 0)
            objectTypeClause.append(",");
         objectTypeClause.append("'").append(objectType.getTypeName()).append("'");
      }
      if (objectTypeClause.length() == 0) {
         return;
      }
      // Seek for objects (join with public synonyms)
      JDBCPreparedStatement dbStat = context
               .prepareStatement("SELECT DISTINCT OWNER,OBJECT_NAME,OBJECT_TYPE FROM (SELECT OWNER,OBJECT_NAME,OBJECT_TYPE FROM ALL_OBJECTS WHERE "
                        + "OBJECT_TYPE IN ("
                        + objectTypeClause
                        + ") AND OBJECT_NAME LIKE ? "
                        + (schema == null ? "" : " AND OWNER=?")
                        + "UNION ALL\n"
                        + "SELECT O.OWNER,O.OBJECT_NAME,O.OBJECT_TYPE\n"
                        + "FROM ALL_SYNONYMS S,ALL_OBJECTS O\n"
                        + "WHERE O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME AND S.OWNER='PUBLIC' AND S.SYNONYM_NAME LIKE ?)"
                        + "\nORDER BY OBJECT_NAME");
      try {
         if (!caseSensitive) {
            objectNameMask = objectNameMask.toUpperCase();
         }
         dbStat.setString(1, objectNameMask);
         if (schema != null) {
            dbStat.setString(2, schema.getName());
         }
         dbStat.setString(schema != null ? 3 : 2, objectNameMask);
         dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
         JDBCResultSet dbResult = dbStat.executeQuery();
         try {
            while (objects.size() < maxResults && dbResult.next()) {
               if (context.getProgressMonitor().isCanceled()) {
                  break;
               }
               final String schemaName = JDBCUtils.safeGetString(dbResult, "OWNER");
               final String objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
               final String objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
               final DB2ObjectType objectType = DB2ObjectType.getByType(objectTypeName);
               if (objectType != null && objectType.isBrowsable() && db2ObjectTypes.contains(objectType)) {
                  DB2Schema objectSchema = dataSource.getSchema(context.getProgressMonitor(), schemaName);
                  if (objectSchema == null) {
                     log.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                     continue;
                  }
                  objects.add(new AbstractObjectReference(objectName, objectSchema, null, objectType) {
                     @Override
                     public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                        DB2Schema tableSchema = (DB2Schema) getContainer();
                        DBSObject object = objectType.findObject(context.getProgressMonitor(), tableSchema, objectName);
                        if (object == null) {
                           throw new DBException(objectTypeName + " '" + objectName + "' not found in schema '"
                                    + tableSchema.getName() + "'");
                        }
                        return object;
                     }
                  });
               }
            }
         } finally {
            dbResult.close();
         }
      } finally {
         dbStat.close();
      }
   }

}
