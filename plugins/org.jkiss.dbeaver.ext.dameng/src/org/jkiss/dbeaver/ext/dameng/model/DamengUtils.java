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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dameng.DamengConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPObjectWithLongId;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.cache.AbstractObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Shengkai Bai
 */
public class DamengUtils {

    private static final int ERROR_UNRESOLVED_MEMBER = -2207;
    private static final String QUOTED_IDENTIFIER = "\"(?:\"\"|[^\"])*+\"";
    private static final String IDENTIFIER = "(?:" + QUOTED_IDENTIFIER + "|[\\p{L}_][\\p{L}\\p{N}_$#]*)";
    private static final String ALTERNATIVE_STRING = "\\bN?Q'(?:\\[(?s:.*?)\\]|\\{(?s:.*?)\\}|\\((?s:.*?)\\)|<(?s:.*?)>"
        + "|(?-i:(?<quoteDelimiter>[^\\s\\[{(<])(?s:.*?)\\k<quoteDelimiter>))'";
    private static final Pattern DDL_PARTS = Pattern.compile(
        ALTERNATIVE_STRING + "|'(?:''|[^'])*+'|" + QUOTED_IDENTIFIER
            + "|--[^\\r\\n]*(?:\\r\\n?|\\n|$)|(?s:/\\*.*?\\*/)"
            + "|(?<keyword>\\b(?:TABLE|INDEX|ON|REFERENCES)\\b)(?<separator>\\s+)"
            + "(?<qualifier>" + IDENTIFIER + "\\s*\\.\\s*)(?=" + IDENTIFIER + ")|(?<space>\\s+)",
        Pattern.CASE_INSENSITIVE
    );

    @Nullable
    public static String getDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject object,
        @NotNull DamengConstants.ObjectType objectType,
        @Nullable String schema
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Load source code for " + objectType + " '" + object.getName() + "'")) {
            return getDDL(session, objectType, object.getName(), schema);
        } catch (SQLException e) {
            throw new DBException("Load source code for " + objectType + " '" + object.getName() + "' failed", e);
        }
    }

    @Nullable
    private static String getDDL(
        @NotNull JDBCSession session,
        @NotNull DamengConstants.ObjectType objectType,
        @NotNull String name,
        @Nullable String schema
    ) throws SQLException {
        try (JDBCPreparedStatement statement = session.prepareStatement("SELECT DBMS_METADATA.GET_DDL(?,?,?)")) {
            statement.setString(1, objectType.name());
            statement.setString(2, name);
            statement.setString(3, schema);
            try (JDBCResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    @NotNull
    static String getTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase table,
        @NotNull String schema,
        @NotNull Map<String, Object> options
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Read Dameng table DDL")) {
            String tableDDL = getNativeTableDDL(session, table.getName(), schema);
            if (tableDDL == null || tableDDL.isBlank()) {
                Map<String, Object> fallbackOptions = new HashMap<>(options);
                fallbackOptions.put(DBPScriptObject.OPTION_SKIP_INDEXES, true);
                tableDDL = DBStructUtils.generateTableDDL(monitor, table, fallbackOptions, false);
            } else {
                tableDDL = formatDDL(tableDDL, options);
            }
            StringBuilder ddl = new StringBuilder(tableDDL.length());
            DBStructUtils.addDDLLine(ddl, tableDDL);
            appendTableComments(session, table, schema, options, ddl);
            if (!CommonUtils.getOption(options, DBPScriptObject.OPTION_SKIP_INDEXES)) {
                appendTableIndexes(session, table.getName(), schema, options, ddl);
            }
            return ddl.toString().stripTrailing();
        } catch (SQLException e) {
            throw new DBException("Load source code for table '" + table.getName() + "' failed", e);
        }
    }

    @Nullable
    private static String getNativeTableDDL(
        @NotNull JDBCSession session,
        @NotNull String table,
        @NotNull String schema
    ) throws SQLException {
        try {
            return getDDL(session, DamengConstants.ObjectType.TABLE, table, schema);
        } catch (SQLException e) {
            if (e.getErrorCode() == ERROR_UNRESOLVED_MEMBER) {
                try (JDBCPreparedStatement statement = session.prepareStatement("SELECT SF_CHECK_SYSTEM_PACKAGE('DBMS_METADATA')");
                     JDBCResultSet result = statement.executeQuery()) {
                    if (result.next() && "0".equals(result.getString(1))) {
                        return null;
                    }
                } catch (SQLException checkError) {
                    e.addSuppressed(checkError);
                }
            }
            throw e;
        }
    }

    private static void appendTableComments(
        @NotNull JDBCSession session,
        @NotNull GenericTableBase table,
        @NotNull String schema,
        @NotNull Map<String, Object> options,
        @NotNull StringBuilder ddl
    ) throws SQLException {
        String query = """
            SELECT NULL AS COLUMN_NAME, COMMENT$ FROM SYS.SYSTABLECOMMENTS
            WHERE SCHNAME = ? AND TVNAME = ? AND TABLE_TYPE = 'TABLE' AND COMMENT$ IS NOT NULL
            UNION ALL
            SELECT COLNAME, COMMENT$ FROM SYS.SYSCOLUMNCOMMENTS
            WHERE SCHNAME = ? AND TVNAME = ? AND TABLE_TYPE = 'TABLE' AND COMMENT$ IS NOT NULL
            ORDER BY COLUMN_NAME NULLS FIRST
            """;
        var dataSource = table.getDataSource();
        String tableName = DBUtils.getQuotedIdentifier(dataSource, table.getName(), true, true);
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true)) {
            tableName = DBUtils.getQuotedIdentifier(dataSource, schema, true, true) + "." + tableName;
        }
        try (JDBCPreparedStatement statement = session.prepareStatement(query)) {
            statement.setString(1, schema);
            statement.setString(2, table.getName());
            statement.setString(3, schema);
            statement.setString(4, table.getName());
            try (JDBCResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String columnName = result.getString(1);
                    String comment = SQLUtils.quoteString(table, result.getString(2));
                    String statementPrefix = columnName == null
                        ? "COMMENT ON TABLE " + tableName
                        : "COMMENT ON COLUMN " + tableName + "."
                            + DBUtils.getQuotedIdentifier(dataSource, columnName, true, true);
                    DBStructUtils.addDDLLine(ddl, statementPrefix + " IS " + comment);
                }
            }
        }
    }

    private static void appendTableIndexes(
        @NotNull JDBCSession session,
        @NotNull String table,
        @NotNull String schema,
        @NotNull Map<String, Object> options,
        @NotNull StringBuilder ddl
    ) throws SQLException {
        // bit 0 marks system-generated indexes, including those backing primary and unique keys.
        String query = """
            SELECT INDEXDEF(info.ID, 1)
            FROM SYS.SYSOBJECTS tab
            JOIN SYS.SYSOBJECTS sch ON sch.ID = tab.SCHID
            JOIN SYS.SYSOBJECTS idx ON idx.PID = tab.ID AND idx.SUBTYPE$ = 'INDEX'
            JOIN SYS.SYSINDEXES info ON info.ID = idx.ID
            WHERE sch.NAME = ? AND tab.NAME = ? AND tab.SUBTYPE$ = 'UTAB'
                AND info.FLAG & 0x01 = 0
            ORDER BY idx.NAME
            """;
        try (JDBCPreparedStatement statement = session.prepareStatement(query)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (JDBCResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String indexDDL = result.getString(1);
                    if (indexDDL != null) {
                        DBStructUtils.addDDLLine(ddl, formatDDL(indexDDL, options));
                    }
                }
            }
        }
    }

    @NotNull
    private static String formatDDL(@NotNull String ddl, @NotNull Map<String, Object> options) {
        boolean qualified = CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true);
        boolean compact = CommonUtils.getOption(options, DBPScriptObject.OPTION_SCRIPT_FORMAT_COMPACT);
        if (qualified && !compact) {
            return ddl;
        }

        // consume quoted text and comments before looking for schema prefixes or whitespace.
        Matcher parts = DDL_PARTS.matcher(ddl);
        StringBuilder formatted = new StringBuilder(ddl.length());
        while (parts.find()) {
            String keyword = parts.group("keyword");
            if (keyword != null) {
                String separator = compact ? " " : parts.group("separator");
                String qualifier = qualified ? parts.group("qualifier") : "";
                parts.appendReplacement(formatted, Matcher.quoteReplacement(keyword + separator + qualifier));
            } else if (compact && parts.group("space") != null) {
                parts.appendReplacement(formatted, " ");
            }
        }
        return parts.appendTail(formatted).toString();
    }

    @Nullable
    public static <OWNER extends DBSObject, OBJECT extends DBPObjectWithLongId & DBSObject> OBJECT getObjectById(
            @NotNull DBRProgressMonitor monitor,
            @NotNull AbstractObjectCache<OWNER, OBJECT> cache,
            @NotNull OWNER owner,
            long objectId)
            throws DBException {
        for (OBJECT object : cache.getAllObjects(monitor, owner)) {
            if (object.getObjectId() == objectId) {
                return object;
            }
        }
        return null;
    }

}
