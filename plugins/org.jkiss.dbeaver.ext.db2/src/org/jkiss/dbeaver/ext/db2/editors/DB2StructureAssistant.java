/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.editors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.model.*;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableType;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.*;

/**
 * DB2 Structure Assistant
 * 
 * @author Denis Forveille
 */
public class DB2StructureAssistant implements DBSStructureAssistant<DB2ExecutionContext> {
    private static final Log LOG = Log.getLog(DB2StructureAssistant.class);

    private static final String WITH_UR = "WITH UR";
    private static final String LF = "\n";

    // TODO DF: Work in progess

    private static final DBSObjectType[] SUPP_OBJ_TYPES = { DB2ObjectType.ALIAS, DB2ObjectType.TABLE, DB2ObjectType.VIEW,
        DB2ObjectType.MQT, DB2ObjectType.NICKNAME, DB2ObjectType.COLUMN, DB2ObjectType.ROUTINE };

    private static final DBSObjectType[] HYPER_LINKS_TYPES = { DB2ObjectType.ALIAS, DB2ObjectType.TABLE, DB2ObjectType.VIEW,
        DB2ObjectType.MQT, DB2ObjectType.NICKNAME, DB2ObjectType.ROUTINE, };

    private static final DBSObjectType[] AUTOC_OBJ_TYPES = { DB2ObjectType.ALIAS, DB2ObjectType.TABLE, DB2ObjectType.VIEW,
        DB2ObjectType.MQT, DB2ObjectType.NICKNAME, DB2ObjectType.ROUTINE, };

    private final DB2DataSource dataSource;

    // -----------------
    // Constructors
    // -----------------
    public DB2StructureAssistant(DB2DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    // -----------------
    // Method Interface
    // -----------------

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return SUPP_OBJ_TYPES;
    }

    @Override
    public DBSObjectType[] getSearchObjectTypes() {
        return getSupportedObjectTypes();
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return HYPER_LINKS_TYPES;
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return AUTOC_OBJ_TYPES;
    }

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(@NotNull DBRProgressMonitor monitor, @NotNull DB2ExecutionContext executionContext,
                                                      @NotNull ObjectsSearchParams params) throws DBException {
        List<DB2ObjectType> db2ObjectTypes = new ArrayList<>(params.getObjectTypes().length);
        for (DBSObjectType dbsObjectType : params.getObjectTypes()) {
            db2ObjectTypes.add((DB2ObjectType) dbsObjectType);
        }

        DB2Schema schema = params.getParentObject() instanceof DB2Schema ? (DB2Schema) params.getParentObject() : null;
        if (schema == null && !params.isGlobalSearch()) {
            schema = executionContext.getContextDefaults().getDefaultSchema();
        }

        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, "Find objects by name")) {
            return searchAllObjects(session, schema, db2ObjectTypes, params);
        } catch (SQLException ex) {
            throw new DBException(ex, dataSource);
        }
    }

    // -----------------
    // Helpers
    // -----------------

    private List<DBSObjectReference> searchAllObjects(final JDBCSession session, @Nullable DBPNamedObject schema, List<DB2ObjectType> db2ObjectTypes,
                                                      @NotNull ObjectsSearchParams params) throws SQLException, DBException {
        List<DBSObjectReference> objects = new ArrayList<>();

        // Tables, Alias, Views, Nicknames, MQT
        if (db2ObjectTypes.stream().anyMatch(DB2StructureAssistant::isTable)) {
            searchTables(session, schema, db2ObjectTypes, objects, params);
            if (objects.size() >= params.getMaxResults()) {
                return objects;
            }
        }

        // Columns
        if (db2ObjectTypes.contains(DB2ObjectType.COLUMN)) {
            searchColumns(session, schema, params, objects);
            if (objects.size() >= params.getMaxResults()) {
                return objects;
            }
        }

        // Routines
        if (db2ObjectTypes.contains(DB2ObjectType.ROUTINE)) {
            searchRoutines(session, schema, params, objects);
        }

        return objects;
    }

    private static boolean isTable(@Nullable DBSObjectType objectType) {
        return objectType == DB2ObjectType.ALIAS
            || objectType == DB2ObjectType.TABLE
            || objectType == DB2ObjectType.NICKNAME
            || objectType == DB2ObjectType.VIEW
            || objectType == DB2ObjectType.MQT;
    }

    private void searchTables(@NotNull JDBCSession session, @Nullable DBPNamedObject schema, @NotNull Iterable<DB2ObjectType> db2ObjectTypes,
                              @NotNull Collection<? super DBSObjectReference> objects, @NotNull ObjectsSearchParams params)
                                throws SQLException, DBException {
        String typeClause = getTypeClause(db2ObjectTypes);
        StringBuilder sql = new StringBuilder("SELECT TABSCHEMA,TABNAME, TYPE FROM SYSCAT.TABLES WHERE");
        StringJoiner whereClause = new StringJoiner(" AND ", " ", " ");
        whereClause.add(typeClause);
        if (schema != null) {
            whereClause.add("TABSCHEMA = ?");
        }
        StringJoiner maskClause = new StringJoiner(" OR ", "(", ")");
        maskClause.add("TABNAME LIKE ?");
        if (params.isSearchInComments()) {
            maskClause.add("REMARKS LIKE ?");
        }
        whereClause.add(maskClause.toString());
        sql.append(whereClause);
        if (params.isSearchInDefinitions()) {
            sql.append("\nUNION ALL\nSELECT\n\tt.TABSCHEMA,\n\tt.TABNAME,\n\tt.\"TYPE\"\nFROM\n\t\"SYSIBM\".SYSVIEWS v\n")
                    .append("INNER JOIN SYSCAT.TABLES t ON\n\tv.NAME = t.TABNAME\nWHERE\n\tv.TEXT LIKE ?\n\tAND ").append(typeClause);
            if (schema != null) {
                sql.append("\n\tAND TABSCHEMA = ?");
            }
        }
        sql.append(LF).append(WITH_UR);

        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (schema != null) {
                dbStat.setString(paramIdx, schema.getName());
                paramIdx++;
            }
            dbStat.setString(paramIdx, params.getMask());
            paramIdx++;
            if (params.isSearchInComments()) {
                dbStat.setString(paramIdx, params.getMask());
                paramIdx++;
            }
            if (params.isSearchInDefinitions()) {
                dbStat.setString(paramIdx, params.getMask());
                paramIdx++;
                if (schema != null) {
                    dbStat.setString(paramIdx, schema.getName());
                }
            }
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);

            String schemaName;
            String objectName;
            DB2Schema db2Schema;
            DB2TableType tableType;
            DB2ObjectType objectType;

            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                DBRProgressMonitor monitor = session.getProgressMonitor();
                while (dbResult.next() && !monitor.isCanceled() && objects.size() < params.getMaxResults()) {
                    schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
                    objectName = JDBCUtils.safeGetString(dbResult, "TABNAME");
                    tableType = CommonUtils.valueOf(DB2TableType.class, JDBCUtils.safeGetString(dbResult, "TYPE"));

                    db2Schema = dataSource.getSchema(monitor, schemaName);
                    if (db2Schema == null) {
                        LOG.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                        continue;
                    }

                    objectType = tableType.getDb2ObjectType();
                    objects.add(new DB2ObjectReference(objectName, db2Schema, objectType));
                }
            }
        }
    }

    @NotNull
    private static String getTypeClause(@NotNull Iterable<DB2ObjectType> types) {
        StringJoiner joiner = new StringJoiner(", ", "TYPE IN (", ")");
        for (DB2ObjectType type: types) {
            if (type == DB2ObjectType.ALIAS) {
                addTypeVariables(joiner, DB2TableType.A);
            } else if (type == DB2ObjectType.TABLE) {
                addTypeVariables(joiner, DB2TableType.G, DB2TableType.H, DB2TableType.L, DB2TableType.T, DB2TableType.U);
            } else if (type == DB2ObjectType.VIEW) {
                addTypeVariables(joiner, DB2TableType.V, DB2TableType.W);
            } else if (type == DB2ObjectType.MQT) {
                addTypeVariables(joiner, DB2TableType.S);
            } else if (type == DB2ObjectType.NICKNAME) {
                addTypeVariables(joiner, DB2TableType.N);
            }
        }
        return joiner.toString();
    }

    private static void addTypeVariables(@NotNull StringJoiner joiner, @NotNull DB2TableType... types) {
        for (DB2TableType type: types) {
            joiner.add("'" + type.name().charAt(0) + "'");
        }
    }

    private void searchRoutines(@NotNull JDBCSession session, @Nullable DBPNamedObject schema, @NotNull ObjectsSearchParams params,
                                @NotNull Collection<? super DBSObjectReference> objects) throws SQLException, DBException {
        StringBuilder sql = new StringBuilder("SELECT ROUTINESCHEMA, ROUTINENAME" + LF + "FROM SYSCAT.ROUTINES" + LF +
            SQLConstants.KEYWORD_WHERE + LF + " ");
        StringJoiner clause = new StringJoiner(" OR ", "(", ")");
        clause.add("ROUTINENAME LIKE ?");
        if (params.isSearchInDefinitions()) {
            clause.add("TEXT LIKE ?");
        }
        if (params.isSearchInComments()) {
            clause.add("REMARKS LIKE ?");
        }
        sql.append(clause);
        if (schema != null) {
            sql.append(" AND ROUTINESCHEMA = ?");
        }
        sql.append(LF + WITH_UR);

        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
            dbStat.setString(1, params.getMask());
            int paramIdx = 2;
            if (params.isSearchInDefinitions()) {
                dbStat.setString(paramIdx, params.getMask());
                paramIdx++;
            }
            if (params.isSearchInComments()) {
                dbStat.setString(paramIdx, params.getMask());
                paramIdx++;
            }
            if (schema != null) {
                dbStat.setString(paramIdx, schema.getName());
            }
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);

            String schemaName;
            String objectName;
            DB2Schema db2Schema;

            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                DBRProgressMonitor monitor = session.getProgressMonitor();
                while (dbResult.next() && !monitor.isCanceled() && objects.size() < params.getMaxResults()) {
                    schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "ROUTINESCHEMA");
                    objectName = JDBCUtils.safeGetString(dbResult, "ROUTINENAME");

                    db2Schema = dataSource.getSchema(monitor, schemaName);
                    if (db2Schema == null) {
                        LOG.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                        continue;
                    }

                    objects.add(new DB2ObjectReference(objectName, db2Schema, DB2ObjectType.ROUTINE));
                }
            }
        }
    }

    private void searchColumns(@NotNull JDBCSession session, @Nullable DBPNamedObject schema, @NotNull ObjectsSearchParams params,
                               @NotNull Collection<? super DBSObjectReference> objects) throws SQLException, DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TABSCHEMA, TABNAME, COLNAME FROM SYSCAT.COLUMNS WHERE ");
        StringJoiner clause = new StringJoiner(" OR ", "(", ")");
        clause.add("COLNAME LIKE ?");
        if (params.isSearchInComments()) {
            clause.add("REMARKS LIKE ?");
        }
        sql.append(clause);
        if (schema != null) {
            sql.append(" AND TABSCHEMA = ?");
        }
        sql.append(" " + WITH_UR);

        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
            dbStat.setString(1, params.getMask());
            int paramIdx = 2;
            if (params.isSearchInComments()) {
                dbStat.setString(paramIdx, params.getMask());
                paramIdx++;
            }
            if (schema != null) {
                dbStat.setString(paramIdx, schema.getName());
            }
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);

            String tableSchemaName;
            String tableOrViewName;
            String columnName;
            DB2Schema db2Schema;
            DB2Table db2Table;
            DB2View db2View;

            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                DBRProgressMonitor monitor = session.getProgressMonitor();
                while (dbResult.next() && !monitor.isCanceled() && objects.size() < params.getMaxResults()) {
                    tableSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
                    tableOrViewName = JDBCUtils.safeGetString(dbResult, "TABNAME");
                    columnName = JDBCUtils.safeGetString(dbResult, "COLNAME");

                    db2Schema = dataSource.getSchema(monitor, tableSchemaName);
                    if (db2Schema == null) {
                        LOG.debug("Schema '" + tableSchemaName + "' not found. Probably was filtered");
                        continue;
                    }
                    // Try with table, then view
                    db2Table = db2Schema.getTable(monitor, tableOrViewName);
                    if (db2Table != null) {
                        objects.add(new DB2ObjectReference(columnName, db2Table, DB2ObjectType.COLUMN));
                    } else {
                        db2View = db2Schema.getView(monitor, tableOrViewName);
                        if (db2View != null) {
                            objects.add(new DB2ObjectReference(columnName, db2View, DB2ObjectType.COLUMN));
                        }
                    }

                }
            }
        }
    }

    private class DB2ObjectReference extends AbstractObjectReference {

        private DB2ObjectReference(String objectName, DB2Schema db2Schema, DB2ObjectType objectType)
        {
            super(objectName, db2Schema, null, DB2Schema.class, objectType);
        }

        private DB2ObjectReference(String objectName, DB2Table db2Table, DB2ObjectType objectType)
        {
            super(objectName, db2Table, null, DB2Table.class, objectType);
        }

        private DB2ObjectReference(String objectName, DB2View db2View, DB2ObjectType objectType)
        {
            super(objectName, db2View, null, DB2View.class, objectType);
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
        {

            DB2ObjectType db2ObjectType = (DB2ObjectType) getObjectType();

            if (getContainer() instanceof DB2Schema) {
                DB2Schema db2Schema = (DB2Schema) getContainer();

                DBSObject object = db2ObjectType.findObject(monitor, db2Schema, getName());
                if (object == null) {
                    throw new DBException(db2ObjectType + " '" + getName() + "' not found in schema '" + db2Schema.getName() + "'");
                }
                return object;
            }
            if (getContainer() instanceof DB2Table) {
                DB2Table db2Table = (DB2Table) getContainer();

                DBSObject object = db2ObjectType.findObject(monitor, db2Table, getName());
                if (object == null) {
                    throw new DBException(db2ObjectType + " '" + getName() + "' not found in table '" + db2Table.getName() + "'");
                }
                return object;
            }
            if (getContainer() instanceof DB2View) {
                DB2View db2View = (DB2View) getContainer();

                DBSObject object = db2ObjectType.findObject(monitor, db2View, getName());
                if (object == null) {
                    throw new DBException(db2ObjectType + " '" + getName() + "' not found in view '" + db2View.getName() + "'");
                }
                return object;
            }
            return null;
        }

    }

    @Override
    public boolean supportsSearchInDefinitionsFor(@NotNull DBSObjectType objectType) {
        return objectType == DB2ObjectType.ROUTINE || isTable(objectType);
    }

    @Override
    public boolean supportsSearchInCommentsFor(@NotNull DBSObjectType type) {
        return type == DB2ObjectType.ROUTINE
            || type == DB2ObjectType.COLUMN
            || (isTable(type) && type != DB2ObjectType.ALIAS);
    }
}
