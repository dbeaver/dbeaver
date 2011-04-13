/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPKeywordManager;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.*;

/**
 * Keyword manager.
 * <p/>
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class DataSourceKeywordManager implements DBPKeywordManager {

    private TreeMap<String, DBPKeywordType> allKeywords = new TreeMap<String, DBPKeywordType>();

    private TreeSet<String> reservedWords = new TreeSet<String>();
    private TreeSet<String> functions = new TreeSet<String>();
    private TreeSet<String> types = new TreeSet<String>();
    private TreeSet<String> tableQueryWords = new TreeSet<String>();
    private TreeSet<String> columnQueryWords = new TreeSet<String>();

    private String[] singleLineComments = {"--"};

    public DataSourceKeywordManager(DBPDataSourceInfo info)
    {
        loadSyntax(info);
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

    void loadSyntax(DBPDataSourceInfo dataSourceInfo)
    {
        allKeywords.clear();
        reservedWords.clear();
        functions.clear();
        types.clear();

        // Add default set of keywords
        reservedWords.addAll(Arrays.asList(DEFAULT_KEYWORDS));
        types.addAll(Arrays.asList(DEFAULT_TYPES));
        tableQueryWords.addAll(Arrays.asList(TABLE_KEYWORDS));
        columnQueryWords.addAll(Arrays.asList(COLUMN_KEYWORDS));

        {
            // Keywords
            List<String> sqlKeywords = dataSourceInfo.getSQLKeywords();
            if (!CommonUtils.isEmpty(sqlKeywords)) {
                for (String keyword : sqlKeywords) {
                    reservedWords.add(keyword.toUpperCase());
                }
            }
            final List<String> executeKeywords = dataSourceInfo.getExecuteKeywords();
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
            List<DBSDataType> supportedDataTypes = dataSourceInfo.getSupportedDataTypes();
            if (supportedDataTypes != null) {
                for (DBSDataType dataType : dataSourceInfo.getSupportedDataTypes()) {
                    types.add(dataType.getName().toUpperCase());
                }
            }
            functions.addAll(allFunctions);
        }

        if (types.isEmpty()) {
            // Add default types
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

    private static final String[] DEFAULT_KEYWORDS = {
        "ALL",
        "ALTER",
        "AND",
        "ANY",
        "AS",
        "ASC",
        "AT",
        "AVG",
        "BEGIN",
        "BETWEEN",
        "BOTH",
        "BY",
        "CASE",
        "CAST",
        "CHECK",
        "COLUMN",
        "COMMIT",
        "CONNECT",
        "CONSTRAINT",
        "CONSTRAINTS",
        "CONTINUE",
        "COUNT",
        "CREATE",
        "CURSOR",
        "DATABASE",
        "DEFAULT",
        "DELETE",
        "DESC",
        "DISCONNECT",
        "DISTINCT",
        "DROP",
        "ELSE",
        "END",
        "END-EXEC",
        "EXECUTE",
        "EXISTS",
        "FOR",
        "FOREIGN",
        "FROM",
        "GRANT",
        "GROUP",
        "HAVING",
        "IN",
        "INDEX",
        "INNER",
        "INSERT",
        "INTERSECT",
        "INTO",
        "IS",
        "JOIN",
        "KEY",
        "LEFT",
        "LIKE",
        "MAX",
        "MIN",
        "NEXT",
        "NOT",
        "NULL",
        "OF",
        "ON",
        "OR",
        "ORDER",
        "OUTER",
        "PREPARE",
        "PRIMARY",
        "PROCEDURE",
        "REFERENCES",
        "REVOKE",
        "RIGHT",
        "ROLLBACK",
        "SELECT",
        "SET",
        "SUM",
        "TABLE",
        "TABLESPACE",
        "THEN",
        "TO",
        "UNION",
        "UNIQUE",
        "UPDATE",
        "USING",
        "VALUES",
        "VIEW",
        "WHEN",
        "WHERE",
        "WITH",
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
