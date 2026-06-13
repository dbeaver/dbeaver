/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ext.tibero.TiberoConstants;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class TiberoDataSource extends JDBCDataSource {

    private static final Log log = Log.getLog(TiberoDataSource.class);

    private final TiberoDataTypeCache dataTypeCache;
    final SchemaCache schemaCache = new SchemaCache();

    public TiberoDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container) throws DBException {
        super(monitor, container, new TiberoSQLDialect());
        this.dataTypeCache = new TiberoDataTypeCache(this);
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
        try {
            dataTypeCache.getAllObjects(monitor, this);
        } catch (Exception e) {
            log.debug("Can't fetch Tibero data types: " + e.getMessage());
        }
        if (CommonUtils.isEmpty(dataTypeCache.getCachedObjects())) {
            dataTypeCache.fillStandardTypes(this);
        }
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new TiberoExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCExecutionContext context,
        JDBCExecutionContext initFrom
    ) throws DBException {
        if (initFrom instanceof TiberoExecutionContext tiberoInitFrom) {
            ((TiberoExecutionContext) context).setCurrentSchema(monitor, tiberoInitFrom.getDefaultSchema());
        } else {
            ((TiberoExecutionContext) context).refreshDefaults(monitor, true);
        }
    }

    @NotNull
    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    @Nullable
    @Override
    public DBSDataType getLocalDataType(@Nullable String typeName) {
        return typeName == null ? null : dataTypeCache.getCachedObject(typeName);
    }

    @NotNull
    @Association
    public Collection<TiberoSchema> getSchemas(@NotNull DBRProgressMonitor monitor) throws DBException {
        return schemaCache.getAllObjects(monitor, this);
    }

    @Nullable
    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchemas(monitor);
    }

    @Nullable
    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return schemaCache.getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return TiberoSchema.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        schemaCache.getAllObjects(monitor, this);
        for (TiberoSchema schema : schemaCache.getCachedObjects()) {
            schema.cacheStructure(monitor, scope);
        }
    }

    void loadPackageProcedures(@NotNull DBRProgressMonitor monitor, @NotNull TiberoPackage procedurePackage) throws DBException {
        procedurePackage.getSchema().packageCache.loadPackageProcedures(monitor, procedurePackage);
    }

    boolean isObjectValid(@NotNull DBRProgressMonitor monitor, @NotNull TiberoPackage object, @NotNull String objectType) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Refresh Tibero " + objectType + " state")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT STATUS FROM ALL_OBJECTS WHERE OBJECT_TYPE = ? AND OWNER = ? AND OBJECT_NAME = ?"
            )) {
                dbStat.setString(1, objectType);
                dbStat.setString(2, object.getSchema().getName());
                dbStat.setString(3, object.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        return "VALID".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
                    }
                    return true;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    @NotNull
    List<TiberoProcedureParameter> loadPackageProcedureParameters(@NotNull DBRProgressMonitor monitor, @NotNull TiberoProcedurePackaged procedure) throws DBException {
        List<TiberoProcedureParameter> arguments = new java.util.ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, procedure, "Load Tibero package procedure arguments")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ARGUMENT_NAME, POSITION, DATA_LEVEL, IN_OUT, " +
                    "       DATA_TYPE, DATA_LENGTH, DATA_PRECISION, DATA_SCALE " +
                    "FROM ALL_ARGUMENTS " +
                    "WHERE OWNER = ? AND PACKAGE_NAME = ? AND OBJECT_NAME = ? " +
                    "ORDER BY POSITION, DATA_LEVEL"
            )) {
                dbStat.setString(1, procedure.getContainer().getSchema().getName());
                dbStat.setString(2, procedure.getContainer().getName());
                dbStat.setString(3, procedure.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        arguments.add(new TiberoProcedureParameter(procedure, dbResult));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, procedure.getDataSource());
        }
        return arguments;
    }

    public static class PackageCache extends JDBCObjectCache<TiberoSchema, TiberoPackage> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull TiberoSchema owner)
            throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OBJECT_NAME, " +
                    "       MIN(CASE WHEN STATUS = 'VALID' THEN 1 ELSE 0 END) AS ALL_VALID, " +
                    "       MIN(CREATED) AS CREATED, " +
                    "       MAX(LAST_DDL_TIME) AS LAST_DDL_TIME " +
                    "FROM ALL_OBJECTS " +
                    "WHERE OWNER = ? AND OBJECT_TYPE IN ('PACKAGE', 'PACKAGE BODY') " +
                    "GROUP BY OBJECT_NAME " +
                    "ORDER BY OBJECT_NAME"
            );
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoPackage fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull JDBCResultSet resultSet
        ) throws DBException {
            String objectName = JDBCUtils.safeGetStringTrimmed(resultSet, "OBJECT_NAME");
            if (CommonUtils.isEmpty(objectName)) {
                return null;
            }
            TiberoPackage pkg = new TiberoPackage(owner, resultSet);
            loadPackageProcedures(session, pkg);
            return pkg;
        }

        void loadPackageProcedures(@NotNull DBRProgressMonitor monitor, @NotNull TiberoPackage procedurePackage) throws DBException {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, procedurePackage, "Load Tibero package procedures")) {
                loadPackageProcedures(session, procedurePackage);
            }
        }

        void loadPackageProcedures(@NotNull JDBCSession session, @NotNull TiberoPackage procedurePackage) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT P.PROCEDURE_NAME, " +
                    "       CASE WHEN A.DATA_TYPE IS NULL THEN 'PROCEDURE' ELSE 'FUNCTION' END AS PROCEDURE_TYPE " +
                    "FROM ALL_PROCEDURES P " +
                    "LEFT JOIN ALL_ARGUMENTS A " +
                    "  ON A.OWNER = P.OWNER AND A.PACKAGE_NAME = P.OBJECT_NAME AND A.OBJECT_NAME = P.PROCEDURE_NAME " +
                    " AND A.ARGUMENT_NAME IS NULL AND A.DATA_LEVEL = 0 " +
                    "WHERE P.OWNER = ? AND P.OBJECT_NAME = ? AND P.PROCEDURE_NAME IS NOT NULL " +
                    "ORDER BY P.PROCEDURE_NAME"
            )) {
                dbStat.setString(1, procedurePackage.getSchema().getName());
                dbStat.setString(2, procedurePackage.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String procedureName = JDBCUtils.safeGetStringTrimmed(dbResult, "PROCEDURE_NAME");
                        String procedureType = JDBCUtils.safeGetStringTrimmed(dbResult, "PROCEDURE_TYPE");
                        if (CommonUtils.isEmpty(procedureName)) {
                            continue;
                        }
                        procedurePackage.addProcedure(new TiberoProcedurePackaged(
                            procedurePackage,
                            procedureName,
                            "FUNCTION".equalsIgnoreCase(procedureType) ?
                                org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType.FUNCTION :
                                org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType.PROCEDURE
                        ));
                    }
                }
            } catch (SQLException e) {
                log.debug("Can't load package procedures for " + procedurePackage.getFullyQualifiedName(org.jkiss.dbeaver.model.DBPEvaluationContext.DDL), e);
            }
            procedurePackage.orderProcedures();
        }
    }

    static int mapDataType(@Nullable String typeName) {
        if (typeName == null) {
            return Types.OTHER;
        }
        return switch (typeName.toUpperCase(Locale.ENGLISH)) {
            case "CHAR", "VARCHAR", "VARCHAR2", "NCHAR", "NVARCHAR2" -> Types.VARCHAR;
            case "NUMBER", "DECIMAL", "NUMERIC" -> Types.NUMERIC;
            case "FLOAT", "BINARY_FLOAT" -> Types.FLOAT;
            case "BINARY_DOUBLE" -> Types.DOUBLE;
            case "DATE", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE" -> Types.TIMESTAMP;
            case "CLOB", "NCLOB", "LONG" -> Types.CLOB;
            case "BLOB" -> Types.BLOB;
            case "RAW", "LONG RAW" -> Types.VARBINARY;
            default -> Types.OTHER;
        };
    }

    public static class SchemaCache extends JDBCObjectCache<TiberoDataSource, TiberoSchema> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull TiberoDataSource owner)
            throws SQLException {
            var cfg = owner.getContainer().getConnectionConfiguration();
            boolean showOnlyCurrentSchema = CommonUtils.toBoolean(
                cfg.getProviderProperty(TiberoConstants.PROP_SHOW_ONLY_ONE_SCHEMA));
            boolean hideEmptySchemas = CommonUtils.toBoolean(
                cfg.getProviderProperty(TiberoConstants.PROP_HIDE_EMPTY_SCHEMAS));
            String sql;
            if (showOnlyCurrentSchema) {
                sql = "SELECT USERNAME FROM ALL_USERS WHERE USERNAME = USER ORDER BY USERNAME";
            } else if (hideEmptySchemas) {
                sql = "SELECT USERNAME FROM ALL_USERS WHERE USERNAME IN "
                    + "(SELECT DISTINCT OWNER FROM ALL_OBJECTS) ORDER BY USERNAME";
            } else {
                sql = "SELECT USERNAME FROM ALL_USERS ORDER BY USERNAME";
            }
            return session.prepareStatement(sql);
        }

        @Nullable
        @Override
        protected TiberoSchema fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoDataSource owner,
            @NotNull JDBCResultSet resultSet
        ) {
            String schemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "USERNAME");
            return CommonUtils.isEmpty(schemaName) ? null : new TiberoSchema(owner, schemaName);
        }
    }

    public static class TableCache extends JDBCStructCache<TiberoSchema, TiberoTableBase, TiberoTableColumn> {
        TableCache() {
            super("TABLE_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull TiberoSchema owner)
            throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT O.OWNER, O.OBJECT_NAME AS TABLE_NAME, O.OBJECT_TYPE AS TABLE_TYPE, C.COMMENTS " +
                    "FROM ALL_OBJECTS O " +
                    "LEFT JOIN ALL_TAB_COMMENTS C ON C.OWNER = O.OWNER AND C.TABLE_NAME = O.OBJECT_NAME " +
                    "WHERE O.OWNER = ? AND O.OBJECT_TYPE IN ('TABLE', 'VIEW', 'MATERIALIZED VIEW') " +
                    "ORDER BY O.OBJECT_TYPE, O.OBJECT_NAME"
            );
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoTableBase fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull JDBCResultSet resultSet
        ) {
            String tableName = JDBCUtils.safeGetStringTrimmed(resultSet, "TABLE_NAME");
            String tableType = JDBCUtils.safeGetStringTrimmed(resultSet, "TABLE_TYPE");
            if (CommonUtils.isEmpty(tableName) || CommonUtils.isEmpty(tableType)) {
                return null;
            }
            String descProp = owner.getDataSource().getContainer()
                .getConnectionConfiguration()
                .getProviderProperty(TiberoConstants.PROP_SHOW_SCHEMA_TABLE_DESCRIPTION);
            boolean showTableDescription = descProp == null || CommonUtils.toBoolean(descProp);
            String comments = showTableDescription ? JDBCUtils.safeGetString(resultSet, "COMMENTS") : null;
            return switch (tableType) {
                case "VIEW", "MATERIALIZED VIEW" -> new TiberoView(owner, tableName, tableType, comments);
                default -> new TiberoTable(owner, tableName, tableType, comments);
            };
        }

        @NotNull
        @Override
        protected JDBCStatement prepareChildrenStatement(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @Nullable TiberoTableBase forObject
        ) throws SQLException {
            boolean readColumnComments = CommonUtils.toBoolean(owner.getDataSource().getContainer()
                .getConnectionConfiguration()
                .getProviderProperty(TiberoConstants.PROP_READ_COLUMN_COMMENTS));
            StringBuilder sql = new StringBuilder(
                "SELECT C.TABLE_NAME, C.COLUMN_NAME, C.DATA_TYPE, " +
                    "       CASE WHEN C.DATA_PRECISION IS NOT NULL THEN C.DATA_PRECISION " +
                    "            WHEN C.CHAR_LENGTH IS NOT NULL AND C.CHAR_LENGTH > 0 THEN C.CHAR_LENGTH " +
                    "            ELSE C.DATA_LENGTH END AS COLUMN_SIZE, " +
                    "       C.DATA_SCALE, C.COLUMN_ID, C.NULLABLE, C.DATA_DEFAULT, "
                    + (readColumnComments ? "CM.COMMENTS " : "NULL AS COMMENTS ") +
                    "FROM ALL_TAB_COLUMNS C "
                    + (readColumnComments
                        ? "LEFT JOIN ALL_COL_COMMENTS CM ON CM.OWNER = C.OWNER AND CM.TABLE_NAME = C.TABLE_NAME AND CM.COLUMN_NAME = C.COLUMN_NAME "
                        : "") +
                    "WHERE C.OWNER = ?"
            );
            if (forObject != null) {
                sql.append(" AND C.TABLE_NAME = ?");
            }
            sql.append(" ORDER BY C.TABLE_NAME, C.COLUMN_ID");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forObject != null) {
                dbStat.setString(2, forObject.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoTableColumn fetchChild(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull TiberoTableBase parent,
            @NotNull JDBCResultSet dbResult
        ) {
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
            String typeName = JDBCUtils.safeGetStringTrimmed(dbResult, "DATA_TYPE");
            if (CommonUtils.isEmpty(columnName) || CommonUtils.isEmpty(typeName)) {
                return null;
            }
            return new TiberoTableColumn(
                parent,
                columnName,
                typeName,
                mapDataType(typeName),
                JDBCUtils.safeGetInt(dbResult, "COLUMN_ID"),
                JDBCUtils.safeGetLong(dbResult, "COLUMN_SIZE"),
                JDBCUtils.safeGetInteger(dbResult, "DATA_SCALE"),
                JDBCUtils.safeGetInteger(dbResult, "COLUMN_SIZE"),
                "N".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "NULLABLE")),
                JDBCUtils.safeGetString(dbResult, "DATA_DEFAULT"),
                JDBCUtils.safeGetString(dbResult, "COMMENTS")
            );
        }
    }

    public static class IndexCache extends JDBCCompositeCache<TiberoSchema, TiberoTable, TiberoTableIndex, TiberoTableIndexColumn> {
        IndexCache(TableCache tableCache) {
            super(tableCache, TiberoTable.class, "TABLE_NAME", "INDEX_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @Nullable TiberoTable forParent
        ) throws SQLException {
            StringBuilder sql = new StringBuilder(
                "SELECT I.OWNER AS INDEX_OWNER, I.TABLE_NAME, I.INDEX_NAME, I.UNIQUENESS, I.INDEX_TYPE, " +
                    "       IC.COLUMN_NAME, IC.COLUMN_POSITION, IC.DESCEND " +
                    "FROM ALL_INDEXES I " +
                    "JOIN ALL_IND_COLUMNS IC " +
                    "  ON IC.INDEX_OWNER = I.OWNER AND IC.INDEX_NAME = I.INDEX_NAME " +
                    " AND IC.TABLE_OWNER = I.TABLE_OWNER AND IC.TABLE_NAME = I.TABLE_NAME " +
                    "WHERE I.TABLE_OWNER = ?"
            );
            if (forParent != null) {
                sql.append(" AND I.TABLE_NAME = ?");
            }
            sql.append(" ORDER BY I.TABLE_NAME, I.OWNER, I.INDEX_NAME, IC.COLUMN_POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forParent != null) {
                dbStat.setString(2, forParent.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoTableIndex fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull TiberoTable parent,
            @NotNull String childName,
            @NotNull JDBCResultSet resultSet
        ) {
            String indexOwner = JDBCUtils.safeGetStringTrimmed(resultSet, "INDEX_OWNER");
            boolean nonUnique = !"UNIQUE".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(resultSet, "UNIQUENESS"));
            String descProp = owner.getDataSource().getContainer()
                .getConnectionConfiguration()
                .getProviderProperty(TiberoConstants.PROP_SHOW_SCHEMA_INDEX_TABLE_DESCRIPTION);
            boolean tableNameDescription = descProp == null || CommonUtils.toBoolean(descProp);
            return new TiberoTableIndex(
                owner,
                parent,
                CommonUtils.isEmpty(indexOwner) ? owner.getName() : indexOwner,
                childName,
                nonUnique,
                DBSIndexType.OTHER,
                tableNameDescription
            );
        }

        @Nullable
        @Override
        protected TiberoTableIndexColumn[] fetchObjectRow(
            @NotNull JDBCSession session,
            @NotNull TiberoTable parent,
            @NotNull TiberoTableIndex forObject,
            @NotNull JDBCResultSet resultSet
        ) throws DBException {
            String columnName = JDBCUtils.safeGetStringTrimmed(resultSet, "COLUMN_NAME");
            if (CommonUtils.isEmpty(columnName)) {
                return null;
            }
            TiberoTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + forObject.getName() + "'");
                return null;
            }
            boolean ascending = !"DESC".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(resultSet, "DESCEND"));
            return new TiberoTableIndexColumn[] {
                new TiberoTableIndexColumn(forObject, tableColumn, JDBCUtils.safeGetInt(resultSet, "COLUMN_POSITION"), ascending)
            };
        }

        @Override
        protected void cacheChildren(
            @NotNull DBRProgressMonitor monitor,
            @NotNull TiberoTableIndex object,
            @NotNull List<TiberoTableIndexColumn> children
        ) {
            object.setColumns(children);
        }
    }

    public static class ConstraintCache extends JDBCCompositeCache<TiberoSchema, TiberoTable, TiberoTableConstraint, TiberoTableConstraintColumn> {
        ConstraintCache(TableCache tableCache) {
            super(tableCache, TiberoTable.class, "TABLE_NAME", "CONSTRAINT_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @Nullable TiberoTable forParent
        ) throws SQLException {
            StringBuilder sql = new StringBuilder(
                "SELECT C.TABLE_NAME, C.CONSTRAINT_NAME, C.CONSTRAINT_TYPE, C.SEARCH_CONDITION, " +
                    "       CC.COLUMN_NAME, CC.POSITION " +
                    "FROM ALL_CONSTRAINTS C " +
                    "LEFT JOIN ALL_CONS_COLUMNS CC " +
                    "  ON CC.OWNER = C.OWNER AND CC.CONSTRAINT_NAME = C.CONSTRAINT_NAME " +
                    "WHERE C.OWNER = ? AND C.CONSTRAINT_TYPE IN ('P','U')"
            );
            if (forParent != null) {
                sql.append(" AND C.TABLE_NAME = ?");
            }
            sql.append(" ORDER BY C.TABLE_NAME, C.CONSTRAINT_NAME, CC.POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forParent != null) {
                dbStat.setString(2, forParent.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoTableConstraint fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull TiberoTable parent,
            @NotNull String childName,
            @NotNull JDBCResultSet resultSet
        ) {
            DBSEntityConstraintType type = TiberoTableConstraint.getConstraintType(
                JDBCUtils.safeGetStringTrimmed(resultSet, "CONSTRAINT_TYPE"));
            String searchCondition = JDBCUtils.safeGetString(resultSet, "SEARCH_CONDITION");
            return new TiberoTableConstraint(parent, childName, type, searchCondition, true);
        }

        @Nullable
        @Override
        protected TiberoTableConstraintColumn[] fetchObjectRow(
            @NotNull JDBCSession session,
            @NotNull TiberoTable parent,
            @NotNull TiberoTableConstraint forObject,
            @NotNull JDBCResultSet resultSet
        ) throws DBException {
            String columnName = JDBCUtils.safeGetStringTrimmed(resultSet, "COLUMN_NAME");
            if (CommonUtils.isEmpty(columnName)) {
                return null;
            }
            TiberoTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for constraint '" + forObject.getName() + "'");
                return null;
            }
            int position = JDBCUtils.safeGetInt(resultSet, "POSITION");
            return new TiberoTableConstraintColumn[] {
                new TiberoTableConstraintColumn(forObject, tableColumn, position)
            };
        }

        @Override
        protected void cacheChildren(
            @NotNull DBRProgressMonitor monitor,
            @NotNull TiberoTableConstraint object,
            @NotNull List<TiberoTableConstraintColumn> children
        ) {
            object.setAttributeReferences(children);
        }
    }

    public static class ForeignKeyCache extends JDBCCompositeCache<TiberoSchema, TiberoTable, TiberoTableForeignKey, TiberoTableForeignKeyColumn> {
        ForeignKeyCache(TableCache tableCache) {
            super(tableCache, TiberoTable.class, "TABLE_NAME", "CONSTRAINT_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @Nullable TiberoTable forParent
        ) throws SQLException {
            StringBuilder sql = new StringBuilder(
                "SELECT FK.TABLE_NAME, FK.CONSTRAINT_NAME, FK.R_OWNER, FK.R_CONSTRAINT_NAME, FK.DELETE_RULE, " +
                    "       PK.TABLE_NAME AS R_TABLE_NAME, " +
                    "       FKC.COLUMN_NAME, FKC.POSITION " +
                    "FROM ALL_CONSTRAINTS FK " +
                    "JOIN ALL_CONS_COLUMNS FKC " +
                    "  ON FKC.OWNER = FK.OWNER AND FKC.CONSTRAINT_NAME = FK.CONSTRAINT_NAME " +
                    "LEFT JOIN ALL_CONSTRAINTS PK " +
                    "  ON PK.OWNER = FK.R_OWNER AND PK.CONSTRAINT_NAME = FK.R_CONSTRAINT_NAME " +
                    "WHERE FK.OWNER = ? AND FK.CONSTRAINT_TYPE = 'R'"
            );
            if (forParent != null) {
                sql.append(" AND FK.TABLE_NAME = ?");
            }
            sql.append(" ORDER BY FK.TABLE_NAME, FK.CONSTRAINT_NAME, FKC.POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forParent != null) {
                dbStat.setString(2, forParent.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoTableForeignKey fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull TiberoTable parent,
            @NotNull String childName,
            @NotNull JDBCResultSet resultSet
        ) throws DBException {
            String refOwner = JDBCUtils.safeGetStringTrimmed(resultSet, "R_OWNER");
            String refConstraintName = JDBCUtils.safeGetStringTrimmed(resultSet, "R_CONSTRAINT_NAME");
            String refTableName = JDBCUtils.safeGetStringTrimmed(resultSet, "R_TABLE_NAME");
            DBSForeignKeyModifyRule deleteRule = TiberoTableForeignKey.parseDeleteRule(
                JDBCUtils.safeGetStringTrimmed(resultSet, "DELETE_RULE"));
            TiberoTableConstraint referencedConstraint = resolveReferencedConstraint(
                session.getProgressMonitor(), owner, refOwner, refTableName, refConstraintName);
            return new TiberoTableForeignKey(parent, childName, referencedConstraint, deleteRule, true);
        }

        @Nullable
        @Override
        protected TiberoTableForeignKeyColumn[] fetchObjectRow(
            @NotNull JDBCSession session,
            @NotNull TiberoTable parent,
            @NotNull TiberoTableForeignKey forObject,
            @NotNull JDBCResultSet resultSet
        ) throws DBException {
            String columnName = JDBCUtils.safeGetStringTrimmed(resultSet, "COLUMN_NAME");
            if (CommonUtils.isEmpty(columnName)) {
                return null;
            }
            TiberoTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for foreign key '" + forObject.getName() + "'");
                return null;
            }
            int position = JDBCUtils.safeGetInt(resultSet, "POSITION");
            return new TiberoTableForeignKeyColumn[] {
                new TiberoTableForeignKeyColumn(forObject, tableColumn, position)
            };
        }

        @Override
        protected void cacheChildren(
            @NotNull DBRProgressMonitor monitor,
            @NotNull TiberoTableForeignKey object,
            @NotNull List<TiberoTableForeignKeyColumn> children
        ) {
            List<TiberoTableConstraintColumn> upcast = new java.util.ArrayList<>(children);
            object.setAttributeReferences(upcast);
        }

        @Nullable
        private TiberoTableConstraint resolveReferencedConstraint(
            @NotNull DBRProgressMonitor monitor,
            @NotNull TiberoSchema localSchema,
            @Nullable String refOwner,
            @Nullable String refTableName,
            @Nullable String refConstraintName
        ) throws DBException {
            if (CommonUtils.isEmpty(refOwner) || CommonUtils.isEmpty(refTableName) || CommonUtils.isEmpty(refConstraintName)) {
                return null;
            }
            TiberoDataSource ds = localSchema.getDataSource();
            TiberoSchema refSchema = ds.schemaCache.getObject(monitor, ds, refOwner);
            if (refSchema == null) {
                log.debug("Referenced schema '" + refOwner + "' not found for foreign key");
                return null;
            }
            TiberoTableBase refTableBase = refSchema.tableCache.getObject(monitor, refSchema, refTableName);
            if (refTableBase == null) {
                log.debug("Referenced table '" + refOwner + "." + refTableName + "' not found for foreign key");
                return null;
            }
            if (!(refTableBase instanceof TiberoTable refTable)) {
                log.debug("Referenced object '" + refOwner + "." + refTableName + "' is not a table");
                return null;
            }
            for (TiberoTableConstraint constraint : refSchema.constraintCache.getObjects(monitor, refSchema, refTable)) {
                if (refConstraintName.equals(constraint.getName())) {
                    return constraint;
                }
            }
            log.debug("Referenced constraint '" + refConstraintName + "' not found in table '" + refOwner + "." + refTableName + "'");
            return null;
        }
    }

    public static class SchemaTriggerCache extends JDBCObjectCache<TiberoSchema, TiberoSchemaTrigger> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull TiberoSchema owner)
            throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT T.OWNER, T.TRIGGER_NAME, T.TRIGGER_TYPE, T.TRIGGERING_EVENT, " +
                    "T.TABLE_OWNER, T.TABLE_NAME, T.REFERENCING_NAMES, T.WHEN_CLAUSE, T.STATUS, " +
                    "'SCHEMA' AS BASE_OBJECT_TYPE, NULL AS COLUMN_NAME, NULL AS DESCRIPTION, NULL AS ACTION_TYPE " +
                    "FROM ALL_TRIGGERS T " +
                    "WHERE T.OWNER = ? AND T.TABLE_NAME IS NULL " +
                    "ORDER BY TRIGGER_NAME"
            );
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoSchemaTrigger fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull JDBCResultSet resultSet
        ) {
            String triggerName = JDBCUtils.safeGetStringTrimmed(resultSet, "TRIGGER_NAME");
            return CommonUtils.isEmpty(triggerName) ? null : new TiberoSchemaTrigger(owner, resultSet);
        }
    }

    public static class TableTriggerCache extends JDBCCompositeCache<TiberoSchema, TiberoTableBase, TiberoTableTrigger, TiberoTriggerColumn> {
        TableTriggerCache(TableCache tableCache) {
            super(tableCache, TiberoTableBase.class, "TABLE_NAME", "TRIGGER_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @Nullable TiberoTableBase forParent
        ) throws SQLException {
            String descProp = owner.getDataSource().getContainer()
                .getConnectionConfiguration()
                .getProviderProperty(TiberoConstants.PROP_SHOW_SCHEMA_TRIGGER_TABLE_DESCRIPTION);
            boolean showTableDescription = descProp == null || CommonUtils.toBoolean(descProp);
            StringBuilder sql = new StringBuilder(
                "SELECT T.OWNER, T.TRIGGER_NAME, T.TRIGGER_TYPE, T.TRIGGERING_EVENT, " +
                    "T.TABLE_OWNER, T.TABLE_NAME, T.REFERENCING_NAMES, T.WHEN_CLAUSE, T.STATUS, " +
                    "'TABLE' AS BASE_OBJECT_TYPE, NULL AS COLUMN_NAME, " +
                    (showTableDescription ? "T.TABLE_NAME" : "NULL") + " AS DESCRIPTION, NULL AS ACTION_TYPE, " +
                    "C.COLUMN_NAME AS TRIGGER_COLUMN_NAME, C.COLUMN_LIST " +
                    "FROM ALL_TRIGGERS T " +
                    "LEFT JOIN ALL_TRIGGER_COLS C " +
                    "  ON C.TRIGGER_OWNER = T.OWNER AND C.TRIGGER_NAME = T.TRIGGER_NAME " +
                    " AND C.TABLE_OWNER = T.TABLE_OWNER AND C.TABLE_NAME = T.TABLE_NAME " +
                    "WHERE T.TABLE_OWNER = ? AND T.TABLE_NAME IS NOT NULL"
            );
            if (forParent != null) {
                sql.append(" AND T.TABLE_NAME = ?");
            }
            sql.append(" ORDER BY T.TABLE_NAME, T.TRIGGER_NAME");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forParent != null) {
                dbStat.setString(2, forParent.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoTableTrigger fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull TiberoTableBase parent,
            @NotNull String childName,
            @NotNull JDBCResultSet resultSet
        ) {
            return new TiberoTableTrigger(parent, resultSet);
        }

        @Nullable
        @Override
        protected TiberoTriggerColumn[] fetchObjectRow(
            @NotNull JDBCSession session,
            @NotNull TiberoTableBase parent,
            @NotNull TiberoTableTrigger forObject,
            @NotNull JDBCResultSet resultSet
        ) throws DBException {
            String columnName = JDBCUtils.safeGetStringTrimmed(resultSet, "TRIGGER_COLUMN_NAME");
            if (CommonUtils.isEmpty(columnName)) {
                return null;
            }
            TiberoTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for trigger '" + forObject.getName() + "'");
                return null;
            }
            return new TiberoTriggerColumn[] {
                new TiberoTriggerColumn(forObject, tableColumn, resultSet)
            };
        }

        @Override
        protected void cacheChildren(
            @NotNull DBRProgressMonitor monitor,
            @NotNull TiberoTableTrigger object,
            @NotNull List<TiberoTriggerColumn> children
        ) {
            object.setColumns(children);
        }

        @Override
        protected boolean isEmptyObjectRowsAllowed() {
            return true;
        }
    }

    public static class SequenceCache extends JDBCObjectCache<TiberoSchema, TiberoSequence> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull TiberoSchema owner)
            throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER = ? ORDER BY SEQUENCE_NAME"
            );
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoSequence fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull JDBCResultSet resultSet
        ) {
            String name = JDBCUtils.safeGetString(resultSet, "SEQUENCE_NAME");
            if (CommonUtils.isEmpty(name)) {
                return null;
            }
            TiberoSequence sequence = new TiberoSequence(owner, resultSet);
//            log.debug("Loaded Tibero sequence " + owner.getName() + "." + name);
            return sequence;
        }
    }

    public static class ProceduresCache extends JDBCObjectCache<TiberoSchema, TiberoProcedure> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull TiberoSchema owner)
            throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OBJECT_NAME, OBJECT_TYPE, STATUS " +
                    "FROM ALL_OBJECTS " +
                    "WHERE OWNER = ? AND OBJECT_TYPE IN ('PROCEDURE', 'FUNCTION') " +
                    "ORDER BY OBJECT_TYPE, OBJECT_NAME"
            );
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Nullable
        @Override
        protected TiberoProcedure fetchObject(
            @NotNull JDBCSession session,
            @NotNull TiberoSchema owner,
            @NotNull JDBCResultSet resultSet
        ) {
            String name = JDBCUtils.safeGetString(resultSet, "OBJECT_NAME");
            return CommonUtils.isEmpty(name) ? null : new TiberoProcedure(owner, resultSet);
        }
    }
}
