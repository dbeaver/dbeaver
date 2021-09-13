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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.ArrayUtils;

import java.sql.SQLException;
import java.util.*;

public class HANAStructureAssistant implements DBSStructureAssistant<JDBCExecutionContext> {
    private static final Log log = Log.getLog(HANAStructureAssistant.class);

    private final HANADataSource dataSource;

    public HANAStructureAssistant(HANADataSource dataSource) {
        this.dataSource = dataSource;
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
    public DBSObjectType[] getSearchObjectTypes() {
        return getSupportedObjectTypes();
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes() {
        return new DBSObjectType[] {RelationalObjectType.TYPE_TABLE,};
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
    public boolean supportsSearchInCommentsFor(@NotNull DBSObjectType type) {
        return type == HANAObjectType.TABLE
            || type == HANAObjectType.VIEW
            || type == RelationalObjectType.TYPE_TABLE_COLUMN
            || type == RelationalObjectType.TYPE_VIEW_COLUMN;
    }

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext executionContext,
                                                      @NotNull ObjectsSearchParams params) throws DBException {
        Collection<DBSObjectReference> references = new HashSet<>();
        GenericSchema parentSchema = null;
        if (params.getParentObject() instanceof GenericSchema) {
            parentSchema = (GenericSchema) params.getParentObject();
        } else if (!params.isGlobalSearch() && executionContext instanceof GenericExecutionContext) {
            parentSchema = ((GenericExecutionContext) executionContext).getDefaultSchema();
        }
        DBSObjectType[] objectTypes = params.getObjectTypes();

        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, ModelMessages.model_jdbc_find_objects_by_name)) {
            if (ArrayUtils.contains(objectTypes, HANAObjectType.TABLE, HANAObjectType.VIEW, HANAObjectType.PROCEDURE, HANAObjectType.SYNONYM)) {
                searchObjectsByNameInObjectsView(session, parentSchema, params, references);
            }
            if (params.isSearchInComments()) {
                if (ArrayUtils.contains(objectTypes, HANAObjectType.TABLE)) {
                    findTablesByComments(session, parentSchema, params, references);
                }
                if (ArrayUtils.contains(objectTypes, HANAObjectType.VIEW)) {
                    findViewsByComments(session, parentSchema, params, references);
                }
            }
            if (ArrayUtils.contains(objectTypes, RelationalObjectType.TYPE_TABLE_COLUMN)) {
                findTableColumns(session, parentSchema, params, references);
            }
            if (ArrayUtils.contains(objectTypes, RelationalObjectType.TYPE_VIEW_COLUMN)) {
                findViewColumns(session, parentSchema, params, references);
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }

        return new ArrayList<>(references);
    }

    private void searchObjectsByNameInObjectsView(@NotNull JDBCSession session, @Nullable GenericSchema parentSchema,
                                                  @NotNull ObjectsSearchParams params, @NotNull Collection<? super DBSObjectReference> result)
                                                    throws SQLException {
        if (result.size() >= params.getMaxResults()) {
            return;
        }

        StringJoiner objectTypeClause = new StringJoiner(", ");
        for (DBSObjectType objectType: params.getObjectTypes()) {
            objectTypeClause.add("'" + objectType.getTypeName() + "'");
        }

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
                while (!monitor.isCanceled() && dbResult.next() && result.size() < params.getMaxResults()) {
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

    private void findTablesByComments(@NotNull JDBCSession session, @Nullable GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                      @NotNull Collection<? super DBSObjectReference> result) throws SQLException {
        if (result.size() >= params.getMaxResults()) {
            return;
        }

        String stmt = "SELECT SCHEMA_NAME, TABLE_NAME FROM SYS.TABLES WHERE";
        stmt += params.isCaseSensitive() ? " COMMENTS LIKE ?" : " UPPER(COMMENTS) LIKE ?";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY SCHEMA_NAME, TABLE_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next() && result.size() < params.getMaxResults()) {
                    String schemaName = dbResult.getString(1);
                    String objectName = dbResult.getString(2);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered

                    DBSObjectType type = HANAObjectType.TABLE;
                    result.add(new AbstractObjectReference(objectName, schema, null, type.getTypeClass(), type) {
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

    private void findViewsByComments(@NotNull JDBCSession session, @Nullable GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                     @NotNull Collection<? super DBSObjectReference> result) throws SQLException {
        if (result.size() >= params.getMaxResults()) {
            return;
        }

        String stmt = "SELECT SCHEMA_NAME, VIEW_NAME FROM SYS.VIEWS WHERE";
        stmt += params.isCaseSensitive() ? " COMMENTS LIKE ?" : " UPPER(COMMENTS) LIKE ?";
        if (parentSchema != null)stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY SCHEMA_NAME, VIEW_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            dbStat.setString(1, params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase());
            if (parentSchema != null) {
                dbStat.setString(2, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next() && result.size() < params.getMaxResults()) {
                    String schemaName = dbResult.getString(1);
                    String objectName = dbResult.getString(2);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered

                    HANAObjectType type = HANAObjectType.VIEW;
                    result.add(new AbstractObjectReference(objectName, schema, null, type.getTypeClass(), type) {
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

    private void findTableColumns(@NotNull JDBCSession session, @Nullable GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                  @NotNull Collection<? super DBSObjectReference> result) throws SQLException {
        if (result.size() >= params.getMaxResults()) {
            return;
        }

        String stmt = "SELECT SCHEMA_NAME, TABLE_NAME, COLUMN_NAME FROM SYS.TABLE_COLUMNS WHERE";
        stmt += params.isCaseSensitive() ? " (COLUMN_NAME LIKE ?" : " (UPPER(COLUMN_NAME) LIKE ?";
        if (params.isSearchInComments()) {
            stmt += " OR ";
            if (params.isCaseSensitive()) {
                stmt += "COMMENTS ";
            } else {
                stmt += "UPPER(COMMENTS) ";
            }
            stmt += "LIKE ?";
        }
        stmt += ")";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY SCHEMA_NAME, TABLE_NAME, COLUMN_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            String mask = params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase();
            dbStat.setString(1, mask);
            int paramIdx = 2;
            if (params.isSearchInComments()) {
                dbStat.setString(paramIdx, mask);
                paramIdx++;
            }
            if (parentSchema != null) {
                dbStat.setString(paramIdx, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next() && result.size() < params.getMaxResults()) {
                    String schemaName = dbResult.getString(1);
                    String tableName = dbResult.getString(2);
                    String columnName = dbResult.getString(3);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered
                    result.add(
                        new AbstractObjectReference(tableName + "." + columnName, schema, null,
                            GenericTableColumn.class, RelationalObjectType.TYPE_TABLE_COLUMN) {

                            @Override
                            public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                                GenericTableBase object = schema.getTable(monitor, tableName);
                                if (object == null) {
                                    throw new DBException("Can't find object '" + tableName + "' in '"
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

    private void findViewColumns(@NotNull JDBCSession session, @Nullable GenericSchema parentSchema, @NotNull ObjectsSearchParams params,
                                 @NotNull Collection<? super DBSObjectReference> result) throws SQLException {
        if (result.size() >= params.getMaxResults()) {
            return;
        }

        String stmt = "SELECT SCHEMA_NAME, VIEW_NAME, COLUMN_NAME FROM SYS.VIEW_COLUMNS WHERE";
        stmt += params.isCaseSensitive() ? " (COLUMN_NAME LIKE ?" : " (UPPER(COLUMN_NAME) LIKE ?";
        if (params.isSearchInComments()) {
            stmt += " OR ";
            if (params.isCaseSensitive()) {
                stmt += "COMMENTS ";
            } else {
                stmt += "UPPER(COMMENTS) ";
            }
            stmt += "LIKE ?";
        }
        stmt += ")";
        if (parentSchema != null) stmt += " AND SCHEMA_NAME = ?";
        stmt += " ORDER BY SCHEMA_NAME, VIEW_NAME, COLUMN_NAME LIMIT " + (params.getMaxResults() - result.size());

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
            String mask = params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase();
            dbStat.setString(1, mask);
            int paramIdx = 2;
            if (params.isSearchInComments()) {
                dbStat.setString(paramIdx, mask);
                paramIdx++;
            }
            if (parentSchema != null) {
                dbStat.setString(paramIdx, parentSchema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next() && result.size() < params.getMaxResults()) {
                    String schemaName = dbResult.getString(1);
                    String viewName = dbResult.getString(2);
                    String columnName = dbResult.getString(3);
                    GenericSchema schema = parentSchema != null ? parentSchema : dataSource.getSchema(schemaName);
                    if (schema == null)
                        continue; // filtered
                    result.add(
                        new AbstractObjectReference(viewName + "." + columnName, schema, null,
                            GenericTableColumn.class, RelationalObjectType.TYPE_VIEW_COLUMN) {

                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            GenericTableBase object = schema.getTable(monitor, viewName);
                            if (object == null) {
                                throw new DBException("Can't find object '" + viewName + "' in '"
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
