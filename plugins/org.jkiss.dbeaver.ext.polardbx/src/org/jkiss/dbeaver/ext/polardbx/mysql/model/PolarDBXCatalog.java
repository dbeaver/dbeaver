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
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedureParameter;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableIndex;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableIndexColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLView;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCResultSetImpl;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.DBUtils;
import java.sql.Types;
import java.sql.DatabaseMetaData;
import org.jkiss.utils.CommonUtils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map;
import java.util.HashMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PolarDBXCatalog extends MySQLCatalog {
    private static final Log log = Log.getLog(PolarDBXCatalog.class);

    private static final Set<String> INFO_SCHEMA_VIEW_BLACKLIST = Set.of(
        "INNODB_SYS_TABLESPACES",
        "LOCAL_PARTITIONS",
        "FULL_TABLE_GROUP",
        "TABLE_GROUP"
    );

    public PolarDBXCatalog(@NotNull MySQLDataSource dataSource, JDBCResultSet dbResult) {
        super(dataSource, dbResult);
    }

    private boolean isInfoSchema() {
        return MySQLConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(getName());
    }

    private static boolean isBlacklistedName(@NotNull String name) {
        for (String s : INFO_SCHEMA_VIEW_BLACKLIST) {
            if (s.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlacklistedView(@NotNull MySQLTableBase tableBase) {
        if (!(tableBase instanceof MySQLView)) {
            return false;
        }
        return isBlacklistedName(tableBase.getName());
    }

    @Override
    public Collection<MySQLView> getViews(@NotNull DBRProgressMonitor monitor) throws DBException {
        Collection<MySQLView> views = super.getViews(monitor);
        if (!isInfoSchema() || views == null || views.isEmpty()) {
            return views;
        }
        List<MySQLView> filtered = new ArrayList<>();
        for (MySQLView v : views) {
            if (!isBlacklistedName(v.getName())) {
                filtered.add(v);
            }
        }
        return filtered;
    }

    @Override
    public Collection<MySQLTableBase> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        Collection<MySQLTableBase> children = super.getChildren(monitor);
        if (!isInfoSchema() || children == null || children.isEmpty()) {
            return children;
        }
        List<MySQLTableBase> filtered = new ArrayList<>();
        for (MySQLTableBase t : children) {
            if (!isBlacklistedView(t)) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    @Override
    public MySQLTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        MySQLTableBase child = super.getChild(monitor, childName);
        if (child != null && isInfoSchema() && isBlacklistedView(child)) {
            return null;
        }
        return child;
    }

    private final MySQLCatalog.TableCache polarTableCache = new PolarDBXTableCache();
    private final PolarDBXProceduresCache polarProceduresCache = new PolarDBXProceduresCache();
    private final PolarDBXIndexCache polarIndexCache = new PolarDBXIndexCache(polarTableCache);

    @Override
    public MySQLCatalog.TableCache getTableCache() {
        return polarTableCache;
    }

    @Override
    public MySQLCatalog.ProceduresCache getProceduresCache() {
        return polarProceduresCache;
    }

    // Override the parent method to directly return our smart index cache.
    // Note: we cannot override getIndexCache() directly here because the return type does not match,
    // so we provide the smart cache functionality by overriding the getIndexes() method instead.
    @Override
    public Collection<MySQLTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return polarIndexCache.getAllObjects(monitor, this);
    }



    /**
     * PolarDB-X custom stored procedure cache implementation.
     * Supports searching stored procedures and functions in both the user database
     * and the mysql system database at the same time.
     */
    public class PolarDBXProceduresCache extends MySQLCatalog.ProceduresCache {

        // Function definition cache to avoid repeated parsing.
        private final Map<String, Map<String, FunctionDefinitionParser.ParameterInfo>> functionDefinitionCache = new HashMap<>();

        @Override
        protected MySQLProcedure fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {
            // Use PolarDBXProcedure instead of MySQLProcedure to support the special handling logic for functions,
            // and to ensure our overridden getName() and getFullyQualifiedName() methods are used.
            return new PolarDBXProcedure((PolarDBXCatalog) owner, dbResult);
        }



        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @Nullable MySQLProcedure object,
            @Nullable String objectName
        ) throws SQLException {
            // Modify the SQL query to search both the user database and the mysql system database.
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + MySQLConstants.META_TABLE_ROUTINES +
                    "\nWHERE " + MySQLConstants.COL_ROUTINE_SCHEMA + " IN (?, 'mysql')" +
                    (object == null && objectName == null ? "" : " AND " + MySQLConstants.COL_ROUTINE_NAME + "=?") +
                    " AND ROUTINE_TYPE" + (object == null ? " IN ('PROCEDURE','FUNCTION')" : "=?") +
                    "\nORDER BY " + MySQLConstants.COL_ROUTINE_NAME
            );
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
                if (object != null) {
                    dbStat.setString(3, String.valueOf(object.getProcedureType()));
                }
            }
            return dbStat;
        }



        /**
         * Fetch the function definition and parse its parameter information.
         */
        private Map<String, FunctionDefinitionParser.ParameterInfo> getFunctionParameterInfo(JDBCSession session, String functionName) {
            String cacheKey = functionName;
            Map<String, FunctionDefinitionParser.ParameterInfo> cachedInfo = functionDefinitionCache.get(cacheKey);
            if (cachedInfo != null) {
                return cachedInfo;
            }

            try {
                // Use SHOW CREATE FUNCTION to obtain the function definition.
                // Note: in PolarDB-X the function name is used directly to avoid the double "mysql" prefix issue.
                String showCreateSql = "SHOW CREATE FUNCTION `" + functionName + "`";

                try (JDBCPreparedStatement stmt = session.prepareStatement(showCreateSql)) {
                    try (JDBCResultSet result = stmt.executeQuery()) {
                        if (result.next()) {
                            String functionDefinition = JDBCUtils.safeGetString(result, "Create Function");
                            if (functionDefinition != null) {
                                Map<String, FunctionDefinitionParser.ParameterInfo> paramInfo =
                                    FunctionDefinitionParser.parseFunctionDefinition(functionDefinition);
                                functionDefinitionCache.put(cacheKey, paramInfo);
                                return paramInfo;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                log.debug("Error reading function definition", e);
            }

            // Return an empty map as a fallback.
            Map<String, FunctionDefinitionParser.ParameterInfo> emptyInfo = new HashMap<>();
            functionDefinitionCache.put(cacheKey, emptyInfo);
            return emptyInfo;
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @Nullable MySQLProcedure procedure
        )
            throws SQLException
        {
            if (procedure != null && procedure.getProcedureType() == DBSProcedureType.FUNCTION) {
                // Warm up the function definition parsing cache.
                getFunctionParameterInfo(session, procedure.getName());

                // For functions: use a custom SQL query against the information_schema.PARAMETERS table,
                // integrating the parsed length information.
                String sql = "SELECT " +
                    "SPECIFIC_CATALOG as PROCEDURE_CAT, " +
                    "SPECIFIC_SCHEMA as PROCEDURE_SCHEM, " +
                    "SPECIFIC_NAME as PROCEDURE_NAME, " +
                    "COALESCE(PARAMETER_NAME, 'RETURN') as COLUMN_NAME, " +
                    "CASE PARAMETER_MODE " +
                    "  WHEN 'IN' THEN 1 " +
                    "  WHEN 'OUT' THEN 4 " +
                    "  WHEN 'INOUT' THEN 2 " +
                    "  ELSE 5 " +  // 5 = procedureColumnReturn for RETURN parameters
                    "END as COLUMN_TYPE, " +
                    "DATA_TYPE as TYPE_NAME, " +
                    // Dynamically compute the JDBC Types constant.
                    "CASE LOWER(DATA_TYPE) " +
                    "  WHEN 'bit' THEN -7 " +           // Types.BIT
                    "  WHEN 'bool' THEN 16 " +          // Types.BOOLEAN
                    "  WHEN 'boolean' THEN 16 " +       // Types.BOOLEAN
                    "  WHEN 'tinyint' THEN -6 " +       // Types.TINYINT
                    "  WHEN 'smallint' THEN 5 " +       // Types.SMALLINT
                    "  WHEN 'mediumint' THEN 4 " +      // Types.INTEGER
                    "  WHEN 'int' THEN 4 " +            // Types.INTEGER
                    "  WHEN 'integer' THEN 4 " +        // Types.INTEGER
                    "  WHEN 'int24' THEN 4 " +          // Types.INTEGER
                    "  WHEN 'bigint' THEN -5 " +        // Types.BIGINT
                    "  WHEN 'real' THEN 8 " +           // Types.DOUBLE
                    "  WHEN 'float' THEN 7 " +          // Types.REAL
                    "  WHEN 'decimal' THEN 3 " +        // Types.DECIMAL
                    "  WHEN 'dec' THEN 3 " +            // Types.DECIMAL
                    "  WHEN 'numeric' THEN 3 " +        // Types.DECIMAL
                    "  WHEN 'double' THEN 8 " +         // Types.DOUBLE
                    "  WHEN 'double precision' THEN 8 " + // Types.DOUBLE
                    "  WHEN 'char' THEN 1 " +           // Types.CHAR
                    "  WHEN 'varchar' THEN 12 " +       // Types.VARCHAR
                    "  WHEN 'date' THEN 91 " +          // Types.DATE
                    "  WHEN 'time' THEN 92 " +          // Types.TIME
                    "  WHEN 'year' THEN 91 " +          // Types.DATE
                    "  WHEN 'timestamp' THEN 93 " +     // Types.TIMESTAMP
                    "  WHEN 'datetime' THEN 93 " +      // Types.TIMESTAMP
                    "  WHEN 'tinyblob' THEN -2 " +      // Types.BINARY
                    "  WHEN 'blob' THEN -4 " +          // Types.LONGVARBINARY
                    "  WHEN 'mediumblob' THEN -4 " +    // Types.LONGVARBINARY
                    "  WHEN 'longblob' THEN -4 " +      // Types.LONGVARBINARY
                    "  WHEN 'tinytext' THEN 12 " +      // Types.VARCHAR
                    "  WHEN 'text' THEN 12 " +          // Types.VARCHAR
                    "  WHEN 'mediumtext' THEN 12 " +    // Types.VARCHAR
                    "  WHEN 'longtext' THEN 12 " +      // Types.VARCHAR
                    "  WHEN 'enum' THEN 1 " +           // Types.CHAR
                    "  WHEN 'set' THEN 1 " +            // Types.CHAR
                    "  WHEN 'geometry' THEN -2 " +      // Types.BINARY
                    "  WHEN 'binary' THEN -2 " +        // Types.BINARY
                    "  WHEN 'varbinary' THEN -3 " +     // Types.VARBINARY
                    "  WHEN 'uuid' THEN 1 " +           // Types.CHAR
                    "  ELSE 1111 " +                    // Types.OTHER
                    "END as DATA_TYPE, " +
                    // Simplified PRECISION calculation: prefer numeric precision,
                    // otherwise character length; the Java code handles the default value.
                    "COALESCE(NULLIF(NUMERIC_PRECISION, 0), NULLIF(CHARACTER_MAXIMUM_LENGTH, 0), 0) as PRECISION, " +
                    // Simplified LENGTH calculation: prefer character length,
                    // otherwise numeric precision; the Java code handles the default value.
                    "COALESCE(NULLIF(CHARACTER_MAXIMUM_LENGTH, 0), NULLIF(NUMERIC_PRECISION, 0), 0) as LENGTH, " +
                    "COALESCE(NUMERIC_SCALE, 0) as SCALE, " +
                    "10 as RADIX, " +
                    "1 as NULLABLE, " +
                    "'' as REMARKS, " +
                    "'' as COLUMN_DEF, " +
                    "0 as SQL_DATA_TYPE, " +
                    "0 as SQL_DATETIME_SUB, " +
                    "COALESCE(CHARACTER_OCTET_LENGTH, CHARACTER_MAXIMUM_LENGTH, 0) as CHAR_OCTET_LENGTH, " +
                    "ORDINAL_POSITION as ORDINAL_POSITION, " +
                    "'YES' as IS_NULLABLE, " +
                    "SPECIFIC_NAME as SPECIFIC_NAME " +
                    "FROM information_schema.PARAMETERS " +
                    "WHERE SPECIFIC_SCHEMA = 'mysql' AND SPECIFIC_NAME = ? " +
                    "ORDER BY ORDINAL_POSITION";

                JDBCPreparedStatement stmt = session.prepareStatement(sql);
                stmt.setString(1, "mysql." + procedure.getName());

                // Parsed parameter information is cached for fetchChild(), which supplies missing lengths.
                return stmt;
            } else {
                // For stored procedures: use the standard JDBC approach.
                String schemaName = owner.getName();

                return session.getMetaData().getProcedureColumns(
                    schemaName,
                    null,
                    procedure.getName(),
                    "%").getSourceStatement();
            }
        }

        @Override
        protected MySQLProcedureParameter fetchChild(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @NotNull MySQLProcedure parent,
            @NotNull JDBCResultSet dbResult
        )
            throws SQLException, DBException {

            if (parent.getProcedureType() == DBSProcedureType.FUNCTION) {
                // For functions: parse the parameter length via SHOW CREATE FUNCTION and create the parameter object directly.
                return createFunctionParameterWithParsedLength(session, owner, parent, dbResult);
            } else {
                // For stored procedures: use the standard handling of the parent class.
                return super.fetchChild(session, owner, parent, dbResult);
            }
        }

        /**
         * Inspired by the OceanBase plugin approach: directly create function parameter objects with parsed lengths.
         * Avoids complex result set wrappers by using the actual length at parameter creation time.
         */
        private MySQLProcedureParameter createFunctionParameterWithParsedLength(
            @NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull MySQLProcedure parent, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException {

            // Get the parameter information parsed from the function definition.
            Map<String, FunctionDefinitionParser.ParameterInfo> functionParams =
                getFunctionParameterInfo(session, parent.getName());

            // Get the basic information from the result set.
            String paramName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
            String typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
            int columnType = JDBCUtils.safeGetInt(dbResult, "COLUMN_TYPE");
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, "ORDINAL_POSITION");
            int originalPrecision = JDBCUtils.safeGetInt(dbResult, "PRECISION");
            int scale = JDBCUtils.safeGetInt(dbResult, "SCALE");

            // Determine the parameter kind.
            DBSProcedureParameterKind parameterKind;
            switch (columnType) {
            case 1: parameterKind = DBSProcedureParameterKind.IN; break;
            case 2: parameterKind = DBSProcedureParameterKind.INOUT; break;
            case 4: parameterKind = DBSProcedureParameterKind.OUT; break;
            case 5: parameterKind = DBSProcedureParameterKind.RETURN; break;
            default: parameterKind = DBSProcedureParameterKind.IN; break;
            }

            // Get the data type.
            DBSDataType dataType = owner.getDataSource().getLocalDataType(typeName);
            int typeID = dataType != null ? dataType.getTypeID() : Types.VARCHAR;

            // Use the parsed length information.
            int enhancedLength = getEnhancedParameterLength(functionParams, paramName, typeName, originalPrecision);

            // Create the parameter object directly, using the actual parsed length.
            MySQLProcedureParameter parameter = new MySQLProcedureParameter(
                parent,
                DBUtils.getUnQuotedIdentifier(owner.getDataSource(), paramName),  // parameter name
                typeName,  // data type name (e.g. VARCHAR)
                typeID,
                ordinalPosition,
                enhancedLength,  // use the actual parsed length
                scale,
                null,
                true,
                parameterKind
            );

            return parameter;
        }

        /**
         * Gets the parameter length from information_schema when available, then falls back to the
         * SHOW CREATE FUNCTION declaration and finally to a Connector/J-compatible type default.
         */
        private int getEnhancedParameterLength(Map<String, FunctionDefinitionParser.ParameterInfo> functionParams,
                                               String paramName, String typeName, int originalPrecision) {

            // Handle the key mapping for the RETURN parameter to ensure key consistency.
            String lookupKey;
            if ("RETURN".equals(paramName)) {
                lookupKey = "return";
            } else {
                // Remove backticks and convert to lower case, consistent with how keys are stored during parsing.
                lookupKey = paramName.replaceAll("`", "").toLowerCase();
            }

            // Prefer metadata returned by information_schema.PARAMETERS.
            if (originalPrecision > 0) {
                return originalPrecision;
            }

            FunctionDefinitionParser.ParameterInfo paramInfo = functionParams.get(lookupKey);
            if (paramInfo != null && paramInfo.getLength() > 0) {
                return paramInfo.getLength();
            }

            // Fall back to the basic default length.
            int defaultLength = getBasicDefaultLengthForType(typeName);
            return defaultLength;
        }

        /**
         * Get the basic default length based on the data type.
         * A simplified version providing default lengths for the main types.
         */
        private int getBasicDefaultLengthForType(String typeName) {
            if (typeName == null) return 255;

            String lowerType = typeName.toLowerCase();
            switch (lowerType) {
            // Numeric types
            case "bit": return 1;
            case "tinyint": return 3;
            case "bool": case "boolean": return 3;
            case "smallint": return 5;
            case "mediumint": return 7;
            case "int": case "integer": case "int24": return 10;
            case "bigint": return 19;
            case "decimal": case "dec": case "numeric": return 65;
            case "float": return 12;
            case "real": case "double": case "double precision": return 22;

            // String types
            case "char": return 1;
            case "varchar": return 65535;
            case "tinytext": return 255;
            case "text": return 65535;
            case "mediumtext": return 16777215;
            case "longtext": return 2147483647;
            case "binary": return 255;
            case "varbinary": return 65535;
            case "tinyblob": return 255;
            case "blob": return 65535;
            case "mediumblob": return 16777215;
            case "longblob": return 2147483647;
            case "enum": return 2;
            case "set": return 3;

            // Date and time types
            case "date": return 10;        // YYYY-MM-DD
            case "time": return 8;         // HH:MM:SS
            case "datetime": return 19;    // YYYY-MM-DD HH:MM:SS
            case "timestamp": return 19;   // YYYY-MM-DD HH:MM:SS
            case "year": return 4;         // YYYY

            // JSON type
            case "json": return 1073741824;

            default: return 255;
            }
        }



    }

    /**
     * PolarDB-X smart index cache implementation.
     * Avoids repeated queries and optimizes the loading strategy.
     */
    public class PolarDBXIndexCache extends JDBCCompositeCache<MySQLCatalog, MySQLTable, MySQLTableIndex, MySQLTableIndexColumn> {

        private volatile boolean fullCacheLoaded = false;

        PolarDBXIndexCache(TableCache tableCache) {
            super(tableCache, MySQLTable.class, MySQLConstants.COL_TABLE_NAME, MySQLConstants.COL_INDEX_NAME);
        }

        @Override
        protected void loadObjects(@NotNull DBRProgressMonitor monitor, @NotNull MySQLCatalog owner, @Nullable MySQLTable forParent)
            throws DBException
        {
            synchronized (this) {
                // New strategy: for a full-database query, always reload to ensure data synchronization.
                if (forParent == null) {
                    // Clear the existing cache to avoid duplicates.
                    super.clearCache();
                    super.loadObjects(monitor, owner, null);
                    fullCacheLoaded = true;
                    return;
                }

                // If the full-database cache has already been loaded and a single table is requested, return directly.
                if (fullCacheLoaded && forParent != null) {
                    return;
                }

                // If this is the first single-table load, load the whole database directly.
                if (!fullCacheLoaded) {
                    super.loadObjects(monitor, owner, null); // force a full-database query
                    fullCacheLoaded = true;
                    return;
                }

                // Handle other cases with the original logic.
                super.loadObjects(monitor, owner, forParent);
            }
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, MySQLTable forTable)
            throws SQLException
        {
            // Use the same query logic as MySQL.
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * FROM ").append(MySQLConstants.META_TABLE_STATISTICS)
               .append(" WHERE ").append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_TABLE_NAME)
                .append(",").append(MySQLConstants.COL_INDEX_NAME)
                .append(",").append(MySQLConstants.COL_SEQ_IN_INDEX);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected MySQLTableIndex fetchObject(
            @NotNull JDBCSession session,
            @NotNull MySQLCatalog owner,
            @NotNull MySQLTable parent,
            @NotNull String indexName,
            @NotNull JDBCResultSet dbResult
        )
            throws SQLException, DBException
        {
            // Replicate the implementation logic of MySQL IndexCache.
            String indexTypeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_INDEX_TYPE);
            DBSIndexType indexType;
            if (MySQLConstants.INDEX_TYPE_BTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_BTREE;
            } else if (MySQLConstants.INDEX_TYPE_FULLTEXT.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_FULLTEXT;
            } else if (CommonUtils.isNotEmpty(indexTypeName) &&
                indexTypeName.toUpperCase(Locale.ENGLISH).contains(MySQLConstants.INDEX_TYPE_HASH.getId().toUpperCase(Locale.ENGLISH))
            ) {
                indexType = MySQLConstants.INDEX_TYPE_HASH;
            } else if (MySQLConstants.INDEX_TYPE_RTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_RTREE;
            } else {
                indexType = DBSIndexType.OTHER;
            }
            return new MySQLTableIndex(parent, indexName, indexType, dbResult);
        }

        @Nullable
        @Override
        protected MySQLTableIndexColumn[] fetchObjectRow(
            @NotNull JDBCSession session,
            @NotNull MySQLTable parent,
            @NotNull MySQLTableIndex object,
            @NotNull JDBCResultSet dbResult
        )
            throws SQLException, DBException
        {
            // Replicate the implementation logic of MySQL IndexCache.
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_SEQ_IN_INDEX);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLLATION);
            boolean nullable = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_NULLABLE));
            String subPart = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_SUB_PART);

            MySQLTableColumn tableColumn = columnName == null ? null : parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                return null;
            }

            return new MySQLTableIndexColumn[] { new MySQLTableIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                "A".equalsIgnoreCase(ascOrDesc),
                nullable,
                subPart)
            };
        }

        @Override
        protected void cacheChildren(
            @NotNull DBRProgressMonitor monitor,
            @NotNull MySQLTableIndex index,
            @NotNull List<MySQLTableIndexColumn> rows
        )
        {
            for (MySQLTableIndexColumn column : rows) {
                index.addColumn(column);
            }
        }

        @Override
        public void clearCache() {
            synchronized (this) {
                super.clearCache();
                fullCacheLoaded = false;
            }
        }
    }
}
