/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLStateType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Basic SQL Dialect
 */
public class BasicSQLDialect extends AbstractSQLDialect implements RelationalSQLDialect {

    public static String DEFAULT_ID = "basic";
    public String id;

    public static final BasicSQLDialect INSTANCE = new BasicSQLDialect();

    private static final String[] DEFAULT_LINE_COMMENTS = {SQLConstants.SL_COMMENT};
    private static final String[] EXEC_KEYWORDS = new String[0];
    private static final String[] DDL_KEYWORDS = new String[]{
        "CREATE", "ALTER", "DROP"
    };

    private static final String[][] DEFAULT_BEGIN_END_BLOCK = new String[][]{
        {
            SQLConstants.BLOCK_BEGIN,
            SQLConstants.BLOCK_END
        }
    };

    protected static final String[] NON_TRANSACTIONAL_KEYWORDS = {
        SQLConstants.KEYWORD_SELECT,
        SQLConstants.KEYWORD_EXPLAIN,
        "DESCRIBE",
        "DESC",
        "USE",
        "SET",
        SQLConstants.KEYWORD_COMMIT,
        SQLConstants.KEYWORD_ROLLBACK
    };

    private static final String[] CORE_NON_TRANSACTIONAL_KEYWORDS = new String[]{
        SQLConstants.KEYWORD_SELECT,
    };

    public static final String[][] DEFAULT_IDENTIFIER_QUOTES = {{"\"", "\""}};
    public static final String[][] DEFAULT_STRING_QUOTES = {{"'", "'"}};

    private static final String[] COMMIT_KEYWORDS = { SQLConstants.KEYWORD_COMMIT };
    private static final String[] ROLLBACK_KEYWORDS = { SQLConstants.KEYWORD_ROLLBACK };

    protected BasicSQLDialect() {
        this("basic");
    }

    protected BasicSQLDialect(String id) {
        this.id = id;
        loadStandardKeywords();
    }

    @NotNull
    @Override
    public String getDialectId() {
        return id;
    }

    @NotNull
    @Override
    public String getDialectName() {
        return "SQL";
    }

    @Nullable
    @Override
    public String[][] getIdentifierQuoteStrings() {
        return DEFAULT_IDENTIFIER_QUOTES;
    }

    @NotNull
    @Override
    public String[][] getStringQuoteStrings() {
        return DEFAULT_STRING_QUOTES;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return super.getExecuteKeywords();
    }

    @NotNull
    @Override
    public String[] getDDLKeywords() {
        return super.getDDLKeywords();
    }

    @NotNull
    @Override
    public String getSearchStringEscape() {
        return "";
    }

    @Override
    public char getStringEscapeCharacter() {
        return 0;
    }

    @Override
    public int getCatalogUsage() {
        return USAGE_NONE;
    }

    @Override
    public int getSchemaUsage() {
        return USAGE_NONE;
    }

    @NotNull
    @Override
    public String getCatalogSeparator() {
        return String.valueOf(SQLConstants.STRUCT_SEPARATOR);
    }

    @Override
    public char getStructSeparator() {
        return SQLConstants.STRUCT_SEPARATOR;
    }

    @Override
    public boolean isCatalogAtStart() {
        return true;
    }

    @NotNull
    @Override
    public SQLStateType getSQLStateType() {
        return SQLStateType.SQL99;
    }

    @Nullable
    @Override
    public String getScriptDelimiterRedefiner() {
        return null;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return DEFAULT_BEGIN_END_BLOCK;
    }

    @Nullable
    @Override
    public String[] getBlockHeaderStrings() {
        return null;
    }

    @Override
    public boolean validIdentifierStart(char c) {
        return Character.isLetter(c);
    }

    @Override
    public boolean supportsUnquotedMixedCase() {
        return true;
    }

    @Override
    public boolean supportsQuotedMixedCase() {
        return true;
    }

    @NotNull
    @Override
    public DBPIdentifierCase storesUnquotedCase() {
        return DBPIdentifierCase.UPPER;
    }

    @NotNull
    @Override
    public DBPIdentifierCase storesQuotedCase() {
        return DBPIdentifierCase.MIXED;
    }

    @NotNull
    @Override
    public String escapeString(String string) {
        return string.replace("'", "''");
    }

    @NotNull
    @Override
    public String unEscapeString(String string) {
        return CommonUtils.notEmpty(string).replace("''", "'");
    }

    @NotNull
    @Override
    public String escapeScriptValue(DBSTypedObject attribute, @NotNull Object value, @NotNull String strValue) {
        if (value instanceof UUID) {
            return '\'' + escapeString(strValue) + '\'';
        }
        return strValue;
    }

    @Override
    public boolean supportsSubqueries() {
        return true;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return false;
    }

    @Override
    public boolean supportsAliasInUpdate() {
        return false;
    }

    @Override
    public boolean supportsTableDropCascade() {
        return false;
    }

    @Override
    public boolean supportsOrderByIndex() {
        return true;
    }

    @Override
    public boolean supportsOrderBy() {
        return true;
    }

    @Override
    public boolean supportsGroupBy() {
        return true;
    }

    @Override
    public boolean supportsCommentQuery() {
        return false;
    }

    @Override
    public boolean supportsNullability() {
        return true;
    }

    @Override
    public String[] getSingleLineComments() {
        return DEFAULT_LINE_COMMENTS;
    }

    @Override
    public boolean isDelimiterAfterQuery() {
        return false;
    }

    @Override
    public boolean isDelimiterAfterBlock() {
        return false;
    }

    @Nullable
    @Override
    public String[] getTransactionCommitKeywords() {
        return COMMIT_KEYWORDS;
    }

    @Nullable
    @Override
    public String[] getTransactionRollbackKeywords() {
        return ROLLBACK_KEYWORDS;
    }

    @Override
    protected boolean isTransactionModifyingKeyword(String firstKeyword) {
        // Handle "DO" separately
        return "DO".equals(firstKeyword) || super.isTransactionModifyingKeyword(firstKeyword);
    }

    @NotNull
    public String[] getDMLKeywords() {
        return isStandardSQL() ? getDescriptor().getDMLKeywords(true).toArray(new String[0]): new String[0];
    }

    @NotNull
    public String[] getNonTransactionKeywords() {
        return isStandardSQL() ? NON_TRANSACTIONAL_KEYWORDS : CORE_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    public boolean isQuoteReservedWords() {
        return true;
    }

    @Override
    public boolean isCRLFBroken() {
        return false;
    }

    @Override
    public boolean isStandardSQL() {
        return true;
    }

    @Override
    public boolean isAmbiguousCountBroken() {
        return false;
    }

    private void loadStandardKeywords() {
        // Add default set of keywords
        Set<String> all = new HashSet<>();
        if (isStandardSQL()) {
            all.addAll(getDescriptor().getReservedWords(true));
            functions.addAll(getDescriptor().getFunctions(true));
            Collections.addAll(tableQueryWords, SQLConstants.TABLE_KEYWORDS);
            Collections.addAll(columnQueryWords, SQLConstants.COLUMN_KEYWORDS);
        }

        for (String executeKeyword : getDescriptor().getExecuteKeywords(true)) {
            addSQLKeyword(executeKeyword);
            setKeywordIndent(executeKeyword, 1);
        }
        for (String ddlKeyword : getDescriptor().getDDLKeywords(true)) {
            addSQLKeyword(ddlKeyword);
            setKeywordIndent(ddlKeyword, 1);
        }
        for (String kw : tableQueryWords) {
            setKeywordIndent(kw, 1);
        }
        for (String kw : columnQueryWords) {
            setKeywordIndent(kw, 1);
        }
        for (String[] beKeywords : ArrayUtils.safeArray(getBlockBoundStrings())) {
            setKeywordIndent(beKeywords[0], 1);
            setKeywordIndent(beKeywords[1], -1);
        }

        if (isStandardSQL()) {
            // Add default types
            types.addAll(getDescriptor().getDataTypes(true));

            addKeywords(all, DBPKeywordType.KEYWORD);
            addKeywords(types, DBPKeywordType.TYPE);
            addKeywords(functions, DBPKeywordType.FUNCTION);
        }
    }


}
