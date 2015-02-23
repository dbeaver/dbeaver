/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.dbeaver.DBeaverConstants;

/**
 * SQL editor constants
 */
public class SQLConstants {

    public static final String NULL_VALUE = "NULL";

    public final static String SQL_CONTENT_TYPE = "org.jkiss.dbeaver.sql";

    public final static String SQL_COMMENT = "sql_comment";

    public static final String SHORT_MESSAGE = "short_message"; //$NON-NLS-1$

    /**
     * Marker type contant for SQL portability targets.
     */
    public static final String PORTABILITY_MARKER_TYPE = DBeaverConstants.PLUGIN_ID + ".portabilitytask";           //$NON-NLS-1$
    /**
     * Marker type contant for SQL syntax errors.
     */
    public static final String SYNTAX_MARKER_TYPE      = DBeaverConstants.PLUGIN_ID + ".syntaxproblem";             //$NON-NLS-1$

    public static final String STR_QUOTE_SINGLE = "'";
    public static final String STR_QUOTE_DOUBLE = "\"";
    public static final String ML_COMMENT_START = "/*";
    public static final String ML_COMMENT_END = "*/";

    public static final String[] TABLE_KEYWORDS = {
        "FROM",
        "UPDATE",
        "INTO",
        "TABLE",
        "JOIN"
    }; //$NON-NLS$
    public static final String[] COLUMN_KEYWORDS = {
        "SELECT",
        "WHERE",
        "SET",
        "ON",
        "AND",
        "OR",
        "BY",
        "HAVING"
    };
    public static final String[] SQL92_KEYWORDS = {
        "ABSOLUTE",
        "ACTION",
        "ADA",
        "ADD",
        "ALL",
        "ALLOCATE",
        "ALTER",
        "AND",
        "ANY",
        "ARE",
        "AS",
        "ASC",
        "ASSERTION",
        "AT",
        "AUTHORIZATION",
        "AVG",
        "BEGIN",
        "BETWEEN",
        "BIT",
        "BIT_LENGTH",
        "BOTH",
        "BY",
        "CASCADE",
        "CASCADED",
        "CASE",
        "CAST",
        "CATALOG",
        "CHAR",
        "CHAR_LENGTH",
        "CHARACTER",
        "CHARACTER_LENGTH",
        "CHECK",
        "CLOSE",
        "COALESCE",
        "COLLATE",
        "COLLATION",
        "COLUMN",
        "COMMIT",
        "CONNECT",
        "CONNECTION",
        "CONSTRAINT",
        "CONSTRAINTS",
        "CONTINUE",
        "CONVERT",
        "CORRESPONDING",
        "COUNT",
        "CREATE",
        "CROSS",
        "CURRENT",
        "CURRENT_DATE",
        "CURRENT_TIME",
        "CURRENT_TIMESTAMP",
        "CURRENT_USER",
        "CURSOR",
        "DATE",
        "DAY",
        "DEALLOCATE",
        "DEC",
        "DECIMAL",
        "DECLARE",
        "DEFAULT",
        "DEFERRABLE",
        "DEFERRED",
        "DELETE",
        "DESC",
        "DESCRIBE",
        "DESCRIPTOR",
        "DIAGNOSTICS",
        "DISCONNECT",
        "DISTINCT",
        "DOMAIN",
        "DOUBLE",
        "DROP",
        "ELSE",
        "END",
        "END-EXEC",
        "ESCAPE",
        "EXCEPT",
        "EXCEPTION",
        "EXEC",
        "EXECUTE",
        "EXISTS",
        "EXTERNAL",
        "EXTRACT",
        "FALSE",
        "FETCH",
        "FIRST",
        "FLOAT",
        "FOR",
        "FOREIGN",
        "FORTRAN",
        "FOUND",
        "FROM",
        "FULL",
        "GET",
        "GLOBAL",
//        "GO",
        "GOTO",
        "GRANT",
        "GROUP",
        "HAVING",
        "HOUR",
        "IDENTITY",
        "IMMEDIATE",
        "IN",
        "INCLUDE",
        "INDEX",
        "INDICATOR",
        "INITIALLY",
        "INNER",
        "INPUT",
        "INSENSITIVE",
        "INSERT",
        "INT",
        "INTEGER",
        "INTERSECT",
        "INTERVAL",
        "INTO",
        "IS",
        "ISOLATION",
        "JOIN",
        "KEY",
        "LANGUAGE",
        "LAST",
        "LEADING",
        "LEFT",
        "LEVEL",
        "LIKE",
        "LOCAL",
        "LOWER",
        "MATCH",
        "MAX",
        "MIN",
        "MINUTE",
        "MODULE",
        "MONTH",
        "NAMES",
        "NATIONAL",
        "NATURAL",
        "NCHAR",
        "NEXT",
        "NO",
        "NONE",
        "NOT",
        "NULL",
        "NULLIF",
        "NUMERIC",
        "OCTET_LENGTH",
        "OF",
        "ON",
        "ONLY",
        "OPEN",
        "OPTION",
        "OR",
        "ORDER",
        "OUTER",
        "OUTPUT",
        "OVERLAPS",
        "PAD",
        "PARTIAL",
        "PASCAL",
        "POSITION",
        "PRECISION",
        "PREPARE",
        "PRESERVE",
        "PRIMARY",
        "PRIOR",
        "PRIVILEGES",
        "PROCEDURE",
//        "PUBLIC",
        "READ",
        "REAL",
        "REFERENCES",
        "RELATIVE",
        "RESTRICT",
        "REVOKE",
        "RIGHT",
        "ROLLBACK",
        "ROWS",
        "SCHEMA",
        "SCROLL",
        "SECOND",
        "SECTION",
        "SELECT",
        "SESSION",
        "SESSION_USER",
        "SET",
        "SIZE",
        "SMALLINT",
        "SOME",
        "SPACE",
        "SQL",
        "SQLCA",
        "SQLCODE",
        "SQLERROR",
        "SQLSTATE",
        "SQLWARNING",
        "SUBSTRING",
        "SUM",
        "SYSTEM_USER",
        "TABLE",
        "TEMPORARY",
        "THEN",
        "TIME",
        "TIMESTAMP",
        "TIMEZONE_HOUR",
        "TIMEZONE_MINUTE",
        "TO",
        "TRAILING",
        "TRANSACTION",
        "TRANSLATE",
        "TRANSLATION",
        "TRIM",
        "TRUE",
        "UNION",
        "UNIQUE",
        "UNKNOWN",
        "UPDATE",
        "UPPER",
        "USAGE",
        "USER",
        "USING",
        "VALUE",
        "VALUES",
        "VARCHAR",
        "VARYING",
        "VIEW",
        "WHEN",
        "WHENEVER",
        "WHERE",
        "WITH",
        "WORK",
        "WRITE",
        "YEAR",
        "ZONE",
    };
    public static final String[] SQL_EX_KEYWORDS = {
        "CHANGE",
        "MODIFY",
    };
    public static final String[] DEFAULT_TYPES = {
        "CHAR",
        "VARCHAR",
        "VARBINARY",
        "INTEGER",
        "FLOAT",
        "DATE",
        "TIME",
        "TIMESTAMP",
        "CLOB",
        "BLOB",
    };

    public static final String BLOCK_BEGIN = "BEGIN";
    public static final String BLOCK_END = "END";

    /**
     * Pseudo variables - these are not dynamic parameters
     */
    public static final String[] PSEUDO_VARIABLES = {
        ":NEW",
        ":OLD",
    };
    public static final char STRUCT_SEPARATOR = '.'; //$NON-NLS-1$
    public static final String DEFAULT_STATEMENT_DELIMITER = ";";
    public static final String CONFIG_COLOR_KEYWORD = "org.jkiss.dbeaver.sql.editor.color.keyword.foreground";
    public static final String CONFIG_COLOR_DATATYPE = "org.jkiss.dbeaver.sql.editor.color.datatype.foreground";
    public static final String CONFIG_COLOR_STRING = "org.jkiss.dbeaver.sql.editor.color.string.foreground";
    public static final String CONFIG_COLOR_NUMBER = "org.jkiss.dbeaver.sql.editor.color.number.foreground";
    public static final String CONFIG_COLOR_COMMENT = "org.jkiss.dbeaver.sql.editor.color.comment.foreground";
    public static final String CONFIG_COLOR_DELIMITER = "org.jkiss.dbeaver.sql.editor.color.delimiter.foreground";
    public static final String CONFIG_COLOR_PARAMETER = "org.jkiss.dbeaver.sql.editor.color.parameter.foreground";
    public static final String CONFIG_COLOR_TEXT = "org.jkiss.dbeaver.sql.editor.color.text.foreground";
    public static final String CONFIG_COLOR_BACKGROUND = "org.jkiss.dbeaver.sql.editor.color.text.background";
    public static final String CONFIG_COLOR_DISABLED = "org.jkiss.dbeaver.sql.editor.color.disabled.background";
    public static final String CONFIG_FONT_OUTPUT = "org.jkiss.dbeaver.sql.editor.font.output";

}
