/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.sql.SQLException;
import java.util.List;

public class HANAStructureAssistant extends JDBCStructureAssistant<JDBCExecutionContext> {

    private HANADataSource dataSource;

    public HANAStructureAssistant(HANADataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected JDBCDataSource getDataSource() {
        return dataSource;
    }

    public DBSObjectType[] getSupportedObjectTypes() {
        return new DBSObjectType[] { 
            RelationalObjectType.TYPE_TABLE, 
            RelationalObjectType.TYPE_VIEW, 
            RelationalObjectType.TYPE_PROCEDURE,
            RelationalObjectType.TYPE_TABLE_COLUMN, 
            RelationalObjectType.TYPE_VIEW_COLUMN, 
       };
    }

    protected void findObjectsByMask(JDBCExecutionContext executionContext, JDBCSession session, DBSObjectType objectType, DBSObject parentObject,
            String objectNameMask, boolean caseSensitive, boolean globalSearch, int maxResults,
            List<DBSObjectReference> result) throws DBException, SQLException {
        GenericSchema parentSchema = parentObject instanceof GenericSchema ? (GenericSchema) parentObject : null;

        if (objectType == RelationalObjectType.TYPE_TABLE)
            findTablesByMask(session, parentSchema, objectNameMask, caseSensitive, maxResults, result);
        if (objectType == RelationalObjectType.TYPE_VIEW)
            findViewsByMask(session, parentSchema, objectNameMask, caseSensitive, maxResults, result);
        if (objectType == RelationalObjectType.TYPE_PROCEDURE)
            findProceduresByMask(session, parentSchema, objectNameMask, caseSensitive, maxResults, result);
        if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN)
            findTableColumnsByMask(session, parentSchema, objectNameMask, caseSensitive, maxResults, result);
        if (objectType == RelationalObjectType.TYPE_VIEW_COLUMN)
            findViewColumnsByMask(session, parentSchema, objectNameMask, caseSensitive, maxResults, result);
    }

    private void findTablesByMask(JDBCSession session, GenericSchema parentSchema, String objectNameMask,
            boolean caseSensitive, int maxResults, List<DBSObjectReference> result) throws SQLException, DBException {
        String stmt =                       "SELECT SCHEMA_NAME, TABLE_NAME, COMMENTS FROM SYS.TABLES WHERE";
        stmt += caseSensitive ?             " TABLE_NAME LIKE ?" : " UPPER(TABLE_NAME) LIKE ?";
        if (parentSchema != null) stmt +=   " AND SCHEMA_NAME = ?";
        stmt +=                             " ORDER BY SCHEMA_NAME, TABLE_NAME LIMIT " + maxResults;

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, caseSensitive ? objectNameMask : objectNameMask.toUpperCase());
            if (parentSchema != null)
                dbStat.setString(2, parentSchema.getName());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int numResults = maxResults;
                while (dbResult.next() && numResults-- > 0) {
                    if (monitor.isCanceled())
                        break;
                    String schemaName = dbResult.getString(1);
                    String objectName = dbResult.getString(2);
                    String description = dbResult.getString(3);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered

                    result.add(new AbstractObjectReference(objectName, schema, description, GenericTable.class,
                            RelationalObjectType.TYPE_TABLE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericTableBase object = ((GenericObjectContainer) getContainer()).getTable(monitor,
                                    getName());
                            if (object == null) {
                                throw new DBException("Can't find object '" + getName() + "' in '"
                                        + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
                            }
                            return object;
                        }

                    });
                }
            }
        }
    }

    private void findViewsByMask(JDBCSession session, GenericSchema parentSchema, String objectNameMask,
            boolean caseSensitive, int maxResults, List<DBSObjectReference> result) throws SQLException, DBException {
        String stmt =                       "SELECT SCHEMA_NAME, VIEW_NAME, COMMENTS FROM SYS.VIEWS WHERE";
        stmt += caseSensitive ?             " VIEW_NAME LIKE ?" : " UPPER(VIEW_NAME) LIKE ?";
        if (parentSchema != null)stmt +=    " AND SCHEMA_NAME = ?";
        stmt +=                             " ORDER BY SCHEMA_NAME, VIEW_NAME LIMIT " + maxResults;

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, caseSensitive ? objectNameMask : objectNameMask.toUpperCase());
            if (parentSchema != null)
                dbStat.setString(2, parentSchema.getName());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int numResults = maxResults;
                while (dbResult.next() && numResults-- > 0) {
                    if (monitor.isCanceled())
                        break;
                    String schemaName = dbResult.getString(1);
                    String objectName = dbResult.getString(2);
                    String description = dbResult.getString(3);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered

                    result.add(new AbstractObjectReference(objectName, schema, description, GenericTable.class,
                            RelationalObjectType.TYPE_VIEW) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericTableBase object = ((GenericObjectContainer) getContainer()).getTable(monitor,
                                    getName());
                            if (object == null) {
                                throw new DBException("Can't find object '" + getName() + "' in '"
                                        + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
                            }
                            return object;
                        }

                    });
                }
            }
        }
    }

    private void findProceduresByMask(JDBCSession session, GenericSchema parentSchema, String objectNameMask,
            boolean caseSensitive, int maxResults, List<DBSObjectReference> result) throws SQLException, DBException {
        String stmt =                       "SELECT SCHEMA_NAME, PROCEDURE_NAME FROM SYS.PROCEDURES WHERE";
        stmt += caseSensitive ?             " PROCEDURE_NAME LIKE ?" : " UPPER(PROCEDURE_NAME) LIKE ?";
        if (parentSchema != null) stmt +=   " AND SCHEMA_NAME = ?";
        stmt +=                             " ORDER BY SCHEMA_NAME, PROCEDURE_NAME LIMIT " + maxResults;

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, caseSensitive ? objectNameMask : objectNameMask.toUpperCase());
            if (parentSchema != null)
                dbStat.setString(2, parentSchema.getName());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int numResults = maxResults;
                while (dbResult.next() && numResults-- > 0) {
                    if (monitor.isCanceled())
                        break;
                    String schemaName = dbResult.getString(1);
                    String objectName = dbResult.getString(2);
                    String description = null;
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered

                    result.add(new AbstractObjectReference(objectName, schema, description, GenericProcedure.class,
                            RelationalObjectType.TYPE_PROCEDURE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericProcedure object = ((GenericObjectContainer) getContainer()).getProcedure(monitor,
                                    getName());
                            if (object == null) {
                                throw new DBException("Can't find object '" + getName() + "' in '"
                                        + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
                            }
                            return object;
                        }

                    });
                }
            }
        }
    }

    private void findTableColumnsByMask(JDBCSession session, GenericSchema parentSchema, String objectNameMask,
            boolean caseSensitive, int maxResults, List<DBSObjectReference> result) throws SQLException, DBException {
        String stmt =                       "SELECT SCHEMA_NAME, TABLE_NAME, COLUMN_NAME, COMMENTS FROM SYS.TABLE_COLUMNS WHERE";
        stmt += caseSensitive ?             " COLUMN_NAME LIKE ?" : " UPPER(COLUMN_NAME) LIKE ?";
        if (parentSchema != null) stmt +=   " AND SCHEMA_NAME = ?";
        stmt +=                             " ORDER BY SCHEMA_NAME, TABLE_NAME, COLUMN_NAME LIMIT " + maxResults;

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, caseSensitive ? objectNameMask : objectNameMask.toUpperCase());
            if (parentSchema != null)
                dbStat.setString(2, parentSchema.getName());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int numResults = maxResults;
                while (dbResult.next() && numResults-- > 0) {
                    if (monitor.isCanceled())
                        break;
                    String schemaName = dbResult.getString(1);
                    String objectName = dbResult.getString(2);
                    String columnName = dbResult.getString(3);
                    String description = dbResult.getString(4);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered

                    result.add(new AbstractObjectReference(objectName, schema, description, GenericTableColumn.class,
                            RelationalObjectType.TYPE_TABLE_COLUMN) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericTableBase object = ((GenericObjectContainer) getContainer()).getTable(monitor,
                                    getName());
                            if (object == null) {
                                throw new DBException("Can't find object '" + getName() + "' in '"
                                        + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
                            }
                            GenericTableColumn column = object.getAttribute(monitor, columnName);
                            if (column == null) {
                                throw new DBException("Column '" + columnName + "' not found in table '"
                                        + object.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            }
                            return column;
                        }

                    });
                }
            }
        }
    }

    private void findViewColumnsByMask(JDBCSession session, GenericSchema parentSchema, String objectNameMask,
            boolean caseSensitive, int maxResults, List<DBSObjectReference> result) throws SQLException, DBException {
        String stmt =                       "SELECT SCHEMA_NAME, VIEW_NAME, COLUMN_NAME, COMMENTS FROM SYS.VIEW_COLUMNS WHERE";
        stmt += caseSensitive ?             " COLUMN_NAME LIKE ?" : " UPPER(COLUMN_NAME) LIKE ?";
        if (parentSchema != null) stmt +=   " AND SCHEMA_NAME = ?";
        stmt +=                             " ORDER BY SCHEMA_NAME, VIEW_NAME, COLUMN_NAME LIMIT " + maxResults;

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, caseSensitive ? objectNameMask : objectNameMask.toUpperCase());
            if (parentSchema != null)
                dbStat.setString(2, parentSchema.getName());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int numResults = maxResults;
                while (dbResult.next() && numResults-- > 0) {
                    if (monitor.isCanceled())
                        break;
                    String schemaName = dbResult.getString(1);
                    String objectName = dbResult.getString(2);
                    String columnName = dbResult.getString(3);
                    String description = dbResult.getString(4);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered

                    result.add(new AbstractObjectReference(objectName, schema, description, GenericTableColumn.class,
                            RelationalObjectType.TYPE_TABLE_COLUMN) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericTableBase object = ((GenericObjectContainer) getContainer()).getTable(monitor,
                                    getName());
                            if (object == null) {
                                throw new DBException("Can't find object '" + getName() + "' in '"
                                        + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
                            }
                            GenericTableColumn column = object.getAttribute(monitor, columnName);
                            if (column == null) {
                                throw new DBException("Column '" + columnName + "' not found in table '"
                                        + object.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            }
                            return column;
                        }

                    });
                }
            }
        }
    }

}
