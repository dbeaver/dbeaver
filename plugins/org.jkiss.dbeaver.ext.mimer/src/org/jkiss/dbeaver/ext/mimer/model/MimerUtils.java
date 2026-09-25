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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Helpers shared by Mimer SQL model objects.
 *
 * @author Mimer Information Technology
 */
public class MimerUtils {

    private static final Log log = Log.getLog(MimerUtils.class);

    private MimerUtils() {
        // utility class
    }

    /**
     * Reads the stored source text of a schema object from
     * {@code INFORMATION_SCHEMA.EXT_SOURCE_DEFINITION}.
     *
     * @param objectType one of {@link MimerConstants} {@code SOURCE_TYPE_*} values
     * @param nameColumn the column in EXT_SOURCE_DEFINITION that carries {@code objectName}
     *                   ({@code OBJECT_NAME} for views/triggers/modules/domains,
     *                   {@code SPECIFIC_NAME} for procedures/functions)
     */
    @NotNull
    public static String readSourceDefinition(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject context,
        @NotNull String schemaName,
        @NotNull String objectName,
        @NotNull String objectType,
        @NotNull String nameColumn
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, context, "Read Mimer SQL source definition")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT SOURCE_DEFINITION\n" +
                "FROM INFORMATION_SCHEMA.EXT_SOURCE_DEFINITION\n" +
                "WHERE OBJECT_SCHEMA = ? AND " + nameColumn + " = ? AND OBJECT_TYPE = ?\n" +
                "ORDER BY LINE_NUMBER")
            ) {
                dbStat.setString(1, schemaName);
                dbStat.setString(2, objectName);
                dbStat.setString(3, objectType);
                // EXT_SOURCE_DEFINITION stores one source line per row (keyed by LINE_NUMBER);
                // re-join with newlines, tolerating rows that already carry a trailing one.
                List<String> lines = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String line = CommonUtils.notEmpty(JDBCUtils.safeGetString(dbResult, 1));
                        while (line.endsWith("\n") || line.endsWith("\r")) {
                            line = line.substring(0, line.length() - 1);
                        }
                        lines.add(line);
                    }
                }
                return lines.isEmpty() ? MimerConstants.SOURCE_NOT_AVAILABLE : String.join("\n", lines);
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, context.getDataSource());
        }
    }

    /**
     * Reads a statement's body text from {@code INFORMATION_SCHEMA.EXT_STATEMENT_DEFINITION} -
     * a separate view from {@link #readSourceDefinition}'s {@code EXT_SOURCE_DEFINITION} (which
     * also carries an {@code OBJECT_TYPE = 'STATEMENT'} row with the same text).
     * Returns only the body - the {@code CREATE [SCROLL|NO SCROLL] STATEMENT "s"."n"} header
     * isn't stored here or in {@code EXT_SOURCE_DEFINITION}; {@link MimerStatement} builds it
     * from {@code EXT_STATEMENTS}' own {@code IS_SCROLLABLE}/{@code IS_FORWARD_ONLY} columns.
     */
    @NotNull
    public static String readStatementDefinition(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject context,
        @NotNull String schemaName,
        @NotNull String statementName
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, context, "Read Mimer SQL statement definition")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT STATEMENT_DEFINITION\n" +
                "FROM INFORMATION_SCHEMA.EXT_STATEMENT_DEFINITION\n" +
                "WHERE STATEMENT_SCHEMA = ? AND STATEMENT_NAME = ?\n" +
                "ORDER BY STATEMENT_SEQUENCE_NO")
            ) {
                dbStat.setString(1, schemaName);
                dbStat.setString(2, statementName);
                List<String> lines = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String line = CommonUtils.notEmpty(JDBCUtils.safeGetString(dbResult, 1));
                        while (line.endsWith("\n") || line.endsWith("\r")) {
                            line = line.substring(0, line.length() - 1);
                        }
                        lines.add(line);
                    }
                }
                return lines.isEmpty() ? MimerConstants.SOURCE_NOT_AVAILABLE : String.join("\n", lines);
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, context.getDataSource());
        }
    }

    /**
     * Reconstructs a {@code CREATE DOMAIN} statement from catalog metadata - unlike other schema
     * objects, a domain's declaration isn't stored as source text in {@code
     * EXT_SOURCE_DEFINITION}. Doesn't yet handle a default or CHECK clause too long to fit inline
     * in the catalog (reported as a {@code 'TRUNCATED'} sentinel, with the real text falling back
     * to {@code EXT_SOURCE_DEFINITION} instead) or {@code INTERVAL} domains' type-suffix
     * formatting.
     */
    @NotNull
    public static String buildDomainSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject context,
        @NotNull String schemaName,
        @NotNull String domainName
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, context, "Read Mimer SQL domain definition")) {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE DOMAIN \"").append(schemaName).append("\".\"").append(domainName).append("\" AS ");

            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE,\n" +
                "       DOMAIN_DEFAULT, COLLATION_SCHEMA, COLLATION_NAME\n" +
                "FROM INFORMATION_SCHEMA.DOMAINS\n" +
                "WHERE DOMAIN_SCHEMA = ? AND DOMAIN_NAME = ?")
            ) {
                dbStat.setString(1, schemaName);
                dbStat.setString(2, domainName);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (!dbResult.next()) {
                        return MimerConstants.SOURCE_NOT_AVAILABLE;
                    }
                    String dataType = JDBCUtils.safeGetStringTrimmed(dbResult, "DATA_TYPE");
                    long charLength = JDBCUtils.safeGetLong(dbResult, "CHARACTER_MAXIMUM_LENGTH");
                    Integer numPrecision = JDBCUtils.safeGetInteger(dbResult, "NUMERIC_PRECISION");
                    Integer numScale = JDBCUtils.safeGetInteger(dbResult, "NUMERIC_SCALE");
                    String domainDefault = JDBCUtils.safeGetString(dbResult, "DOMAIN_DEFAULT");
                    String collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATION_SCHEMA");
                    String collationName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATION_NAME");

                    sb.append(formatDomainDataType(dataType, charLength, numPrecision, numScale));

                    // Skip the two implicit default collations - not worth spelling out explicitly.
                    boolean isDefaultCollation = "INFORMATION_SCHEMA".equalsIgnoreCase(collationSchema)
                        && ("ISO8BIT".equalsIgnoreCase(collationName) || "UCS_BASIC".equalsIgnoreCase(collationName));
                    if (!CommonUtils.isEmpty(collationName) && !isDefaultCollation) {
                        sb.append(" COLLATE \"").append(collationSchema).append("\".\"").append(collationName).append('"');
                    }
                    if (!CommonUtils.isEmpty(domainDefault)) {
                        sb.append("\n    DEFAULT ").append(domainDefault);
                    }
                }
            }

            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT CC.CONSTRAINT_NAME, CC.CHECK_CLAUSE\n" +
                "FROM INFORMATION_SCHEMA.DOMAIN_CONSTRAINTS DC\n" +
                "JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS CC\n" +
                "  ON DC.CONSTRAINT_SCHEMA = CC.CONSTRAINT_SCHEMA AND DC.CONSTRAINT_NAME = CC.CONSTRAINT_NAME\n" +
                "WHERE DC.DOMAIN_SCHEMA = ? AND DC.DOMAIN_NAME = ?\n" +
                "ORDER BY DC.CONSTRAINT_NAME")
            ) {
                dbStat.setString(1, schemaName);
                dbStat.setString(2, domainName);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String constraintName = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME");
                        String checkClause = JDBCUtils.safeGetString(dbResult, "CHECK_CLAUSE");
                        sb.append("\n    CONSTRAINT \"").append(constraintName).append("\" CHECK (").append(checkClause).append(')');
                    }
                }
            }

            return sb.toString();
        } catch (SQLException e) {
            throw new DBDatabaseException(e, context.getDataSource());
        }
    }

    /**
     * Reconstructs {@code CREATE COLLATION "schema"."name" FROM "baseSchema"."baseName" [USING
     * '...']} from {@code INFORMATION_SCHEMA.EXT_COLLATION_DEFINITIONS} - one row per collation
     * for a plain copy with no delta, more than one (ordered by {@code COLLATION_SEQNO}) when the
     * {@code USING} delta-string itself is long enough to be split across several rows; {@code
     * BASE_COLLATION_SCHEMA}/{@code BASE_COLLATION_NAME} are only meaningful on the first
     * (lowest-{@code COLLATION_SEQNO}) row. Works the same for a server's own built-in collations
     * (e.g. {@code "INFORMATION_SCHEMA"."ISO8BIT"}) as for a user-created one - both are rows in
     * this same view.
     */
    @NotNull
    public static String buildCollationSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject context,
        @NotNull String schemaName,
        @NotNull String collationName
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, context, "Read Mimer SQL collation definition")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT COLLATION_SEQNO, COLLATION_DEFINITION, BASE_COLLATION_SCHEMA, BASE_COLLATION_NAME\n" +
                "FROM INFORMATION_SCHEMA.EXT_COLLATION_DEFINITIONS\n" +
                "WHERE COLLATION_SCHEMA = ? AND COLLATION_NAME = ?\n" +
                "ORDER BY COLLATION_SEQNO")
            ) {
                dbStat.setString(1, schemaName);
                dbStat.setString(2, collationName);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder definition = new StringBuilder();
                    String baseSchema = null;
                    String baseName = null;
                    boolean any = false;
                    while (dbResult.next()) {
                        if (!any) {
                            baseSchema = JDBCUtils.safeGetString(dbResult, "BASE_COLLATION_SCHEMA");
                            baseName = JDBCUtils.safeGetString(dbResult, "BASE_COLLATION_NAME");
                            any = true;
                        }
                        definition.append(CommonUtils.notEmpty(JDBCUtils.safeGetString(dbResult, "COLLATION_DEFINITION")));
                    }
                    if (!any) {
                        return MimerConstants.SOURCE_NOT_AVAILABLE;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("CREATE COLLATION \"").append(schemaName).append("\".\"").append(collationName).append("\"\n");
                    sb.append("FROM \"").append(baseSchema).append("\".\"").append(baseName).append('"');
                    if (!definition.isEmpty()) {
                        sb.append("\nUSING '").append(definition).append('\'');
                    }
                    return sb.toString();
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, context.getDataSource());
        }
    }

    /**
     * Reads a procedure/function's parameters from {@code INFORMATION_SCHEMA.PARAMETERS} -
     * shared by {@link MimerProcedure} and {@link MimerModuleRoutine} (both otherwise-unrelated
     * subclasses of {@code GenericProcedure}, so this can't just be inherited) since the bundled
     * Mimer SQL JDBC driver ({@code mimjdbc3.jar}) predates JDBC 4.1 and does not implement
     * {@code DatabaseMetaData.getFunctionColumns()}.
     */
    public static void loadProcedureColumns(@NotNull GenericProcedure procedure, @NotNull DBRProgressMonitor monitor) throws DBException {
        loadProcedureColumns(procedure, monitor, procedure.getProcedureType() == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE");
    }

    /**
     * Same as {@link #loadProcedureColumns(GenericProcedure, DBRProgressMonitor)}, but with the
     * catalog {@code ROUTINE_TYPE} value given explicitly rather than derived from {@link
     * GenericProcedure#getProcedureType()} - needed for {@link MimerUdtMethod}, whose real
     * catalog type is {@code INSTANCE METHOD}/{@code STATIC METHOD}/{@code CONSTRUCTOR METHOD},
     * not {@code FUNCTION}, even though it reports {@link DBSProcedureType#FUNCTION} (every
     * method has a return value, same as a function).
     */
    public static void loadProcedureColumns(
        @NotNull GenericProcedure procedure,
        @NotNull DBRProgressMonitor monitor,
        @NotNull String routineType
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, procedure.getDataSource(), "Read Mimer SQL routine parameters")) {
            // Join through ROUTINES so we can filter on the routine NAME (matches getName())
            // rather than SPECIFIC_NAME, which the Mimer SQL catalog does not always expose to JDBC.
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p.PARAMETER_NAME, p.PARAMETER_MODE, p.ORDINAL_POSITION, p.DATA_TYPE,\n" +
                "       p.CHARACTER_MAXIMUM_LENGTH, p.NUMERIC_PRECISION, p.NUMERIC_SCALE\n" +
                "FROM INFORMATION_SCHEMA.PARAMETERS p\n" +
                "JOIN INFORMATION_SCHEMA.ROUTINES r\n" +
                "  ON r.SPECIFIC_SCHEMA = p.SPECIFIC_SCHEMA AND r.SPECIFIC_NAME = p.SPECIFIC_NAME\n" +
                "WHERE r.ROUTINE_SCHEMA = ? AND r.ROUTINE_NAME = ? AND r.ROUTINE_TYPE = ?\n" +
                "ORDER BY p.ORDINAL_POSITION")
            ) {
                dbStat.setString(1, procedure.getContainer().getName());
                dbStat.setString(2, procedure.getName());
                dbStat.setString(3, routineType);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, "PARAMETER_NAME");
                        String mode = JDBCUtils.safeGetStringTrimmed(dbResult, "PARAMETER_MODE");
                        int position = JDBCUtils.safeGetInt(dbResult, "ORDINAL_POSITION");
                        String typeName = JDBCUtils.safeGetStringTrimmed(dbResult, "DATA_TYPE");
                        int charLength = JDBCUtils.safeGetInt(dbResult, "CHARACTER_MAXIMUM_LENGTH");
                        Integer precision = JDBCUtils.safeGetInteger(dbResult, "NUMERIC_PRECISION");
                        Integer scale = JDBCUtils.safeGetInteger(dbResult, "NUMERIC_SCALE");

                        DBSProcedureParameterKind kind;
                        if (position == 0 || CommonUtils.isEmpty(mode)) {
                            // ORDINAL_POSITION 0 (no PARAMETER_MODE) is the function result
                            kind = procedure.getProcedureType() == DBSProcedureType.FUNCTION
                                ? DBSProcedureParameterKind.RETURN : DBSProcedureParameterKind.IN;
                        } else {
                            kind = switch (mode.toUpperCase()) {
                                case "OUT" -> DBSProcedureParameterKind.OUT;
                                case "INOUT" -> DBSProcedureParameterKind.INOUT;
                                default -> DBSProcedureParameterKind.IN;
                            };
                        }
                        if (CommonUtils.isEmpty(name) && kind == DBSProcedureParameterKind.RETURN) {
                            name = "RETURN";
                        }

                        int columnSize = charLength > 0 ? charLength : (precision != null ? precision : 0);
                        procedure.addColumn(new GenericProcedureParameter(
                            procedure,
                            name,
                            typeName,
                            resolveTypeId(procedure, typeName),
                            position,
                            columnSize,
                            scale,
                            precision,
                            false,
                            null,
                            kind));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, procedure.getDataSource());
        }
    }

    /**
     * A user-defined type method's return type. Unlike a plain procedure/function, {@code
     * INFORMATION_SCHEMA.PARAMETERS} carries no ordinal-0 row for a method's return type (the
     * convention {@link #loadProcedureColumns} otherwise relies on), so this reads it from
     * {@code ROUTINES.DATA_TYPE} directly instead. Adds a single synthetic {@link
     * DBSProcedureParameterKind#RETURN} column, same shape as the ordinal-0 one {@link
     * #loadProcedureColumns} adds for a plain function.
     */
    public static void loadMethodReturnType(@NotNull MimerUdtMethod method, @NotNull DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, method.getDataSource(), "Read Mimer SQL method return type")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE\n" +
                "FROM INFORMATION_SCHEMA.ROUTINES\n" +
                "WHERE SPECIFIC_SCHEMA = ? AND SPECIFIC_NAME = ? AND ROUTINE_TYPE = ?")
            ) {
                dbStat.setString(1, method.getContainer().getName());
                dbStat.setString(2, method.getUniqueName());
                dbStat.setString(3, method.getMethodKind());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        String typeName = JDBCUtils.safeGetStringTrimmed(dbResult, "DATA_TYPE");
                        int charLength = JDBCUtils.safeGetInt(dbResult, "CHARACTER_MAXIMUM_LENGTH");
                        Integer precision = JDBCUtils.safeGetInteger(dbResult, "NUMERIC_PRECISION");
                        Integer scale = JDBCUtils.safeGetInteger(dbResult, "NUMERIC_SCALE");
                        int columnSize = charLength > 0 ? charLength : (precision != null ? precision : 0);
                        method.addColumn(new GenericProcedureParameter(
                            method,
                            "RETURN",
                            typeName,
                            resolveTypeId(method, typeName),
                            0,
                            columnSize,
                            scale,
                            precision,
                            false,
                            null,
                            DBSProcedureParameterKind.RETURN));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, method.getDataSource());
        }
    }

    /**
     * For an external (Mimer SQL 11.1+ {@code LANGUAGE CLR}) routine, reconstructs the full
     * {@code CREATE PROCEDURE}/{@code CREATE FUNCTION ... LANGUAGE <language> EXTERNAL NAME
     * '...' IN "<library>"} statement from {@code ROUTINES.ROUTINE_BODY}/{@code
     * EXTERNAL_LANGUAGE}/{@code EXTERNAL_NAME}/{@code EXTERNAL_LIBRARY}. {@code
     * EXT_SOURCE_DEFINITION} - what every other routine's source comes from, see {@link
     * #readSourceDefinition} - has no row for an external routine, since it has no SQL body to
     * store.
     * <p>
     * Returns {@code null} for a normal SQL-bodied routine, or on a pre-11.1 server (where these
     * columns don't exist at all) - the caller then falls back to the usual {@code
     * EXT_SOURCE_DEFINITION}-based read.
     * <p>
     * The parameter list and (for a function) return type come from the procedure's own
     * already-loaded {@link GenericProcedure#getParameters(DBRProgressMonitor)}, not a fresh
     * query - {@link #loadProcedureColumns} already populated them.
     */
    @Nullable
    public static String buildExternalRoutineSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericProcedure procedure,
        @NotNull String schemaName,
        @NotNull String name,
        @NotNull String specificName
    ) throws DBException {
        MimerExternalRoutineInfo info = readExternalRoutineInfo(monitor, procedure, schemaName, specificName);
        if (!info.external()) {
            return null;
        }

        boolean isFunction = procedure.getProcedureType() == DBSProcedureType.FUNCTION;
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ").append(isFunction ? "FUNCTION" : "PROCEDURE")
            .append(" \"").append(schemaName).append("\".\"").append(name).append("\"(");
        String returnType = null;
        boolean first = true;
        for (GenericProcedureParameter param : CommonUtils.safeCollection(procedure.getParameters(monitor))) {
            if (param.getParameterKind() == DBSProcedureParameterKind.RETURN) {
                returnType = param.getFullTypeName();
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            first = false;
            if (!isFunction) {
                sb.append(param.getParameterKind().name()).append(' ');
            }
            sb.append('"').append(param.getName()).append("\" ").append(param.getFullTypeName());
        }
        sb.append(')');
        if (isFunction && returnType != null) {
            sb.append("\nRETURNS ").append(returnType);
        }
        sb.append("\nLANGUAGE ").append(CommonUtils.notEmpty(info.language()));
        sb.append("\nEXTERNAL NAME '").append(CommonUtils.notEmpty(info.externalName()).replace("'", "''")).append('\'');
        if (!CommonUtils.isEmpty(info.library())) {
            sb.append(" IN \"").append(info.library()).append('"');
        }
        return sb.toString();
    }

    /**
     * Reads {@code INFORMATION_SCHEMA.ROUTINES.ROUTINE_BODY}/{@code EXTERNAL_LANGUAGE}/{@code
     * EXTERNAL_NAME}/{@code EXTERNAL_LIBRARY} for one procedure/function - see {@link
     * MimerExternalRoutineInfo}. Returns {@link MimerExternalRoutineInfo#NOT_EXTERNAL} (never
     * throws for this reason) for a normal SQL-bodied routine, or on a pre-11.1 server where
     * these columns don't exist at all.
     */
    @NotNull
    public static MimerExternalRoutineInfo readExternalRoutineInfo(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericProcedure procedure,
        @NotNull String schemaName,
        @NotNull String specificName
    ) throws DBException {
        if (!(procedure.getDataSource() instanceof MimerDataSource dataSource) || !dataSource.supportsExternalLibraries()) {
            return MimerExternalRoutineInfo.NOT_EXTERNAL;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, procedure, "Read Mimer SQL external routine info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ROUTINE_BODY, EXTERNAL_LANGUAGE, EXTERNAL_NAME, EXTERNAL_LIBRARY\n" +
                "FROM INFORMATION_SCHEMA.ROUTINES\n" +
                "WHERE SPECIFIC_SCHEMA = ? AND SPECIFIC_NAME = ?")
            ) {
                dbStat.setString(1, schemaName);
                dbStat.setString(2, specificName);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (!dbResult.next() || !"EXTERNAL".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "ROUTINE_BODY"))) {
                        return MimerExternalRoutineInfo.NOT_EXTERNAL;
                    }
                    return new MimerExternalRoutineInfo(
                        true,
                        JDBCUtils.safeGetStringTrimmed(dbResult, "EXTERNAL_LANGUAGE"),
                        JDBCUtils.safeGetString(dbResult, "EXTERNAL_NAME"),
                        JDBCUtils.safeGetStringTrimmed(dbResult, "EXTERNAL_LIBRARY"));
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, procedure.getDataSource());
        }
    }

    private static int resolveTypeId(@NotNull GenericProcedure procedure, @NotNull String typeName) {
        DBSDataType dataType = procedure.getDataSource().getLocalDataType(typeName);
        return dataType != null ? dataType.getTypeID() : Types.OTHER;
    }

    /**
     * Reads an object's own comment (set via {@code COMMENT ON <type> ... IS '...'}) from
     * {@code INFORMATION_SCHEMA.EXT_OBJECT_IDENT_USAGE} - used by every Mimer SQL object type with a
     * manually-implemented "Comment" property (Table/View/Column get it for free instead, via
     * {@code MimerMetaModel#isTableCommentEditable}/{@code isTableColumnCommentEditable}).
     *
     * @param schemaName   {@code null} for a datasource-global object (matches {@code
     *                     OBJECT_SCHEMA IS NULL}); non-null for a schema-scoped object.
     * @param specificName non-null only for an overloaded routine, matched via {@code
     *                     SPECIFIC_NAME} ({@code COMMENT ON SPECIFIC FUNCTION/PROCEDURE}).
     * @param objectType   the {@code EXT_OBJECT_IDENT_USAGE.OBJECT_TYPE} value - not always the
     *                     same word as the {@code COMMENT ON <keyword>} DDL syntax (e.g. a table
     *                     is {@code "BASE TABLE"}; users/groups/programs are all {@code "IDENT"}).
     */
    @Nullable
    public static String readObjectComment(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject context,
        @Nullable String schemaName,
        @Nullable String specificName,
        @NotNull String objectName,
        @NotNull String objectType
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, context, "Read Mimer SQL object comment")) {
            StringBuilder sql = new StringBuilder("SELECT REMARKS FROM INFORMATION_SCHEMA.EXT_OBJECT_IDENT_USAGE WHERE ");
            sql.append(schemaName != null ? "OBJECT_SCHEMA = ? AND " : "OBJECT_SCHEMA IS NULL AND ");
            sql.append("OBJECT_NAME = ? AND OBJECT_TYPE = ?");
            if (specificName != null) {
                sql.append(" AND SPECIFIC_NAME = ?");
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
                int idx = 1;
                if (schemaName != null) {
                    dbStat.setString(idx++, schemaName);
                }
                dbStat.setString(idx++, objectName);
                dbStat.setString(idx++, objectType);
                if (specificName != null) {
                    dbStat.setString(idx, specificName);
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    return dbResult.next() ? JDBCUtils.safeGetString(dbResult, "REMARKS") : null;
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, context.getDataSource());
        }
    }

    /**
     * Builds a {@code COMMENT ON <keyword> <name> IS '<escaped>'} statement - the DDL half
     * matching {@link #readObjectComment}'s read side. {@code name} must already be the fully
     * quoted/qualified object reference (e.g. {@code "schema"."name"}, or a bare {@code "name"}
     * for a datasource-global object) - building that part is object-type-specific enough (some
     * are schema-qualified, some are specific-name-keyed, Column is three-part) that it's left
     * to each caller rather than folded in here.
     */
    @NotNull
    public static String buildCommentDDL(@NotNull DBSObject context, @NotNull String keyword, @NotNull String name, @Nullable String comment) {
        return "COMMENT ON " + keyword + " " + name + " IS " + SQLUtils.quoteString(context, CommonUtils.notEmpty(comment));
    }

    // --- SET DATABANK/SHADOW ONLINE/OFFLINE ---------------------------------------------------
    // Whether a databank/shadow is online is real, catalog-persisted state - shown as a read-only
    // "Online" checkbox in the properties view. Changing it is its own statement family, not
    // ALTER: SET DATABANK/SHADOW "n" OFFLINE | ONLINE PRESERVE LOG | ONLINE RESET LOG. Going
    // online forces a PRESERVE-vs-RESET-LOG choice, which a plain checkbox can't express.
    //
    // Reachable two ways: the "Change state" action dropdown next to the checkbox in the
    // properties view (ONLINE_NOCHANGE is its no-op default), and the "Set Online State"
    // navigator action (mimer.ui - also handles multi-select, Restore From Log, and To Master).
    // ONLINE_STATE_* are the literal SQL state clauses; the dropdown reuses them verbatim as its
    // non-sentinel options.

    public static final String ONLINE_NOCHANGE = "(no change)";
    public static final String ONLINE_STATE_OFFLINE = "OFFLINE";
    public static final String ONLINE_STATE_PRESERVE = "ONLINE PRESERVE LOG";
    public static final String ONLINE_STATE_RESET = "ONLINE RESET LOG";

    /**
     * The target states valid for an object currently in the given state: an online object can
     * only go {@code OFFLINE}; an offline one can come back {@code ONLINE} either preserving or
     * resetting the log.
     */
    @NotNull
    public static String[] onlineStatesFor(boolean online) {
        return online
            ? new String[]{ONLINE_STATE_OFFLINE}
            : new String[]{ONLINE_STATE_PRESERVE, ONLINE_STATE_RESET};
    }

    /**
     * {@link #onlineStatesFor} with the {@link #ONLINE_NOCHANGE} sentinel prepended - the option
     * list for the properties-view "Change state" dropdown (its default, no-op value first).
     */
    @NotNull
    public static String[] onlineTransitionsFor(boolean online) {
        String[] states = onlineStatesFor(online);
        String[] result = new String[states.length + 1];
        result[0] = ONLINE_NOCHANGE;
        System.arraycopy(states, 0, result, 1, states.length);
        return result;
    }

    /** Whether the given target state / non-sentinel transition leaves the object online. */
    public static boolean stateGoesOnline(@Nullable String state) {
        return state != null && !ONLINE_NOCHANGE.equals(state) && !ONLINE_STATE_OFFLINE.equals(state);
    }

    /**
     * Force a {@code FORCE_REFRESH} on {@code object}'s navigator node so any open properties
     * editor fully reloads its property values from the (already-updated) model - used after the
     * "Change state" dropdown's SET succeeds, which the grid would otherwise not reflect until
     * the editor is reopened. Queued and dispatched on the UI thread, safely after the save.
     * Falls back to a plain object-update event when the node isn't loaded.
     */
    public static void refreshObjectEditor(@NotNull DBSObject object) {
        DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
        DBNDatabaseNode node = navigatorModel == null ? null : navigatorModel.getNodeByObject(object);
        if (node != null) {
            node.getModel().fireNodeUpdate(DBNEvent.FORCE_REFRESH, node, DBNEvent.NodeChange.REFRESH);
        } else {
            DBUtils.fireObjectUpdate(object);
        }
    }

    /**
     * {@code SET <keyword> "a", "b" <state>} - a statement family distinct from {@code ALTER
     * DATABANK}. {@code keyword} is {@code "DATABANK"} or {@code "SHADOW"}; {@code state} is one
     * of {@link #ONLINE_STATE_OFFLINE} / {@link #ONLINE_STATE_PRESERVE} / {@link #ONLINE_STATE_RESET};
     * {@code names} are the (unquoted) object names, quoted here (Mimer SQL accepts a list).
     * Returns {@code null} for the {@link #ONLINE_NOCHANGE} sentinel / an empty name list.
     */
    @Nullable
    public static String buildSetOnlineDDL(@NotNull String keyword, @NotNull List<String> names, @Nullable String state) {
        if (names.isEmpty() || state == null || ONLINE_NOCHANGE.equals(state)) {
            return null;
        }
        StringBuilder sb = new StringBuilder("SET ").append(keyword).append(' ');
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(names.get(i)).append('"');
        }
        return sb.append(' ').append(state).toString();
    }

    @NotNull
    public static String formatDomainDataType(
        @NotNull String dataType,
        long charLength,
        Integer numPrecision,
        Integer numScale
    ) {
        if (CommonUtils.isEmpty(dataType)) {
            return "";
        }
        String upper = dataType.toUpperCase();
        if (charLength > 0) {
            return upper + "(" + charLength + ")";
        }
        if (numPrecision != null && numPrecision > 0) {
            if (numScale != null && numScale > 0) {
                return upper + "(" + numPrecision + "," + numScale + ")";
            }
            return upper + "(" + numPrecision + ")";
        }
        return upper;
    }

    /**
     * Resolves one {@code EXT_OBJECT_OBJECT_USED}/{@code EXT_OBJECT_OBJECT_USING}-style row (an
     * object type name + schema + name, as read by {@link MimerObjectUsedBy}/{@link
     * MimerObjectUses}) to the real, already-modeled object it names. This is what lets a "Used
     * By"/"Uses" grid cell open the exact same detail view you'd get browsing the schema tree
     * directly, instead of only ever showing the bare name/schema/type.
     * <p>
     * Returns {@code null} rather than throwing whenever there's no resolver for the type, the
     * schema/object can't be found (e.g. dropped since the catalog row was read), or the lookup
     * itself fails - the caller then falls back to the plain summary display, same as before
     * this existed.
     * <p>
     * Not exhaustive by design: covers every type that resolves via a single, unambiguous
     * by-name lookup (tables/views, sequences, procedures/functions, modules, domains,
     * statements, synonyms, triggers, user-defined types, collations, databanks). Column/index/
     * constraint usages and UDT method usages need more than a bare name to resolve unambiguously
     * - the owning table/type isn't part of this row - so those are left unresolved.
     * <p>
     * {@code specificName}, when the row carries one (only routines do), picks out the exact
     * overload a procedure/function name resolves to - confirmed live that Mimer SQL allows
     * overloading a name by parameter list, so a bare-name lookup alone can silently resolve to
     * the wrong overload once more than one shares a name. Falls back to the first bare-name
     * match when no specific name is given (a non-routine type, or an older caller).
     */
    @Nullable
    public static DBSObject resolveDependencyObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull MimerDataSource dataSource,
        @Nullable String objectSchema,
        @Nullable String objectName,
        @Nullable String objectType,
        @Nullable String specificName
    ) {
        if (CommonUtils.isEmpty(objectName) || CommonUtils.isEmpty(objectType)) {
            return null;
        }
        try {
            String type = objectType.trim().toUpperCase(Locale.ENGLISH);
            if ("DATABANK".equals(type)) {
                return dataSource.getDatabankCache().getObject(monitor, dataSource, objectName);
            }
            if (objectSchema == null) {
                return null;
            }
            MimerSchema schema = (MimerSchema) dataSource.getSchema(objectSchema);
            if (schema == null) {
                return null;
            }
            switch (type) {
                case "TABLE":
                case "BASE TABLE":
                case "VIEW":
                    return schema.getTable(monitor, objectName);
                case "SEQUENCE":
                    return schema.getSequence(monitor, objectName);
                case "PROCEDURE":
                case "FUNCTION": {
                    List<GenericProcedure> procedures = schema.getProcedures(monitor, objectName);
                    if (!CommonUtils.isEmpty(procedures)) {
                        if (!CommonUtils.isEmpty(specificName)) {
                            for (GenericProcedure procedure : procedures) {
                                if (specificName.equals(procedure.getUniqueName())) {
                                    return procedure;
                                }
                            }
                        }
                        return procedures.get(0);
                    }
                    // Not in the flat schema-level list - MimerMetaModel#loadProcedures
                    // deliberately excludes routines declared inside a module (see
                    // MimerModuleRoutine), so look for a same-named one there instead.
                    MimerModuleRoutine firstNameMatch = null;
                    for (MimerModule module : CommonUtils.safeCollection(schema.getModules(monitor))) {
                        for (MimerModuleRoutine routine : CommonUtils.safeCollection(module.getRoutines(monitor))) {
                            if (!objectName.equals(routine.getName())) {
                                continue;
                            }
                            if (!CommonUtils.isEmpty(specificName) && specificName.equals(routine.getUniqueName())) {
                                return routine;
                            }
                            if (firstNameMatch == null) {
                                firstNameMatch = routine;
                            }
                        }
                    }
                    if (firstNameMatch != null) {
                        return firstNameMatch;
                    }
                    return null;
                }
                case "MODULE":
                    return schema.getModuleCache().getObject(monitor, schema, objectName);
                case "DOMAIN":
                    return schema.getDomainCache().getObject(monitor, schema, objectName);
                case "STATEMENT":
                    return schema.getStatementCache().getObject(monitor, schema, objectName);
                case "COLLATION":
                    return schema.getCollationCache().getObject(monitor, schema, objectName);
                case "SYNONYM":
                    return schema.getSynonym(monitor, objectName);
                case "TRIGGER":
                    return schema.getTableTrigger(monitor, objectName);
                case "USER DEFINED TYPE":
                    return schema.getUserDefinedTypeCache().getObject(monitor, schema, objectName);
                default:
                    return null;
            }
        } catch (DBException e) {
            log.debug("Can't resolve dependency object " + objectSchema + "." + objectName + " (" + objectType + ")", e);
            return null;
        }
    }
}
