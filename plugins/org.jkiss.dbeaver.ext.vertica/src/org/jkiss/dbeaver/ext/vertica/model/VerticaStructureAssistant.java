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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
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
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class VerticaStructureAssistant extends JDBCStructureAssistant<JDBCExecutionContext> {

    private final VerticaDataSource dataSource;

    VerticaStructureAssistant(VerticaDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected JDBCDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes() {
        return new DBSObjectType[]{
            RelationalObjectType.TYPE_TABLE,
            VerticaObjectType.VIEW,
            RelationalObjectType.TYPE_CONSTRAINT,
            RelationalObjectType.TYPE_TABLE_COLUMN,
            RelationalObjectType.TYPE_VIEW_COLUMN,
            VerticaObjectType.SEQUENCE,
            VerticaObjectType.PROJECTION,
            VerticaObjectType.NODE
        };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes() {
        return new DBSObjectType[]{
            RelationalObjectType.TYPE_TABLE,
            VerticaObjectType.VIEW
        };
    }

    @Override
    protected void findObjectsByMask(@NotNull JDBCExecutionContext executionContext, @NotNull JDBCSession session, @NotNull DBSObjectType objectType, @NotNull ObjectsSearchParams params, @NotNull List<DBSObjectReference> references) throws DBException, SQLException {
        GenericSchema parentSchema = params.isGlobalSearch() ? null : params.getParentObject() instanceof GenericSchema ? (GenericSchema) params.getParentObject() : null;

        if (objectType == RelationalObjectType.TYPE_TABLE || objectType == RelationalObjectType.TYPE_VIEW) {
            findTablesAndViewsByMask(session, parentSchema, params, references);
        }
        if (objectType == VerticaObjectType.SEQUENCE) {
            findSequencesByMask(session, parentSchema, params, references);
        }
        if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
            findConstraintsByMask(session, parentSchema, params, references);
        }
        if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN) {
            findTableColumnsByMask(session, parentSchema, params, references);
        }
        if (objectType == RelationalObjectType.TYPE_VIEW_COLUMN) {
            findViewColumnsByMask(session, parentSchema, params, references);
        }
        if (objectType == VerticaObjectType.PROJECTION) {
            findProjectionsByMask(session, parentSchema, params, references);
        }
        if (objectType == VerticaObjectType.NODE) {
            findNodesByMask(session, params, references);
        }
    }

    private void findTablesAndViewsByMask(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                          List<DBSObjectReference> result) throws SQLException {
        List<DBSObjectType> objectTypesList = Arrays.asList(params.getObjectTypes());
        StringBuilder objectTypeClause = new StringBuilder(100);

        boolean addTables = objectTypesList.contains(RelationalObjectType.TYPE_TABLE);
        if (addTables) {
            objectTypeClause.append("'TABLE','SYSTEM TABLE'");
        }
        if (objectTypesList.contains(RelationalObjectType.TYPE_VIEW)) {
            if (addTables) objectTypeClause.append(",");
            objectTypeClause.append("'VIEW'");
        }
        boolean searchInComments = params.isSearchInComments();

        String sql = "SELECT schema_name, table_name, CASE WHEN table_type = 'SYSTEM TABLE' THEN 'SYSTEM_TABLE' ELSE table_type END, remarks" +
            "\nFROM v_catalog.all_tables WHERE " + (searchInComments ? "(" : "") + "TABLE_NAME";
        sql += params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?";
        if (searchInComments) {
            sql += " OR remarks" + (params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?") + ")";
        }
        if (parentSchema != null) sql += " AND SCHEMA_NAME = ?";
        sql += " AND table_type IN (" + objectTypeClause.toString() + ")";
        sql += "\nORDER BY SCHEMA_NAME, TABLE_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            dbStat.setString(1, params.getMask());
            if (searchInComments) {
                dbStat.setString(2, params.getMask());
            }
            if (parentSchema != null) {
                dbStat.setString(searchInComments ? 3 : 2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String schemaName = dbResult.getString(1);
                    final String objectName = dbResult.getString(2);
                    final String tableType = dbResult.getString(3);
                    final String description = dbResult.getString(4);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null) {
                        continue; // filtered
                    }
                    final VerticaObjectType objectType = VerticaObjectType.valueOf(tableType);
                    result.add(new AbstractObjectReference(objectName, schema, description, objectType.getClass(), objectType) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            DBSObject object = objectType.findObject(monitor, schema, objectName);
                            if (object == null) {
                                throw new DBException(tableType + " '" + objectName + "' not found in schema '" + schema.getName() + "'");
                            }
                            return object;
                        }
                    });
                }
            }
        }
    }

    private void findSequencesByMask(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                     List<DBSObjectReference> result) throws SQLException {
        boolean searchInComments = params.isSearchInComments();

        String sql = "SELECT s.sequence_schema, s.sequence_name, c.\"comment\" FROM v_catalog.sequences s\n" +
            "LEFT JOIN v_catalog.comments c ON s.sequence_id = c.object_id WHERE " + (searchInComments ? "(" : "") + " s.sequence_name";
        sql += params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?";
        if (searchInComments) {
            sql += " OR c.\"comment\"" + (params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?") + ")";
        }
        if (parentSchema != null) sql += " AND s.sequence_schema = ?";
        sql += "\nORDER BY s.sequence_schema, s.sequence_name LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            dbStat.setString(1, params.getMask());
            if (searchInComments) {
                dbStat.setString(2, params.getMask());
            }
            if (parentSchema != null) {
                dbStat.setString(searchInComments ? 3 : 2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String schemaName = dbResult.getString(1);
                    final String objectName = dbResult.getString(2);
                    final String description = dbResult.getString(3);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null) {
                        continue; // filtered
                    }
                    result.add(new AbstractObjectReference(objectName, schema, description, VerticaSequence.class, VerticaObjectType.SEQUENCE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericSequence object = ((GenericObjectContainer) getContainer()).getSequence(monitor, objectName);
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

    private void findConstraintsByMask(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                       List<DBSObjectReference> result) throws SQLException {
        boolean searchInComments = params.isSearchInComments();
        String sql = "SELECT s.schema_name, al.table_name, tc.constraint_name, tc.constraint_type, c.\"comment\" FROM v_catalog.table_constraints tc\n" +
            "LEFT JOIN v_catalog.all_tables al ON al.table_id = tc.table_id\n" +
            "LEFT JOIN v_catalog.schemata s ON tc.constraint_schema_id = s.schema_id\n" +
            "LEFT JOIN v_catalog.comments c ON tc.constraint_id = c.object_id WHERE " + (searchInComments ? "(" : "") + "tc.constraint_name";
        sql += params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?";
        if (searchInComments) {
            sql += " OR c.\"comment\"" + (params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?") + ")";
        }
        if (parentSchema != null) sql += " AND s.schema_name = ?";
        sql += "\nORDER BY s.schema_name, tc.constraint_name LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            dbStat.setString(1, params.getMask());
            if (searchInComments) {
                dbStat.setString(2, params.getMask());
            }
            if (parentSchema != null) {
                dbStat.setString(searchInComments ? 3 : 2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String schemaName = dbResult.getString(1);
                    final String tableName = dbResult.getString(2);
                    final String objectName = dbResult.getString(3);
                    final String constraintType = dbResult.getString(4);
                    final String description = dbResult.getString(5);
                    final boolean isFK = constraintType.equals("f");
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null) {
                        continue; // filtered
                    }
                    result.add(new AbstractObjectReference(objectName, schema, description, isFK ? GenericTableForeignKey.class : VerticaConstraint.class, RelationalObjectType.TYPE_CONSTRAINT) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericTableBase tableBase = ((GenericObjectContainer) getContainer()).getTable(monitor, tableName);
                            if (tableBase == null) {
                                throw new DBException("Can't find constraint table '" + tableName + "' in '"
                                    + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
                            }
                            DBSTableConstraint constraint;
                            if (isFK) {
                                constraint = tableBase.getAssociation(monitor, objectName);
                            } else {
                                constraint = tableBase.getConstraint(monitor, objectName);
                            }
                            if (constraint == null) {
                                throw new DBException("Can't find constraint '" + objectName + "' in '"
                                    + DBUtils.getFullyQualifiedName(dataSource, getContainer().getName(), tableBase.getName()) + "'");
                            }
                            return constraint;
                        }
                    });
                }
            }
        }
    }

    private void findProjectionsByMask(JDBCSession session, GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                       List<DBSObjectReference> result) throws SQLException {
        boolean searchInComments = params.isSearchInComments();
        String sql = "SELECT p.projection_schema, p.projection_name, c.\"comment\" FROM v_catalog.projections p\n" +
            "LEFT JOIN v_catalog.comments c ON p.projection_id = c.object_id\n" +
            "WHERE " + (searchInComments ? "(" : "") + "p.projection_name";
        sql += params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?";
        if (searchInComments) {
            sql += " OR c.\"comment\"" + (params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?") + ")";
        }
        if (parentSchema != null) sql += " AND p.projection_schema = ?";
        sql += "\nORDER BY p.projection_schema, p.projection_name LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            dbStat.setString(1, params.getMask());
            if (searchInComments) {
                dbStat.setString(2, params.getMask());
            }
            if (parentSchema != null) {
                dbStat.setString(searchInComments ? 3 : 2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String schemaName = dbResult.getString(1);
                    final String objectName = dbResult.getString(2);
                    final String description = dbResult.getString(3);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null) {
                        continue; // filtered
                    }
                    result.add(new AbstractObjectReference(objectName, schema, description, VerticaProjection.class, VerticaObjectType.PROJECTION) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            VerticaProjection object = ((VerticaSchema) getContainer()).getProjection(monitor, objectName);
                            if (object == null) {
                                throw new DBException("Can't find object '" + objectName + "' in '"
                                    + DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
                            }
                            return object;
                        }
                    });
                }
            }
        }
    }

    private void findNodesByMask(JDBCSession session, @NotNull ObjectsSearchParams params, List<DBSObjectReference> result) throws SQLException {
        boolean searchInComments = params.isSearchInComments();
        String sql = "SELECT n.node_name, c.\"comment\" FROM v_catalog.nodes n \n" +
            "LEFT JOIN v_catalog.comments c ON n.node_id = c.object_id\n" +
            "WHERE n.node_name";
        sql += params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?";
        if (searchInComments) {
            sql += " OR c.\"comment\"" + (params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?");
        }
        sql += "\nORDER BY n.node_name LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            dbStat.setString(1, params.getMask());
            if (searchInComments) {
                dbStat.setString(2, params.getMask());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String objectName = dbResult.getString(1);
                    final String description = dbResult.getString(2);
                    result.add(new AbstractObjectReference(objectName, dataSource, description, VerticaNode.class, VerticaObjectType.NODE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            VerticaNode object = ((VerticaDataSource) getContainer()).getClusterNode(monitor, objectName);
                            if (object == null) {
                                throw new DBException("Can't find object '" + objectName + "' in '"
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
        String stmt = "SELECT table_schema, table_name, column_name FROM v_catalog.columns WHERE column_name";
        stmt += params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY table_schema, table_name, column_name LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.getMask());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    String schemaName = dbResult.getString(1);
                    String tableName = dbResult.getString(2);
                    String columnName = dbResult.getString(3);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered

                    result.add(new AbstractObjectReference(columnName, schema, null, GenericTableColumn.class, RelationalObjectType.TYPE_TABLE_COLUMN) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericTableBase object = ((GenericObjectContainer) getContainer()).getTable(monitor, tableName);
                            if (object == null) {
                                throw new DBException("Can't find column table '" + tableName + "' in '"
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
        String stmt = "SELECT table_schema, table_name, column_name from v_catalog.view_columns WHERE column_name";
        stmt += params.isCaseSensitive() ? " LIKE ?" : " ILIKE ?";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY table_schema, table_name, column_name LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.getMask());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    String schemaName = dbResult.getString(1);
                    String tableName = dbResult.getString(2);
                    String columnName = dbResult.getString(3);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered

                    result.add(new AbstractObjectReference(columnName, schema, null, GenericTableColumn.class, RelationalObjectType.TYPE_VIEW_COLUMN) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericTableBase object = ((GenericObjectContainer) getContainer()).getTable(monitor, tableName);
                            if (object == null) {
                                throw new DBException("Can't find column view '" + tableName + "' in '"
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

    @Override
    public boolean supportsSearchInCommentsFor(@NotNull DBSObjectType objectType) {
        return objectType != RelationalObjectType.TYPE_TABLE_COLUMN && objectType != RelationalObjectType.TYPE_VIEW_COLUMN;
    }
}
