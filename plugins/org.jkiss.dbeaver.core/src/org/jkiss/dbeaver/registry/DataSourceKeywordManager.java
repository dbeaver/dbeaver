/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Keyword manager.
 * <p/>
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class DataSourceKeywordManager implements DBPKeywordManager {
    static final Log log = LogFactory.getLog(DataSourceKeywordManager.class);

    private TreeMap<String, DBPKeywordType> allKeywords = new TreeMap<String, DBPKeywordType>();

    private TreeSet<String> reservedWords = new TreeSet<String>();
    private TreeSet<String> functions = new TreeSet<String>();
    private TreeSet<String> types = new TreeSet<String>();
    private TreeSet<String> tableQueryWords = new TreeSet<String>();
    private TreeSet<String> columnQueryWords = new TreeSet<String>();

    private String[] singleLineComments = {"--"};

    public DataSourceKeywordManager(DBPDataSource dataSource)
    {
        loadSyntax(dataSource);
    }

    public Set<String> getReservedWords()
    {
        return reservedWords;
    }

    public Set<String> getFunctions()
    {
        return functions;
    }

    public TreeSet<String> getTypes()
    {
        return types;
    }

    public DBPKeywordType getKeywordType(String word)
    {
        return allKeywords.get(word);
    }

    public List<String> getMatchedKeywords(String word)
    {
        word = word.toUpperCase();
        List<String> result = new ArrayList<String>();
        for (String keyword : allKeywords.tailMap(word).keySet()) {
            if (keyword.startsWith(word)) {
                result.add(keyword);
            } else {
                break;
            }
        }
        return result;
    }

    public boolean isKeywordStart(String word)
    {
        SortedMap<String, DBPKeywordType> map = allKeywords.tailMap(word);
        return !map.isEmpty() && map.firstKey().startsWith(word);
    }

    public boolean isTableQueryWord(String word)
    {
        return tableQueryWords.contains(word.toUpperCase());
    }

    public boolean isColumnQueryWord(String word)
    {
        return columnQueryWords.contains(word.toUpperCase());
    }

    void loadSyntax(final DBPDataSource dataSource)
    {
        DBPDataSourceInfo dataSourceInfo = dataSource.getInfo();
        allKeywords.clear();
        reservedWords.clear();
        functions.clear();
        types.clear();

        // Add default set of keywords
        Collections.addAll(reservedWords, SQL92_KEYWORDS);
        Collections.addAll(reservedWords, SQL_EX_KEYWORDS);
        Collections.addAll(tableQueryWords, TABLE_KEYWORDS);
        Collections.addAll(columnQueryWords, COLUMN_KEYWORDS);

        try {
            // Keywords
            Collection<String> sqlKeywords = dataSourceInfo.getSQLKeywords();
            if (!CommonUtils.isEmpty(sqlKeywords)) {
                for (String keyword : sqlKeywords) {
                    reservedWords.add(keyword.toUpperCase());
                }
            }
            final Collection<String> executeKeywords = dataSourceInfo.getExecuteKeywords();
            if (!CommonUtils.isEmpty(executeKeywords)) {
                for (String keyword : executeKeywords) {
                    reservedWords.add(keyword.toUpperCase());
                }
            }

            // Functions
            Set<String> allFunctions = new HashSet<String>();
            if (dataSourceInfo.getNumericFunctions() != null) {
                for (String func : dataSourceInfo.getNumericFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            if (dataSourceInfo.getStringFunctions() != null) {
                for (String func : dataSourceInfo.getStringFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            if (dataSourceInfo.getSystemFunctions() != null) {
                for (String func : dataSourceInfo.getSystemFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            if (dataSourceInfo.getTimeDateFunctions() != null) {
                for (String func : dataSourceInfo.getTimeDateFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            functions.addAll(allFunctions);

            // Types
            if (dataSource instanceof DBPDataTypeProvider) {
                Collection<? extends DBSDataType> supportedDataTypes = ((DBPDataTypeProvider)dataSource).getDataTypes();
                if (supportedDataTypes != null) {
                    for (DBSDataType dataType : supportedDataTypes) {
                        types.add(dataType.getName().toUpperCase());
                    }
                }
            }
            if (types.isEmpty()) {
                // Add default types
                Collections.addAll(types, DEFAULT_TYPES);
            }

            functions.addAll(allFunctions);
        }
        catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException)e).getTargetException();
            }
            log.error(e);
        }

        // Remove types and functions from reserved words list
        reservedWords.removeAll(types);
        reservedWords.removeAll(functions);

        addKeywords(reservedWords, DBPKeywordType.KEYWORD);
        addKeywords(functions, DBPKeywordType.FUNCTION);
        addKeywords(types, DBPKeywordType.TYPE);
    }

    private void addKeywords(Set<String> set, DBPKeywordType type)
    {
        for (String keyword : set) {
            allKeywords.put(keyword, type);
        }
    }

    public String[] getSingleLineComments()
    {
        return singleLineComments;
    }

    private static final String[] TABLE_KEYWORDS = {
        "FROM",
        "UPDATE",
        "INTO",
        "TABLE"
    };

    private static final String[] COLUMN_KEYWORDS = {
        "SELECT",
        "WHERE",
        "SET",
        "ON",
        "AND",
        "OR"
    };

    private static final String[] SQL92_KEYWORDS = {
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
        "GO",
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

    private static final String[] SQL_EX_KEYWORDS = {
        "CHANGE",
        "MODIFY",
    };

    private static final String[] DEFAULT_TYPES = {
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

}
