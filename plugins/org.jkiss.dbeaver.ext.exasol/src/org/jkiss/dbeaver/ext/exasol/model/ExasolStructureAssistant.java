/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.exasol.model;


import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolObjectType;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ExasolStructureAssistant implements DBSStructureAssistant {


    private static final Log LOG = Log.getLog(ExasolStructureAssistant.class);


    private static final DBSObjectType[] SUPP_OBJ_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.VIEW, ExasolObjectType.COLUMN, ExasolObjectType.SCHEMA};
    private static final DBSObjectType[] HYPER_LINKS_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.COLUMN, ExasolObjectType.VIEW, ExasolObjectType.SCHEMA};
    private static final DBSObjectType[] AUTOC_OBJ_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.VIEW, ExasolObjectType.COLUMN, ExasolObjectType.SCHEMA};




    private static final String SQL_TABLES_ALL = "SELECT table_schem,table_name,table_type from \"$ODBCJDBC\".ALL_TABLES WHERE TABLE_NAME = ? AND TABLE_TYPE IN (%s)";
    private static final String SQL_TABLES_SCHEMA = "SELECT table_schem,table_name,table_type from \"$ODBCJDBC\".ALL_TABLES WHERE TABLE_SCHEM = ? AND TABLE_NAME LIKE ? AND TABLE_TYPE IN (%s)";
    private static final String SQL_COLS_ALL = "SELECT TABLE_SCHEM,TABLE_NAME,COLUMN_NAME from \"$ODBCJDBC\".ALL_COLUMNS WHERE COLUMN_NAME LIKE ?";
    private static final String SQL_COLS_SCHEMA = "SELECT TABLE_SCHEM,TABLE_NAME,COLUMN_NAME from \"$ODBCJDBC\".ALL_COLUMNS WHERE TABLE_SCHEM = ? and COLUMN_NAME LIKE ?";


    private ExasolDataSource dataSource;


    // -----------------
    // Constructors
    // -----------------
    public ExasolStructureAssistant(ExasolDataSource dataSource) {
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


    @NotNull
    @Override
    public Collection<DBSObjectReference> findObjectsByMask(DBRProgressMonitor monitor, DBSObject parentObject,
                                                            DBSObjectType[] objectTypes, String objectNameMask, boolean caseSensitive, boolean globalSearch,
                                                            int maxResults) throws DBException {
        LOG.debug(objectNameMask);


        List<ExasolObjectType> exasolObjectTypes = new ArrayList<>(objectTypes.length);
        for (DBSObjectType dbsObjectType : objectTypes) {
            exasolObjectTypes.add((ExasolObjectType) dbsObjectType);
        }


        ExasolSchema schema = parentObject instanceof ExasolSchema ? (ExasolSchema) parentObject : null;


        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Find objects by name")) {
            return searchAllObjects(session, schema, objectNameMask, exasolObjectTypes, caseSensitive, maxResults);
        } catch (SQLException ex) {
            throw new DBException(ex, dataSource);
        }


    }
    // -----------------
    // Helpers
    // -----------------


    private List<DBSObjectReference> searchAllObjects(final JDBCSession session, final ExasolSchema schema, String objectNameMask,
                                                      List<ExasolObjectType> exasolObjectTypes, boolean caseSensitive, int maxResults) throws SQLException, DBException {


        List<DBSObjectReference> objects = new ArrayList<>();


        String searchObjectNameMask = objectNameMask;
        if (!caseSensitive) {
            searchObjectNameMask = searchObjectNameMask.toUpperCase();
        }


        int nbResults = 0;


        // Tables, Views
        if ((exasolObjectTypes.contains(ExasolObjectType.TABLE)) || (exasolObjectTypes.contains(ExasolObjectType.VIEW))) {


            searchTables(session, schema, searchObjectNameMask, exasolObjectTypes, maxResults, objects, nbResults);


            if (nbResults >= maxResults) {
                return objects;
            }
        }


        // Columns
        if (exasolObjectTypes.contains(ExasolObjectType.COLUMN)) {
            searchColumns(session, schema, searchObjectNameMask, exasolObjectTypes, maxResults, objects, nbResults);
        }


        return objects;
    }


    // --------------
    // Helper Classes
    // --------------


    private void searchTables(JDBCSession session, ExasolSchema schema, String searchObjectNameMask,
                              List<ExasolObjectType> exasolObjectTypes, int maxResults, List<DBSObjectReference> objects, int nbResults) throws SQLException,
        DBException {
        String baseSQL;
        if (schema != null) {
            baseSQL = SQL_TABLES_SCHEMA;
        } else {
            baseSQL = SQL_TABLES_ALL;
        }


        String sql = buildTableSQL(baseSQL, exasolObjectTypes);


        int n = 1;
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            if (schema != null) {
                dbStat.setString(n++, schema.getName());
            }
            dbStat.setString(n++, searchObjectNameMask);


            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);


            String schemaName;
            String objectName;
            ExasolSchema exasolSchema;
            ExasolObjectType objectType;


            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }


                    if (nbResults++ >= maxResults) {
                        break;
                    }


                    schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABLE_SCHEM");
                    objectName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");


                    exasolSchema = dataSource.getSchema(session.getProgressMonitor(), schemaName);
                    if (exasolSchema == null) {
                        LOG.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                        continue;
                    }


                    objectType = ExasolObjectType.TABLE;
                    objects.add(new ExasolObjectReference(objectName, exasolSchema, objectType));
                }
            }
        }
    }


    private void searchColumns(JDBCSession session, ExasolSchema schema, String searchObjectNameMask, List<ExasolObjectType> objectTypes,
                               int maxResults, List<DBSObjectReference> objects, int nbResults) throws SQLException, DBException {
        String sql;
        if (schema != null) {
            sql = SQL_COLS_SCHEMA;
        } else {
            sql = SQL_COLS_ALL;
        }


        int n = 1;
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            if (schema != null) {
                dbStat.setString(n++, schema.getName());
            }
            dbStat.setString(n++, searchObjectNameMask);


            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);


            String tableSchemaName;
            String tableOrViewName;
            String columnName;
            ExasolSchema exasolSchema;
            ExasolTable exasolTable;


            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }


                    if (nbResults++ >= maxResults) {
                        return;
                    }


                    tableSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABLE_SCHEM");
                    tableOrViewName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
                    columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");


                    exasolSchema = dataSource.getSchema(session.getProgressMonitor(), tableSchemaName);
                    if (exasolSchema == null) {
                        LOG.debug("Schema '" + tableSchemaName + "' not found. Probably was filtered");
                        continue;
                    }
                    // Try with table, then view
                    exasolTable = exasolSchema.getTable(session.getProgressMonitor(), tableOrViewName);
                    if (exasolTable != null) {
                        objects.add(new ExasolObjectReference(columnName, exasolTable, ExasolObjectType.COLUMN));
                    }


                }
            }
        }
    }


    private class ExasolObjectReference extends AbstractObjectReference {


        private ExasolObjectReference(String objectName, ExasolSchema exasolSchema, ExasolObjectType objectType) {
            super(objectName, exasolSchema, null, ExasolSchema.class, objectType);
        }


        private ExasolObjectReference(String objectName, ExasolTable exasolTable, ExasolObjectType objectType) {
            super(objectName, exasolTable, null, ExasolTable.class, objectType);
        }




        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {


            ExasolObjectType exasolObjectType = (ExasolObjectType) getObjectType();


            if (getContainer() instanceof ExasolSchema) {
                ExasolSchema exasolSchema = (ExasolSchema) getContainer();


                DBSObject object = exasolObjectType.findObject(monitor, exasolSchema, getName());
                if (object == null) {
                    throw new DBException(exasolObjectType + " '" + getName() + "' not found in schema '" + exasolSchema.getName() + "'");
                }
                return object;
            }
            if (getContainer() instanceof ExasolTable) {
                ExasolTable exasolTable = (ExasolTable) getContainer();


                DBSObject object = exasolObjectType.findObject(monitor, exasolTable, getName());
                if (object == null) {
                    throw new DBException(exasolObjectType + " '" + getName() + "' not found in table '" + exasolTable.getName() + "'");
                }
                return object;
            }
            return null;
        }


    }


    private String buildTableSQL(String baseStatement, List<ExasolObjectType> objectTypes) {
        List<String> types = new ArrayList<>();
        for (ExasolObjectType objectType : objectTypes) {
            if (objectType.equals(ExasolObjectType.TABLE)) {
                types.add("'" + ExasolObjectType.TABLE.name() + "'");
            }
            if (objectType.equals(ExasolObjectType.VIEW)) {
                types.add("'" + ExasolObjectType.VIEW.name() + "'");
            }


        }
        return String.format(baseStatement, CommonUtils.joinStrings(",", types));
    }


}

