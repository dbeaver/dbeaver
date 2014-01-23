/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLStateType;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.utils.Pair;

import java.util.*;

/**
 * Basic SQL Dialect
 */
public class BasicSQLDialect implements SQLDialect {

    static final Log log = LogFactory.getLog(BasicSQLDialect.class);

    // Keywords
    private TreeMap<String, DBPKeywordType> allKeywords = new TreeMap<String, DBPKeywordType>();

    protected final TreeSet<String> reservedWords = new TreeSet<String>();
    protected final TreeSet<String> functions = new TreeSet<String>();
    protected final TreeSet<String> types = new TreeSet<String>();
    protected final TreeSet<String> tableQueryWords = new TreeSet<String>();
    protected final TreeSet<String> columnQueryWords = new TreeSet<String>();
    // Comments
    private Pair<String, String> multiLineComments = new Pair<String, String>(SQLConstants.ML_COMMENT_START, SQLConstants.ML_COMMENT_END);
    private String[] singleLineComments = {"--"};

    public BasicSQLDialect()
    {
        loadKeywords();
    }

    @Override
    public String getDialectName() {
        return "SQL";
    }

    @Override
    public String getIdentifierQuoteString()
    {
        return "\\";
    }

    @Nullable
    @Override
    public Collection<String> getExecuteKeywords() {
        return null;
    }

    public void addSQLKeyword(String keyword)
    {
        reservedWords.add(keyword);
        allKeywords.put(keyword, DBPKeywordType.KEYWORD);
    }

    public void addKeywords(Set<String> set, DBPKeywordType type)
    {
        for (String keyword : set) {
            allKeywords.put(keyword, type);
        }
    }

    @Override
    public Set<String> getReservedWords()
    {
        return reservedWords;
    }

    @Override
    public Set<String> getFunctions()
    {
        return functions;
    }

    @Override
    public TreeSet<String> getTypes()
    {
        return types;
    }

    @Override
    public DBPKeywordType getKeywordType(String word)
    {
        return allKeywords.get(word.toUpperCase());
    }

    @Override
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

    @Override
    public boolean isKeywordStart(String word)
    {
        SortedMap<String, DBPKeywordType> map = allKeywords.tailMap(word);
        return !map.isEmpty() && map.firstKey().startsWith(word);
    }

    @Override
    public boolean isEntityQueryWord(String word)
    {
        return tableQueryWords.contains(word.toUpperCase());
    }

    @Override
    public boolean isAttributeQueryWord(String word)
    {
        return columnQueryWords.contains(word.toUpperCase());
    }

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

    @Override
    public SQLStateType getSQLStateType()
    {
        return SQLStateType.SQL99;
    }

    @Override
    public String getScriptDelimiter()
    {
        return ";"; //$NON-NLS-1$
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

    @Override
    public DBPIdentifierCase storesUnquotedCase()
    {
        return DBPIdentifierCase.UPPER;
    }

    @Override
    public DBPIdentifierCase storesQuotedCase()
    {
        return DBPIdentifierCase.MIXED;
    }

    @Override
    public boolean supportsSubqueries()
    {
        return true;
    }

    @Override
    public Pair<String, String> getMultiLineComments()
    {
        return multiLineComments;
    }

    @Override
    public String[] getSingleLineComments()
    {
        return singleLineComments;
    }

    void loadKeywords()
    {
        // Add default set of keywords
        Collections.addAll(reservedWords, SQLConstants.SQL92_KEYWORDS);
        Collections.addAll(reservedWords, SQLConstants.SQL_EX_KEYWORDS);
        Collections.addAll(tableQueryWords, SQLConstants.TABLE_KEYWORDS);
        Collections.addAll(columnQueryWords, SQLConstants.COLUMN_KEYWORDS);

        final Collection<String> executeKeywords = getExecuteKeywords();
        if (executeKeywords != null) {
            for (String keyword : executeKeywords) {
                reservedWords.add(keyword.toUpperCase());
            }
        }

        // Add default types
        Collections.addAll(types, SQLConstants.DEFAULT_TYPES);

        addKeywords(reservedWords, DBPKeywordType.KEYWORD);
        addKeywords(types, DBPKeywordType.TYPE);
    }

}
