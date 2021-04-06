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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.*;

/**
 * OracleStructureAssistant
 */
public class OracleStructureAssistant implements DBSStructureAssistant<OracleExecutionContext> {
    private static final Log log = Log.getLog(OracleStructureAssistant.class);

    private final OracleDataSource dataSource;

    public OracleStructureAssistant(OracleDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            OracleObjectType.TABLE,
            OracleObjectType.PACKAGE,
            OracleObjectType.CONSTRAINT,
            OracleObjectType.FOREIGN_KEY,
            OracleObjectType.INDEX,
            OracleObjectType.PROCEDURE,
            OracleObjectType.SEQUENCE,
            OracleObjectType.TRIGGER,
            };
    }

    @Override
    public DBSObjectType[] getSearchObjectTypes() {
        return new DBSObjectType[] {
            OracleObjectType.TABLE,
            OracleObjectType.VIEW,
            OracleObjectType.MATERIALIZED_VIEW,
            OracleObjectType.PACKAGE,
            OracleObjectType.INDEX,
            OracleObjectType.PROCEDURE,
            OracleObjectType.SEQUENCE,
        };
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return new DBSObjectType[] {
            OracleObjectType.TABLE,
            OracleObjectType.PACKAGE,
            OracleObjectType.PROCEDURE,
        };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return new DBSObjectType[] {
            OracleObjectType.TABLE,
            OracleObjectType.PACKAGE,
            OracleObjectType.PROCEDURE,
            };
    }

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(@NotNull DBRProgressMonitor monitor, @NotNull OracleExecutionContext executionContext,
                                                      @NotNull ObjectsSearchParams params) throws DBException {
        OracleSchema schema = params.getParentObject() instanceof OracleSchema ? (OracleSchema) params.getParentObject() : null;

        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, "Find objects by name")) {
            List<DBSObjectReference> objects = new ArrayList<>();

            if (ArrayUtils.contains(params.getObjectTypes(), OracleObjectType.CONSTRAINT, OracleObjectType.FOREIGN_KEY)) {
                // Search constraints
                findConstraintsByMask(session, schema, params, objects);
                if (!containsOnlyConstraintOrFK(params.getObjectTypes())) {
                    searchAllObjects(session, schema, params, objects);
                }
            } else {
                // Search all objects
                searchAllObjects(session, schema, params, objects);
            }
            if (params.isSearchInComments()) {
                searchInTableComments(session, schema, params, objects);
            }

            // Sort objects. Put ones in the current schema first
            final OracleSchema activeSchema = executionContext.getContextDefaults().getDefaultSchema();
            objects.sort((o1, o2) -> {
                if (CommonUtils.equalObjects(o1.getContainer(), o2.getContainer())) {
                    return o1.getName().compareTo(o2.getName());
                }
                if (o1.getContainer() == null || o1.getContainer() == activeSchema) {
                    return -1;
                }
                if (o2.getContainer() == null || o2.getContainer() == activeSchema) {
                    return 1;
                }
                return o1.getContainer().getName().compareTo(o2.getContainer().getName());
            });

            return objects;
        }
        catch (SQLException ex) {
            throw new DBException(ex, dataSource);
        }
    }

    private void findConstraintsByMask(JDBCSession session, final OracleSchema schema, @NotNull ObjectsSearchParams params,
                                       List<DBSObjectReference> objects) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        List<DBSObjectType> objectTypesList = Arrays.asList(params.getObjectTypes());
        final boolean hasFK = objectTypesList.contains(OracleObjectType.FOREIGN_KEY);
        final boolean hasConstraints = objectTypesList.contains(OracleObjectType.CONSTRAINT);

        // Load tables
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + OracleUtils.getSysCatalogHint((OracleDataSource) session.getDataSource()) + " OWNER, TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, (OracleDataSource) session.getDataSource(), "CONSTRAINTS") + "\n" +
                "WHERE CONSTRAINT_NAME like ?" + (!hasFK ? " AND CONSTRAINT_TYPE<>'R'" : "") +
                (schema != null ? " AND OWNER=?" : ""))) {
            dbStat.setString(1, params.getMask().toUpperCase());
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next() && objects.size() < params.getMaxResults()) {
                    final String schemaName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_OWNER);
                    final String tableName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_NAME);
                    final String constrName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_NAME);
                    final String constrType = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_TYPE);
                    final DBSEntityConstraintType type = OracleTableConstraint.getConstraintType(constrType);
                    objects.add(new AbstractObjectReference(
                        constrName,
                        dataSource.getSchema(session.getProgressMonitor(), schemaName),
                        null,
                        type == DBSEntityConstraintType.FOREIGN_KEY ? OracleTableForeignKey.class : OracleTableConstraint.class,
                        type == DBSEntityConstraintType.FOREIGN_KEY ? OracleObjectType.FOREIGN_KEY : OracleObjectType.CONSTRAINT) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            OracleSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                            if (tableSchema == null) {
                                throw new DBException("Constraint schema '" + schemaName + "' not found");
                            }
                            OracleTable table = tableSchema.getTable(monitor, tableName);
                            if (table == null) {
                                throw new DBException("Constraint table '" + tableName + "' not found in catalog '" + tableSchema.getName() + "'");
                            }
                            DBSObject constraint = null;
                            if (hasFK && type == DBSEntityConstraintType.FOREIGN_KEY) {
                                constraint = table.getForeignKey(monitor, constrName);
                            }
                            if (hasConstraints && type != DBSEntityConstraintType.FOREIGN_KEY) {
                                constraint = table.getConstraint(monitor, constrName);
                            }
                            if (constraint == null) {
                                throw new DBException("Constraint '" + constrName + "' not found in table '" + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            }
                            return constraint;
                        }
                    });
                }
            }
        }
    }

    private void searchAllObjects(final JDBCSession session, final OracleSchema schema, @NotNull ObjectsSearchParams params,
                                  List<DBSObjectReference> objects) throws SQLException, DBException {
        final List<OracleObjectType> oracleObjectTypes = new ArrayList<>(params.getObjectTypes().length + 2);
        for (DBSObjectType objectType : params.getObjectTypes()) {
            if (objectType instanceof OracleObjectType) {
                oracleObjectTypes.add((OracleObjectType) objectType);
                if (objectType == OracleObjectType.PROCEDURE) {
                    oracleObjectTypes.add(OracleObjectType.FUNCTION);
                }
            } else if (DBSProcedure.class.isAssignableFrom(objectType.getTypeClass())) {
                oracleObjectTypes.add(OracleObjectType.FUNCTION);
                oracleObjectTypes.add(OracleObjectType.PROCEDURE);
            }
        }
        StringJoiner objectTypeClause = new StringJoiner(",");
        for (OracleObjectType objectType: oracleObjectTypes) {
            objectTypeClause.add("'" + objectType.getTypeName() + "'");
        }
        if (objectTypeClause.length() == 0) {
            return;
        }

        // Seek for objects (join with public synonyms)
        OracleDataSource dataSource = (OracleDataSource) session.getDataSource();
        String objectNameMask = params.getMask();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + OracleUtils.getSysCatalogHint(dataSource) + " DISTINCT OWNER,OBJECT_NAME,OBJECT_TYPE FROM " +
                "   (SELECT OWNER,OBJECT_NAME,OBJECT_TYPE FROM " + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), dataSource, "OBJECTS") + " WHERE " +
                    "OBJECT_TYPE IN (" + objectTypeClause + ") AND " + (!params.isCaseSensitive() ? "UPPER(OBJECT_NAME)" : "OBJECT_NAME") + " LIKE ? " +
                    (schema == null ? "" : " AND OWNER=?") +
                    "UNION ALL\n" +
                "SELECT " + OracleUtils.getSysCatalogHint(dataSource) + " O.OWNER,O.OBJECT_NAME,O.OBJECT_TYPE\n" +
                    "FROM " + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), dataSource, "SYNONYMS") + " S," +
                        OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), dataSource, "OBJECTS") + " O\n" +
                    "WHERE O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME AND O.OBJECT_TYPE<>'JAVA CLASS' AND " +
                    (!params.isCaseSensitive() ? "UPPER(S.SYNONYM_NAME)" : "S.SYNONYM_NAME") + "  LIKE ?)" +
                "\nORDER BY OBJECT_NAME")) {
            if (!params.isCaseSensitive()) {
                objectNameMask = objectNameMask.toUpperCase();
            }
            dbStat.setString(1, objectNameMask);
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            dbStat.setString(schema != null ? 3 : 2, objectNameMask);
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!session.getProgressMonitor().isCanceled() && objects.size() < params.getMaxResults() && dbResult.next()) {
                    final String schemaName = JDBCUtils.safeGetString(dbResult, "OWNER");
                    final String objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
                    final String objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
                    final OracleObjectType objectType = OracleObjectType.getByType(objectTypeName);
                    if (objectType != null && objectType.isBrowsable() && oracleObjectTypes.contains(objectType)) {
                        OracleSchema objectSchema = this.dataSource.getSchema(session.getProgressMonitor(), schemaName);
                        if (objectSchema == null) {
                            log.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                            continue;
                        }
                        addObjectReference(objects, objectName, objectSchema, objectType, objectTypeName, schemaName, session);
                    }
                }
            }
        }
    }

    private void addObjectReference(@NotNull Collection<DBSObjectReference> references, String objectName, @NotNull DBSObject objectSchema,
                                    @NotNull OracleObjectType objectType, String objectTypeName, String schemaName, @NotNull JDBCSession session) {
        references.add(
                new AbstractObjectReference(objectName, objectSchema, null, objectType.getTypeClass(), objectType) {
                    @Override
                    public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                        OracleSchema tableSchema = (OracleSchema) getContainer();
                        DBSObject object = objectType.findObject(session.getProgressMonitor(), tableSchema, objectName);
                        if (object == null) {
                            throw new DBException(objectTypeName + " '" + objectName + "' not found in schema '" + tableSchema.getName() + "'");
                        }
                        return object;
                    }

                    @NotNull
                    @Override
                    public String getFullyQualifiedName(DBPEvaluationContext context) {
                        if (objectType == OracleObjectType.SYNONYM && OracleConstants.USER_PUBLIC.equals(schemaName)) {
                            return DBUtils.getQuotedIdentifier(dataSource, objectName);
                        }
                        return super.getFullyQualifiedName(context);
                    }
                }
        );
    }

    private void searchInTableComments(@NotNull JDBCSession session, @Nullable OracleSchema schema, @NotNull ObjectsSearchParams params,
                                       @NotNull List<DBSObjectReference> objects) throws SQLException, DBException {
        if (objects.size() >= params.getMaxResults() || !ArrayUtils.contains(params.getObjectTypes(), OracleObjectType.TABLE)) {
            return;
        }
        StringBuilder sql = new StringBuilder("SELECT atc.OWNER, atc.TABLE_NAME, atc.TABLE_TYPE FROM ALL_TAB_COMMENTS atc WHERE ");
        String mask = params.getMask();
        if (params.isCaseSensitive()) {
            sql.append("atc.COMMENTS ");
        } else {
            sql.append("UPPER(atc.COMMENTS) ");
            mask = mask.toUpperCase();
        }
        sql.append("LIKE ? ");
        if (schema != null) {
            sql.append("AND atc.OWNER = ? ");
        }
        sql.append("ORDER BY atc.TABLE_NAME");

        try (JDBCPreparedStatement preparedStatement = session.prepareStatement(sql.toString())) {
            preparedStatement.setString(1, mask);
            if (schema != null) {
                preparedStatement.setString(2, schema.getName());
            }
            try (JDBCResultSet resultSet = preparedStatement.executeQuery()) {
                while (!session.getProgressMonitor().isCanceled() && objects.size() < params.getMaxResults() && resultSet.next()) {
                    String owner = JDBCUtils.safeGetString(resultSet, "OWNER");
                    String tableName = JDBCUtils.safeGetString(resultSet, "TABLE_NAME");
                    String tableType = JDBCUtils.safeGetString(resultSet, "TABLE_TYPE");
                    OracleObjectType oracleObjectType = OracleObjectType.getByType(tableType);
                    if (oracleObjectType == null || !oracleObjectType.isBrowsable() || tableName == null) {
                        continue;
                    }
                    OracleSchema objectSchema = dataSource.getSchema(session.getProgressMonitor(), owner);
                    if (objectSchema == null) {
                        log.debug("Schema '" + owner + "' not found. Probably was filtered");
                        continue;
                    }
                    addObjectReference(objects, tableName, objectSchema, oracleObjectType, tableType, owner, session);
                }
            }
        }
    }

    private boolean containsOnlyConstraintOrFK(DBSObjectType[] objectTypes) {
        for (DBSObjectType objectType : objectTypes) {
            if (!(objectType == OracleObjectType.CONSTRAINT || objectType == OracleObjectType.FOREIGN_KEY)) {
                return false;
            }
        }
        return true;
    }
}
