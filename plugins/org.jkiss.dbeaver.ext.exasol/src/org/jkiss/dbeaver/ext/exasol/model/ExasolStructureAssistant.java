/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.exasol.model;


import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolObjectType;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
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
import java.util.List;


public class ExasolStructureAssistant implements DBSStructureAssistant<ExasolExecutionContext> {


    private static final Log LOG = Log.getLog(ExasolStructureAssistant.class);


    private static final DBSObjectType[] SUPP_OBJ_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.VIEW, ExasolObjectType.COLUMN, ExasolObjectType.SCHEMA};
    private static final DBSObjectType[] HYPER_LINKS_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.COLUMN, ExasolObjectType.VIEW, ExasolObjectType.SCHEMA};
    private static final DBSObjectType[] AUTOC_OBJ_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.VIEW, ExasolObjectType.COLUMN, ExasolObjectType.SCHEMA};




    private static final String SQL_TABLES_ALL = "/*snapshot execution*/ SELECT table_schem,table_name,table_type from \"$ODBCJDBC\".ALL_TABLES WHERE TABLE_NAME = '%s' AND TABLE_TYPE IN (%s)";
    private static final String SQL_TABLES_SCHEMA = "/*snapshot execution*/ SELECT table_schem,table_name,table_type from \"$ODBCJDBC\".ALL_TABLES WHERE TABLE_SCHEM = '%s' AND TABLE_NAME LIKE '%%%s%%' AND TABLE_TYPE IN (%s)";
    //private static final String SQL_COLS_ALL = "SELECT TABLE_SCHEM,TABLE_NAME,COLUMN_NAME from \"$ODBCJDBC\".ALL_COLUMNS WHERE COLUMN_NAME LIKE '%s'";
    private static final String SQL_COLS_SCHEMA = "/*snapshot execution*/ SELECT TABLE_SCHEM,TABLE_NAME,COLUMN_NAME from \"$ODBCJDBC\".ALL_COLUMNS WHERE TABLE_SCHEM = '%s' and COLUMN_NAME LIKE '%%%s%%'";


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
    public DBSObjectType[] getSearchObjectTypes() {
        return getSupportedObjectTypes();
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
    public List<DBSObjectReference> findObjectsByMask(
        @NotNull DBRProgressMonitor monitor, @NotNull ExasolExecutionContext executionContext, DBSObject parentObject,
        DBSObjectType[] objectTypes, String objectNameMask, boolean caseSensitive, boolean globalSearch,
        int maxResults) throws DBException {
        LOG.debug(objectNameMask);


        List<ExasolObjectType> exasolObjectTypes = new ArrayList<>(objectTypes.length);
        for (DBSObjectType dbsObjectType : objectTypes) {
            exasolObjectTypes.add((ExasolObjectType) dbsObjectType);
        }

        ExasolSchema schema = parentObject instanceof ExasolSchema ? (ExasolSchema) parentObject : null;
        if (schema == null) {
            schema = executionContext.getContextDefaults().getDefaultSchema();
        }

        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, "Find objects by name")) {
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
        String sql;
        if (schema != null) {
            sql = String.format(SQL_TABLES_SCHEMA, ExasolUtils.quoteString(schema.getName()),ExasolUtils.quoteString(searchObjectNameMask), buildTableTypes(exasolObjectTypes)) ;
        } else {
            sql = String.format(SQL_TABLES_ALL, ExasolUtils.quoteString(searchObjectNameMask), buildTableTypes(exasolObjectTypes));
        }

        try (JDBCStatement dbStat = session.createStatement()) {
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);


            String schemaName;
            String objectName;
            ExasolSchema exasolSchema;
            ExasolObjectType objectType;


            try (JDBCResultSet dbResult = dbStat.executeQuery(sql)) {
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
            sql = String.format(SQL_COLS_SCHEMA, ExasolUtils.quoteString(schema.getName()), ExasolUtils.quoteString(searchObjectNameMask));
        } else {
            // sql = String.format(SQL_COLS_ALL, ExasolUtils.quoteString(searchObjectNameMask));
        	// search for columns is to slow in exasol
        	return;
        }


        try (JDBCStatement dbStat = session.createStatement()) {


            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);


            String tableSchemaName;
            String tableOrViewName;
            String columnName;
            ExasolSchema exasolSchema;
            ExasolTable exasolTable;


            try (JDBCResultSet dbResult = dbStat.executeQuery(sql)) {
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


    private String buildTableTypes(List<ExasolObjectType> objectTypes) {
        List<String> types = new ArrayList<>();
        for (ExasolObjectType objectType : objectTypes) {
            if (objectType.equals(ExasolObjectType.TABLE)) {
                types.add("'" + ExasolObjectType.TABLE.name() + "'");
            }
            if (objectType.equals(ExasolObjectType.VIEW)) {
                types.add("'" + ExasolObjectType.VIEW.name() + "'");
            }


        }
        return CommonUtils.joinStrings(",", types);
    }


}

