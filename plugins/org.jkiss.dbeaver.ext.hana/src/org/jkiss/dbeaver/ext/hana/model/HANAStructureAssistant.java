/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HANAStructureAssistant extends JDBCStructureAssistant<JDBCExecutionContext> {
    private final HANADataSource dataSource;

    public HANAStructureAssistant(HANADataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected JDBCDataSource getDataSource() {
        return dataSource;
    }

    public DBSObjectType[] getSupportedObjectTypes() {
        return new DBSObjectType[] {
            HANAObjectType.TABLE,
            HANAObjectType.VIEW,
            HANAObjectType.PROCEDURE,
            HANAObjectType.SYNONYM,
            RelationalObjectType.TYPE_TABLE_COLUMN,
            RelationalObjectType.TYPE_VIEW_COLUMN, 
       };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes() {
        return new DBSObjectType[]{
                HANAObjectType.TABLE,
                HANAObjectType.VIEW,
                HANAObjectType.PROCEDURE,
                HANAObjectType.SYNONYM
        };
    }

    @Override
    protected void findObjectsByMask(@NotNull JDBCExecutionContext executionContext, @NotNull JDBCSession session, @NotNull DBSObjectType objectType,
                                     @NotNull ObjectsSearchParams params, @NotNull List<DBSObjectReference> references)
                                        throws SQLException {
        GenericSchema parentSchema = params.getParentObject() instanceof GenericSchema ? (GenericSchema) params.getParentObject() : null;

        if (objectType == RelationalObjectType.TYPE_TABLE)
            findTablesByMask(session, parentSchema, params, references);
        if (objectType == RelationalObjectType.TYPE_VIEW)
            findViewsByMask(session, parentSchema, params, references);
        if (objectType == RelationalObjectType.TYPE_PROCEDURE)
            findProceduresByMask(session, parentSchema, params, references);
        if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN)
            findTableColumnsByMask(session, parentSchema, params, references);
        if (objectType == RelationalObjectType.TYPE_VIEW_COLUMN)
            findViewColumnsByMask(session, parentSchema, params, references);
    }

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext executionContext,
                                                      @NotNull ObjectsSearchParams params) throws DBException {
        List<DBSObjectReference> result = new ArrayList<>();
        List<DBSObjectType> objectTypesList = Arrays.asList(params.getObjectTypes());
        StringBuilder objectTypeClause = new StringBuilder(100);
        GenericSchema parentSchema = params.getParentObject() instanceof GenericSchema ?
                (GenericSchema) params.getParentObject() : (params.isGlobalSearch() || !(executionContext instanceof GenericExecutionContext) ? null : ((GenericExecutionContext) executionContext).getDefaultSchema());

        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, "Find objects by mask")) {
            if (objectTypesList.contains(HANAObjectType.TABLE) || objectTypesList.contains(HANAObjectType.VIEW) || objectTypesList.contains(HANAObjectType.PROCEDURE) ||
                    objectTypesList.contains(HANAObjectType.SYNONYM)) {
                for (DBSObjectType objectType : params.getObjectTypes()) {
                    if (objectTypeClause.length() > 0) objectTypeClause.append(",");
                    objectTypeClause.append("'").append(objectType.getTypeName()).append("'");
                }
                if (objectTypeClause.length() == 0) {
                    objectTypeClause.append("'TABLE', 'VIEW', 'SYNONYM', 'PROCEDURE'");
                }
                searchNotColumnObjects(session, parentSchema, params, result, objectTypeClause.toString());
            }
            if (objectTypesList.contains(RelationalObjectType.TYPE_TABLE_COLUMN)) {
                findTableColumnsByMask(session, parentSchema, params, result);
            }
            if (objectTypesList.contains(RelationalObjectType.TYPE_VIEW_COLUMN)) {
                findViewColumnsByMask(session, parentSchema, params, result);
            }
        } catch (SQLException ex) {
            throw new DBException(ex, dataSource);
        }

        return result;
    }

    private void searchNotColumnObjects(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                        List<DBSObjectReference> result, String objectTypeClause) throws SQLException {
        String stmt = "SELECT SCHEMA_NAME, OBJECT_NAME, OBJECT_TYPE FROM SYS.OBJECTS WHERE";
        stmt += params.isCaseSensitive() ? " OBJECT_NAME LIKE ?" : " UPPER(OBJECT_NAME) LIKE ?";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " AND OBJECT_TYPE IN (" + objectTypeClause + ")";
        stmt += " ORDER BY SCHEMA_NAME, OBJECT_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String schemaName = dbResult.getString(1);
                    final String objectName = dbResult.getString(2);
                    final String objectTypeName = dbResult.getString(3);
                    final HANAObjectType objectType = HANAObjectType.valueOf(objectTypeName);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null) {
                        log.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                        continue;
                    }

                    result.add(
                            new AbstractObjectReference(objectName, schema, null, objectType.getTypeClass(), objectType) {
                                @Override
                                public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                                    DBSObject object = objectType.findObject(session.getProgressMonitor(), schema, objectName);
                                    if (object == null) {
                                        throw new DBException(objectTypeName + " '" + objectName + "' not found in schema '" + schema.getName() + "'");
                                    }
                                    return object;
                                }

                                @NotNull
                                @Override
                                public String getFullyQualifiedName(DBPEvaluationContext context) {
                                    if (objectType == HANAObjectType.SYNONYM && "PUBLIC".equals(schemaName)) {
                                        return DBUtils.getQuotedIdentifier(dataSource, objectName);
                                    }
                                    return super.getFullyQualifiedName(context);
                                }
                            });

                }
            }
        }
    }

    private void findTablesByMask(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                  List<DBSObjectReference> result) throws SQLException {
        String stmt = "SELECT SCHEMA_NAME, TABLE_NAME, COMMENTS FROM SYS.TABLES WHERE";
        stmt += params.isCaseSensitive() ? " TABLE_NAME LIKE ?" : " UPPER(TABLE_NAME) LIKE ?";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY SCHEMA_NAME, TABLE_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
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

    private void findViewsByMask(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                 List<DBSObjectReference> result) throws SQLException {
        String stmt = "SELECT SCHEMA_NAME, VIEW_NAME, COMMENTS FROM SYS.VIEWS WHERE";
        stmt += params.isCaseSensitive() ? " VIEW_NAME LIKE ?" : " UPPER(VIEW_NAME) LIKE ?";
        if (parentSchema != null)stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY SCHEMA_NAME, VIEW_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
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

    private void findProceduresByMask(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                      List<DBSObjectReference> result) throws SQLException {
        String stmt = "SELECT SCHEMA_NAME, PROCEDURE_NAME FROM SYS.PROCEDURES WHERE";
        stmt += params.isCaseSensitive() ? " PROCEDURE_NAME LIKE ?" : " UPPER(PROCEDURE_NAME) LIKE ?";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY SCHEMA_NAME, PROCEDURE_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
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

    private void findTableColumnsByMask(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                        List<DBSObjectReference> result) throws SQLException {
        String stmt = "SELECT SCHEMA_NAME, TABLE_NAME, COLUMN_NAME, COMMENTS FROM SYS.TABLE_COLUMNS WHERE";
        stmt += params.isCaseSensitive() ? " COLUMN_NAME LIKE ?" : " UPPER(COLUMN_NAME) LIKE ?";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY SCHEMA_NAME, TABLE_NAME, COLUMN_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
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

    private void findViewColumnsByMask(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                       List<DBSObjectReference> result) throws SQLException {
        String stmt = "SELECT SCHEMA_NAME, VIEW_NAME, COLUMN_NAME, COMMENTS FROM SYS.VIEW_COLUMNS WHERE";
        stmt += params.isCaseSensitive() ? " COLUMN_NAME LIKE ?" : " UPPER(COLUMN_NAME) LIKE ?";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY SCHEMA_NAME, VIEW_NAME, COLUMN_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
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
