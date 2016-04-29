/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLStateType;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.Pair;

import java.util.*;

/**
 * Basic SQL Dialect
 */
public class BasicSQLDialect implements SQLDialect {

    public static final BasicSQLDialect INSTANCE = new BasicSQLDialect();

    private static final String[] DEFAULT_LINE_COMMENTS = {"--"};

    // Keywords
    private TreeMap<String, DBPKeywordType> allKeywords = new TreeMap<>();

    protected final TreeSet<String> reservedWords = new TreeSet<>();
    private final TreeSet<String> functions = new TreeSet<>();
    protected final TreeSet<String> types = new TreeSet<>();
    protected final TreeSet<String> tableQueryWords = new TreeSet<>();
    protected final TreeSet<String> columnQueryWords = new TreeSet<>();
    // Comments
    protected Pair<String, String> multiLineComments = new Pair<>(SQLConstants.ML_COMMENT_START, SQLConstants.ML_COMMENT_END);

    protected BasicSQLDialect()
    {
        loadStandardKeywords();
    }

    @NotNull
    @Override
    public String getDialectName() {
        return "SQL";
    }

    @Nullable
    @Override
    public String getIdentifierQuoteString()
    {
        return "\"";
    }

    @NotNull
    @Override
    public Collection<String> getExecuteKeywords() {
        return Collections.emptyList();
    }

    public void addSQLKeyword(String keyword)
    {
        reservedWords.add(keyword);
        allKeywords.put(keyword, DBPKeywordType.KEYWORD);
    }

    public void removeSQLKeyword(String keyword)
    {
        reservedWords.remove(keyword);
        allKeywords.remove(keyword);
    }

    protected void addFunctions(Collection<String> allFunctions) {
        functions.addAll(allFunctions);
        addKeywords(allFunctions, DBPKeywordType.FUNCTION);
    }

    public void addKeywords(Collection<String> set, DBPKeywordType type)
    {
        if (set != null) {
            for (String keyword : set) {
                reservedWords.add(keyword);
                DBPKeywordType oldType = allKeywords.get(keyword);
                if (oldType != DBPKeywordType.KEYWORD) {
                    // We can't mark keywords as functions or types because keywords are reserved and
                    // if some identifier conflicts with keyword it must be quoted.
                    allKeywords.put(keyword, type);
                }
            }
        }
    }

    @NotNull
    @Override
    public Set<String> getReservedWords()
    {
        return reservedWords;
    }

    @NotNull
    @Override
    public Set<String> getFunctions(@NotNull DBPDataSource dataSource)
    {
        return functions;
    }

    @NotNull
    @Override
    public TreeSet<String> getDataTypes(@NotNull DBPDataSource dataSource)
    {
        return types;
    }

    @Override
    public DBPKeywordType getKeywordType(@NotNull String word)
    {
        return allKeywords.get(word.toUpperCase(Locale.ENGLISH));
    }

    @NotNull
    @Override
    public List<String> getMatchedKeywords(@NotNull String word)
    {
        word = word.toUpperCase();
        List<String> result = new ArrayList<>();
        for (String keyword : allKeywords.tailMap(word).keySet()) {
            if (keyword.startsWith(word)) {
                result.add(keyword);
            } else {
                break;
            }
        }
        return result;
    }

    @Override
    public boolean isKeywordStart(@NotNull String word)
    {
        SortedMap<String, DBPKeywordType> map = allKeywords.tailMap(word);
        return !map.isEmpty() && map.firstKey().startsWith(word);
    }

    @Override
    public boolean isEntityQueryWord(@NotNull String word)
    {
        return tableQueryWords.contains(word.toUpperCase());
    }

    @Override
    public boolean isAttributeQueryWord(@NotNull String word)
    {
        return columnQueryWords.contains(word.toUpperCase());
    }

    @NotNull
    @Override
    public String getSearchStringEscape()
    {
        return "%";
    }

    @Override
    public int getCatalogUsage()
    {
        return USAGE_NONE;
    }

    @Override
    public int getSchemaUsage()
    {
        return USAGE_NONE;
    }

    @NotNull
    @Override
    public String getCatalogSeparator()
    {
        return String.valueOf(SQLConstants.STRUCT_SEPARATOR);
    }

    @Override
    public char getStructSeparator()
    {
        return SQLConstants.STRUCT_SEPARATOR;
    }

    @Override
    public boolean isCatalogAtStart()
    {
        return true;
    }

    @NotNull
    @Override
    public SQLStateType getSQLStateType()
    {
        return SQLStateType.SQL99;
    }

    @NotNull
    @Override
    public String getScriptDelimiter()
    {
        return ";"; //$NON-NLS-1$
    }

    @Nullable
    @Override
    public String getScriptDelimiterRedefiner() {
        return null;
    }

    @Nullable
    @Override
    public String getBlockToggleString() {
        return null;
    }

    @Override
    public boolean validUnquotedCharacter(char c)
    {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_';
    }

    @Override
    public boolean supportsUnquotedMixedCase()
    {
        return true;
    }

    @Override
    public boolean supportsQuotedMixedCase()
    {
        return true;
    }

    @NotNull
    @Override
    public DBPIdentifierCase storesUnquotedCase()
    {
        return DBPIdentifierCase.UPPER;
    }

    @NotNull
    @Override
    public DBPIdentifierCase storesQuotedCase()
    {
        return DBPIdentifierCase.MIXED;
    }

    @NotNull
    @Override
    public String escapeString(String string) {
        return string.replace("'", "''");
    }

    @NotNull
    @Override
    public String escapeScriptValue(DBSAttributeBase attribute, @NotNull Object value, @NotNull String strValue) {
        if (value instanceof UUID) {
            return '\'' + strValue + '\'';
        }
        return strValue;
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.NOT_SUPPORTED;
    }

    @Override
    public String addFiltersToQuery(DBPDataSource dataSource, String query, DBDDataFilter filter) throws DBException {
        return SQLSemanticProcessor.addFiltersToQuery(dataSource, query, filter);
    }

    @Override
    public boolean supportsSubqueries()
    {
        return true;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsAliasInUpdate() {
        return false;
    }

    @Override
    public boolean supportsCommentQuery() {
        return false;
    }

    @Override
    public Pair<String, String> getMultiLineComments()
    {
        return multiLineComments;
    }

    @Override
    public String[] getSingleLineComments()
    {
        return DEFAULT_LINE_COMMENTS;
    }

    @Override
    public boolean isDelimiterAfterBlock() {
        return false;
    }

    protected void loadStandardKeywords()
    {
        // Add default set of keywords
        Set<String> all = new HashSet<>();
        Collections.addAll(all, SQLConstants.SQL2003_RESERVED_KEYWORDS);
        //Collections.addAll(reservedWords, SQLConstants.SQL2003_NON_RESERVED_KEYWORDS);
        Collections.addAll(all, SQLConstants.SQL_EX_KEYWORDS);
        Collections.addAll(functions, SQLConstants.SQL2003_FUNCTIONS);
        Collections.addAll(tableQueryWords, SQLConstants.TABLE_KEYWORDS);
        Collections.addAll(columnQueryWords, SQLConstants.COLUMN_KEYWORDS);

        final Collection<String> executeKeywords = getExecuteKeywords();
        addKeywords(executeKeywords, DBPKeywordType.KEYWORD);

        // Add default types
        Collections.addAll(types, SQLConstants.DEFAULT_TYPES);

        addKeywords(all, DBPKeywordType.KEYWORD);
        addKeywords(types, DBPKeywordType.TYPE);
        addKeywords(functions, DBPKeywordType.FUNCTION);
    }

}
