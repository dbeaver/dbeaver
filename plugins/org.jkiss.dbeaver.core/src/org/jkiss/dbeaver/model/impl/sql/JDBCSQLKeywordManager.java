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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLKeywordManager;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Keyword manager.
 * <p/>
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class JDBCSQLKeywordManager implements SQLKeywordManager {
    static final Log log = LogFactory.getLog(JDBCSQLKeywordManager.class);

    private TreeMap<String, DBPKeywordType> allKeywords = new TreeMap<String, DBPKeywordType>();

    private TreeSet<String> reservedWords = new TreeSet<String>();
    private TreeSet<String> functions = new TreeSet<String>();
    private TreeSet<String> types = new TreeSet<String>();
    private TreeSet<String> tableQueryWords = new TreeSet<String>();
    private TreeSet<String> columnQueryWords = new TreeSet<String>();

    private Pair<String, String> multiLineComments = new Pair<String, String>(SQLConstants.ML_COMMENT_START, SQLConstants.ML_COMMENT_END);
    private String[] singleLineComments = {"--"};

    public JDBCSQLKeywordManager(SQLDataSource dataSource)
    {
        loadSyntax(dataSource);
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

    void loadSyntax(final SQLDataSource dataSource)
    {
        SQLDialect sqlDialect = dataSource.getSQLDialect();
        allKeywords.clear();
        reservedWords.clear();
        functions.clear();
        types.clear();

        // Add default set of keywords
        Collections.addAll(reservedWords, SQLConstants.SQL92_KEYWORDS);
        Collections.addAll(reservedWords, SQLConstants.SQL_EX_KEYWORDS);
        Collections.addAll(tableQueryWords, SQLConstants.TABLE_KEYWORDS);
        Collections.addAll(columnQueryWords, SQLConstants.COLUMN_KEYWORDS);

        try {
            // Keywords
            Collection<String> sqlKeywords = sqlDialect.getSQLKeywords();
            if (!CommonUtils.isEmpty(sqlKeywords)) {
                for (String keyword : sqlKeywords) {
                    reservedWords.add(keyword.toUpperCase());
                }
            }
            final Collection<String> executeKeywords = sqlDialect.getExecuteKeywords();
            if (!CommonUtils.isEmpty(executeKeywords)) {
                for (String keyword : executeKeywords) {
                    reservedWords.add(keyword.toUpperCase());
                }
            }

            // Functions
            Set<String> allFunctions = new HashSet<String>();
            if (sqlDialect.getNumericFunctions() != null) {
                for (String func : sqlDialect.getNumericFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            if (sqlDialect.getStringFunctions() != null) {
                for (String func : sqlDialect.getStringFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            if (sqlDialect.getSystemFunctions() != null) {
                for (String func : sqlDialect.getSystemFunctions()) {
                    allFunctions.add(func.toUpperCase());
                }
            }
            if (sqlDialect.getTimeDateFunctions() != null) {
                for (String func : sqlDialect.getTimeDateFunctions()) {
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
                Collections.addAll(types, SQLConstants.DEFAULT_TYPES);
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

}
