/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHexNative;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.EmptyTokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.SQLTokenPredicateSet;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Abstract SQL Dialect
 */
public abstract class AbstractSQLDialect implements SQLDialect {

    public static final String ID = "basic";

    private static final String[] DEFAULT_LINE_COMMENTS = { "//"};
    private static final String[] QUERY_KEYWORDS = new String[] { SQLConstants.KEYWORD_SELECT };
    private static final String[] EXEC_KEYWORDS = new String[0];
    private static final String[] DDL_KEYWORDS = new String[0];
    private static final Collection<String> TRANSACTION_NON_MODIFYING_KEYWORDS =
        Set.of(SQLConstants.KEYWORD_SELECT, "SHOW", "USE", "SET", SQLConstants.KEYWORD_EXPLAIN);

    public static final String[][] DEFAULT_IDENTIFIER_QUOTES = {{"\"", "\""}};
    public static final String[][] DEFAULT_STRING_QUOTES = {{"'", "'"}};
    private static final String[][] DEFAULT_BEGIN_END_BLOCK = new String[0][];
    private static final String[] CORE_NON_TRANSACTIONAL_KEYWORDS = new String[0];
    public static final String[] DML_KEYWORDS = new String[0];
    public static final Pair<String, String> IN_CLAUSE_PARENTHESES = new Pair<>("(", ")");

    public static final Locale DEF_LOCALE = Locale.ENGLISH;

    private static class KeywordHolder {
        DBPKeywordType type;
        String original;

        public KeywordHolder(DBPKeywordType type, String original) {
            this.type = type;
            this.original = original;
        }
    }
    // Keywords
    private final TreeMap<String, KeywordHolder> allKeywords = new TreeMap<>();

    // avoiding ConcurrentModificationException (CB-5521)
    private final ConcurrentNavigableMap<String, String> reservedWords = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<String, String> functions = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<String, String> types = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<String, String> tableQueryWords = new ConcurrentSkipListMap<>();
    private final ConcurrentNavigableMap<String, String> columnQueryWords = new ConcurrentSkipListMap<>();
    // Comments
    private final Pair<String, String> multiLineComments = new Pair<>(SQLConstants.ML_COMMENT_START, SQLConstants.ML_COMMENT_END);
    private final Map<String, Integer> keywordsIndent = new HashMap<>();

    protected AbstractSQLDialect() {
    }

    @NotNull
    @Override
    public SQLQueryGenerator getQueryGenerator() {
        return StandardSQLDialectQueryGenerator.INSTANCE;
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
    public String[] getQueryKeywords() {
        return QUERY_KEYWORDS;
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @NotNull
    @Override
    public String[] getDDLKeywords() {
        return DDL_KEYWORDS;
    }

    protected void addSQLKeyword(String keyword) {
        String ciWord = keyword.toUpperCase(DEF_LOCALE);
        reservedWords.put(ciWord, keyword);
        allKeywords.put(ciWord, new KeywordHolder(DBPKeywordType.KEYWORD, keyword));
    }

    protected void removeSQLKeyword(String keyword) {
        String ciWord = keyword.toUpperCase(DEF_LOCALE);
        reservedWords.remove(ciWord);
        allKeywords.remove(ciWord);
    }

    protected void addSQLKeywords(Collection<String> allKeywords) {
        for (String kw : allKeywords) {
            addSQLKeyword(kw);
        }
    }

    @NotNull
    @Override
    public Pair<String, String> getInClauseParentheses() {
        return IN_CLAUSE_PARENTHESES;
    }

    protected void setKeywordIndent(String ketyword, int indent) {
        keywordsIndent.put(ketyword, indent);
    }

    protected void addFunctions(Collection<String> allFunctions) {
        for (String function : allFunctions) {
            functions.put(function.toUpperCase(DEF_LOCALE), function);
        }
        addKeywords(allFunctions, DBPKeywordType.FUNCTION);
    }

    protected void turnFunctionIntoKeyword(String function) {
        functions.remove(function);
        addKeywords(Collections.singletonList(function), DBPKeywordType.KEYWORD);
    }

    protected void addDataTypes(Collection<String> allTypes) {
        for (String type : allTypes) {
            types.put(type.toUpperCase(DEF_LOCALE), type);
        }
        addKeywords(allTypes, DBPKeywordType.TYPE);
    }

    protected Collection<String> getTableQueryWords() {
        return tableQueryWords.values();
    }

    protected void addTableQueryKeywords(String ... keywords) {
        for (String keyword : keywords) {
            tableQueryWords.put(keyword.toUpperCase(DEF_LOCALE), keyword);
        }
    }

    public Collection<String> getColumnQueryWords() {
        return columnQueryWords.values();
    }

    protected void addColumnQueryKeywords(String ... keywords) {
        for (String keyword : keywords) {
            columnQueryWords.put(keyword.toUpperCase(DEF_LOCALE), keyword);
        }
    }

    /**
     * Add keywords.
     *
     * @param set  keywords. Must be in upper case.
     * @param type keyword type
     */
    protected void addKeywords(Collection<String> set, DBPKeywordType type) {
        if (set != null) {
            for (String keyword : set) {
                String ciKeyword = keyword.toUpperCase(DEF_LOCALE);
                reservedWords.put(ciKeyword, keyword);
                KeywordHolder oldType = allKeywords.get(ciKeyword);
                if (oldType == null || oldType.type != DBPKeywordType.KEYWORD) {
                    // We can't mark keywords as functions or types because keywords are reserved and
                    // if some identifier conflicts with keyword it must be quoted.
                    allKeywords.put(ciKeyword, new KeywordHolder(type, keyword));
                }
            }
        }
    }

    @NotNull
    @Override
    public Collection<String> getReservedWords() {
        return reservedWords.values();
    }

    @NotNull
    @Override
    public Collection<String> getFunctions() {
        return functions.values();
    }

    @NotNull
    @Override
    public Collection<String> getDataTypes(@Nullable DBPDataSource dataSource) {
        return types.values();
    }

    protected void clearDataTypes() {
        types.clear();
    }

    @Override
    public DBPKeywordType getKeywordType(@NotNull String word) {
        KeywordHolder keywordHolder = allKeywords.get(word.toUpperCase(DEF_LOCALE));
        return keywordHolder == null ? null : keywordHolder.type;
    }

    @NotNull
    @Override
    public List<String> getMatchedKeywords(@NotNull String word) {
        word = word.toUpperCase(DEF_LOCALE);
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, KeywordHolder> keyword : allKeywords.tailMap(word).entrySet()) {
            if (keyword.getKey().startsWith(word)) {
                result.add(keyword.getValue().original);
            } else {
                break;
            }
        }
        return result;
    }

    @Override
    public boolean isKeywordStart(@NotNull String word) {
        SortedMap<String, KeywordHolder> map = allKeywords.tailMap(word.toUpperCase(DEF_LOCALE));
        return !map.isEmpty() && map.firstKey().startsWith(word);
    }

    @Override
    public boolean isEntityQueryWord(@NotNull String word) {
        return tableQueryWords.containsKey(word.toUpperCase(DEF_LOCALE));
    }

    @Override
    public boolean isAttributeQueryWord(@NotNull String word) {
        return columnQueryWords.containsKey(word.toUpperCase(DEF_LOCALE));
    }

    @Override
    public int getKeywordNextLineIndent(@NotNull String word) {
        Integer indent = keywordsIndent.get(word.toUpperCase(DEF_LOCALE));
        return indent == null ? 0 : indent;
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

    @NotNull
    @Override
    public String[] getParametersPrefixes() {
        return new String[0];//{String.valueOf(SQLConstants.DEFAULT_PARAMETER_PREFIX)};
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

    @NotNull
    @Override
    public String[] getScriptDelimiters() {
        return SQLConstants.DEFAULT_SCRIPT_DELIMITER; //$NON-NLS-1$
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

    @Nullable
    @Override
    public String[] getInnerBlockPrefixes() {
        return null;
    }

    @Override
    public boolean isWordStart(int ch) {
        return Character.isUnicodeIdentifierStart(ch) || ch == '_';
    }

    @Override
    public boolean isWordPart(int ch) {
        return Character.isUnicodeIdentifierPart(ch);
    }

    @Override
    public boolean validIdentifierStart(char c) {
        return Character.isLetter(c);
    }

    @Override
    public boolean validIdentifierPart(char c, boolean quoted) {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_';
    }

    @Override
    public boolean useCaseInsensitiveNameLookup() {
        return false;
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

    /**
     * Appends cast clause to attribute name.
     * @attributeName is preformatted name of attribute
     */
    @Override
    public String getCastedAttributeName(@NotNull DBSAttributeBase attribute, String attributeName) {
        if (attribute instanceof DBSObject && !DBUtils.isPseudoAttribute(attribute)) {
            if (!CommonUtils.equalObjects(attributeName, attribute.getName())) {
                // Must use explicit attribute name
                attributeName = DBUtils.getQuotedIdentifier(((DBSObject) attribute).getDataSource(), attributeName);
            } else {
                attributeName = DBUtils.getObjectFullName(((DBSObject) attribute).getDataSource(), attribute, DBPEvaluationContext.DML);
            }
        }
        return attributeName;
    }

    @NotNull
    @Override
    public String getTypeCastClause(@NotNull DBSTypedObject attribute, String expression, boolean isInCondition) {
        return expression;
    }

    @Override
    public boolean isQuotedIdentifier(String identifier) {
        {
            final String[][] quoteStrings = this.getIdentifierQuoteStrings();
            if (ArrayUtils.isEmpty(quoteStrings)) {
                return false;
            }
            for (String[] quoteString : quoteStrings) {
                if (identifier.startsWith(quoteString[0]) && identifier.endsWith(quoteString[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getQuotedIdentifier(String str, boolean forceCaseSensitive, boolean forceQuotes) {
        if (isQuotedIdentifier(str)) {
            // Already quoted
            return str;
        }

        String[][] quoteStrings = this.getIdentifierQuoteStrings();
        if (ArrayUtils.isEmpty(quoteStrings)) {
            return str;
        }

        if (mustBeQuoted(str, forceCaseSensitive) || forceQuotes) {
            return quoteIdentifier(str, quoteStrings);
        } else {
            return str;
        }
    }

    public boolean mustBeQuoted(@NotNull String str, boolean forceCaseSensitive) {
        // Check for keyword conflict
        final DBPKeywordType keywordType = this.getKeywordType(str);
        boolean hasBadChars = (keywordType == DBPKeywordType.KEYWORD || keywordType == DBPKeywordType.TYPE || keywordType == DBPKeywordType.OTHER) &&
                this.isQuoteReservedWords();

        if (!hasBadChars && !str.isEmpty()) {
            hasBadChars = !this.validIdentifierStart(str.charAt(0));
        }
        if (!hasBadChars && forceCaseSensitive) {
            // Check for case of quoted indents. Do not check for unquoted case - we don't need to quote em anyway
            // Disable supportsQuotedMixedCase checking. Let's quote identifiers always if storage case doesn't match actual case
            // unless database use case-insensitive search always (e.g. MySQL with lower_case_table_names <> 0)
            if (!this.useCaseInsensitiveNameLookup()) {
                // See how unquoted identifiers are stored
                // If passed identifier case differs from unquoted then we need to escape it
                switch (this.storesUnquotedCase()) {
                    case UPPER:
                        hasBadChars = !str.equals(str.toUpperCase());
                        break;
                    case LOWER:
                        hasBadChars = !str.equals(str.toLowerCase());
                        break;
                }
            }
        }

        // Check for bad characters
        if (!hasBadChars && !str.isEmpty()) {
            for (int i = 0; i < str.length(); i++) {
                if (!this.validIdentifierPart(str.charAt(i), false)) {
                    hasBadChars = true;
                    break;
                }
            }
        }

        return hasBadChars;
    }

    @NotNull
    protected String quoteIdentifier(@NotNull String str, @NotNull String[][] quoteStrings) {
        // Escape quote chars
        for (String[] pair : quoteStrings) {
            final String q1 = pair[0];
            final String q2 = pair[1];
            if (q1.equals(q2) && (q1.equals("\"") || q1.equals("'")) && str.contains(q1)) {
                str = str.replace(q1, q1 + q1);
            }
        }
        // Escape with first (default) quote string
        return quoteStrings[0][0] + str + quoteStrings[0][1];
    }

    @Override
    public String getUnquotedIdentifier(String identifier) {
        return getUnquotedIdentifier(identifier, false);
    }
    
    @Override
    public String getUnquotedIdentifier(String identifier, boolean unescapeQuotesInsideIdentifier) {
        String[][] quoteStrings = this.getIdentifierQuoteStrings();
        if (ArrayUtils.isEmpty(quoteStrings)) {
            quoteStrings = BasicSQLDialect.DEFAULT_IDENTIFIER_QUOTES;
        }
        for (int i = 0; i < quoteStrings.length; i++) {
            identifier = DBUtils.getUnQuotedIdentifier(identifier, quoteStrings[i][0], quoteStrings[i][1]);
            if (unescapeQuotesInsideIdentifier) {
                identifier = identifier.replace(quoteStrings[i][0] + quoteStrings[i][0], quoteStrings[i][0]);
            }
        }
        return identifier;
    }

    @Override
    public boolean isQuotedString(String string) {
        return string.length() >= 2 && string.charAt(0) == '\'' && string.charAt(string.length() - 1) == '\'';
    }

    @Override
    public String getQuotedString(String string) {
        return '\'' + escapeString(string) + '\'';
    }

    @Override
    public String getUnquotedString(String string) {
        return isQuotedString(string) ? unEscapeString(string.substring(1, string.length() - 1)) : string;
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

    @NotNull
    @Override
    public MultiValueInsertMode getDefaultMultiValueInsertMode() {
        return MultiValueInsertMode.NOT_SUPPORTED;
    }

    @Override
    public String addFiltersToQuery(DBRProgressMonitor monitor, DBPDataSource dataSource, String query, DBDDataFilter filter) {
        return getQueryGenerator().getQueryWithAppliedFilters(monitor, dataSource, query, filter);
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
    public boolean supportsAsKeywordBeforeAliasInFromClause() {
        return true;
    }

    @Override
    public boolean supportsAliasInUpdate() {
        return false;
    }

    @Nullable
    @Override
    public String getAllAttributesAlias() {
        return SQLConstants.COLUMN_ASTERISK;
    }

    @Nullable
    @Override
    public String getDefaultGroupAttribute() {
        return getAllAttributesAlias();
    }

    @Override
    public boolean supportsAliasInConditions() {
        return true;
    }

    @Override
    public String getOffsetLimitQueryPart(int offset, int limit) {
        return String.format("LIMIT %d OFFSET %d", limit, offset);
    }

    @Override
    public String getClobComparingPart(@NotNull String columnName) {
        return "%s=?".formatted(columnName);
    }

    @Override
    public boolean supportsAliasInHaving() {
        return true;
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
    public boolean supportsNestedComments() {
        return false;
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
    public boolean supportsColumnAutoIncrement() {
        return true;
    }

    @Nullable
    @Override
    public SQLExpressionFormatter getCaseInsensitiveExpressionFormatter(@NotNull DBCLogicalOperator operator) {
        return null;
    }

    @Override
    public Pair<String, String> getMultiLineComments() {
        return multiLineComments;
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

    @Override
    public boolean needsDelimiterFor(String firstKeyword, String lastKeyword) {
        return false;
    }

    @NotNull
    @Override
    public DBDBinaryFormatter getNativeBinaryFormatter() {
        return BinaryFormatterHexNative.INSTANCE;
    }

    @Override
    public String getTestSQL() {
        return null;
    }

    @Nullable
    @Override
    public String getDualTableName() {
        return null;
    }

    @Override
    public boolean isTransactionModifyingQuery(String queryString) {
        queryString = SQLUtils.stripComments(this, queryString);
        if (queryString.isEmpty()) {
            // Empty query - must be some metadata reading or something
            // anyhow it shouldn't be transactional
            return false;
        }
        String firstKeyword = SQLUtils.getFirstKeyword(this, queryString);
        if (firstKeyword.isEmpty()) {
            return false;
        }
        firstKeyword = firstKeyword.toUpperCase(DEF_LOCALE);
        return isTransactionModifyingKeyword(firstKeyword);
    }

    @Nullable
    @Override
    public String[] getTransactionCommitKeywords() {
        return null;
    }

    @Nullable
    @Override
    public String[] getTransactionRollbackKeywords() {
        return null;
    }

    protected boolean isTransactionModifyingKeyword(String firstKeyword) {
        if (getKeywordType(firstKeyword) != DBPKeywordType.KEYWORD) {
            return false;
        }
        return !TRANSACTION_NON_MODIFYING_KEYWORDS.contains(firstKeyword);
    }

    private static boolean containsKeyword(String[] keywords, String keyword) {
        if (keywords == null) {
            return false;
        }
        for (int i = 0; i < keywords.length; i++) {
            if (keyword.equals(keywords[i])) return true;
        }
        return false;
    }

    @Override
    @NotNull
    public String[] getDMLKeywords() {
        return DML_KEYWORDS;
    }

    @NotNull
    public String[] getNonTransactionKeywords() {
        return CORE_NON_TRANSACTIONAL_KEYWORDS;
    }

    public boolean isQuoteReservedWords() {
        return true;
    }

    @Override
    public boolean isCRLFBroken() {
        return false;
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column, @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        typeName = CommonUtils.notEmpty(typeName).toUpperCase(DEF_LOCALE);
        if (column instanceof DBSObject) {
            // If type is UDT (i.e. we can find it in type list) and type precision == column precision
            // then do not use explicit precision in column definition
            final DBSDataType dataType;
            if (column instanceof DBSTypedObjectEx) {
                dataType = ((DBSTypedObjectEx) column).getDataType();
            } else {
                dataType = DBUtils.getLocalDataType(((DBSObject) column).getDataSource(), column.getTypeName());
            }
            if (dataType != null && CommonUtils.equalObjects(dataType.getScale(), column.getScale()) &&
                ((CommonUtils.toInt(dataType.getPrecision()) > 0 && CommonUtils.equalObjects(dataType.getPrecision(), column.getPrecision())) ||
                    (dataType.getMaxLength() > 0 && dataType.getMaxLength() == column.getMaxLength()))) {
                return null;
            }
        }
        if (dataKind == DBPDataKind.STRING) {
            if (typeName.indexOf('(') == -1) {
                long maxLength = column.getMaxLength();
                if (maxLength > 0) {
                    boolean badValue = maxLength == Integer.MAX_VALUE || maxLength == Long.MAX_VALUE;
                    Object maxStringLength = dataSource.getDataSourceFeature(DBPDataSource.FEATURE_MAX_STRING_LENGTH);
                    if (maxStringLength instanceof Number) {
                        int lengthLimit = ((Number) maxStringLength).intValue();
                        if (lengthLimit < 0) {
                            return null;
                        } else if (lengthLimit < maxLength) {
                            maxLength = lengthLimit;
                        }
                    } else if (badValue) {
                        return null;
                    }
                    return "(" + maxLength + ")";
                }
            }
        } else if ((dataKind == DBPDataKind.CONTENT || dataKind == DBPDataKind.BINARY) && !typeName.contains("LOB")) {
            final long maxLength = column.getMaxLength();
            if (maxLength > 0 && maxLength < Integer.MAX_VALUE) {
                return "(" + maxLength + ')';
            }
        } else if (dataKind == DBPDataKind.NUMERIC) {
            if (typeName.equals("DECIMAL") || typeName.equals("NUMERIC") || typeName.equals("NUMBER")) {
                Integer scale = column.getScale();
                int precision = CommonUtils.toInt(column.getPrecision());
                if (precision == 0) {
                    precision = (int) column.getMaxLength();
                    if (precision > 0) {
                        // FIXME: max length is actually length in character.
                        // FIXME: On Oracle it returns bigger values than maximum (#1767)
                        // FIXME: in other DBs it equals to precision in most cases
                        //precision--; // One character for sign?
                    }
                }
                if (scale != null && scale >= 0 && precision >= 0 && !(scale == 0 && precision == 0)) {
                    /*if (scale == 0) {
                        return "(" + precision + ')';
                    } else */{
                        return "(" + precision + ',' + scale + ')';
                    }
                }
            } else if (typeName.equals("BIT")) {
                // Bit string?
                int precision = CommonUtils.toInt(column.getPrecision());
                if (precision > 1) {
                    return "(" + precision + ')';
                }
            }
        }
        return null;
    }

    @Override
    public String formatStoredProcedureCall(DBPDataSource dataSource, String sqlText) {
        return sqlText;
    }

    /**
     * @param inParameters empty list to collect IN parameters
     */
    protected int getMaxParameterLength(Collection<? extends DBSProcedureParameter> parameters, List<DBSProcedureParameter> inParameters) {
        int maxParamLength = 0;
        for (DBSProcedureParameter param : parameters) {
            if (param.getParameterKind() == DBSProcedureParameterKind.IN) {
                inParameters.add(param);
                if (param.getName().length() > maxParamLength) {
                    maxParamLength = param.getName().length();
                }
            }
        }
        return maxParamLength;
    }

    protected boolean useBracketsForExec(DBSProcedure procedure) {
        return false;
    }

    // first line of the call stored procedure SQL (to be overridden)
    protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
        String[] executeKeywords = getExecuteKeywords();
        if (proc.getProcedureType() == DBSProcedureType.FUNCTION || ArrayUtils.isEmpty(executeKeywords)) {
            return SQLConstants.KEYWORD_SELECT + " " + proc.getFullyQualifiedName(DBPEvaluationContext.DML);
        } else {
            return executeKeywords[0] + " " + proc.getFullyQualifiedName(DBPEvaluationContext.DML);
        }
    }

    @NotNull
    protected String getProcedureCallEndClause(DBSProcedure procedure) {
        return "";
    }

    @Override
    public void generateStoredProcedureCall(
        StringBuilder sql, 
        DBSProcedure proc, 
        Collection<? extends DBSProcedureParameter> parameters, 
        boolean castParams
    ) {
        List<DBSProcedureParameter> inParameters = new ArrayList<>();
        if (parameters != null) {
            inParameters.addAll(parameters);
        }
        //getMaxParameterLength(parameters, inParameters);
        DBPPreferenceStore prefStore;
        DBPDataSource dataSource = proc.getDataSource();
        if (dataSource != null) {
            prefStore = dataSource.getContainer().getPreferenceStore();
        } else {
            prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        }
        String namedParameterPrefix = prefStore.getString(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX);        
        boolean useBrackets = useBracketsForExec(proc);
        if (useBrackets) sql.append("{ ");
        sql.append(getStoredProcedureCallInitialClause(proc)).append("(");
        if (!inParameters.isEmpty()) {
            boolean first = true;
            for (DBSProcedureParameter parameter : inParameters) {
                String typeName = parameter.getParameterType().getFullTypeName();
                switch (parameter.getParameterKind()) {
                    case IN:
                        if (!first) {
                            sql.append(", ");
                        }
                        if (castParams) {
                            sql.append("cast(").append(namedParameterPrefix).append(CommonUtils.escapeIdentifier(parameter.getName()))
                                .append(" as ").append(typeName).append(")");
                        } else {
                            sql.append(namedParameterPrefix).append(CommonUtils.escapeIdentifier(parameter.getName()));
                        }
                        break;
                    case RETURN:
                        continue;
                    default:
                        if (isStoredProcedureCallIncludesOutParameters()) {
                            if (!first) {
                                sql.append(", ");
                            }
                            if (castParams) {
                                sql.append("cast(?")
                                    .append(" as ").append(typeName).append(")");
                            } else {
                                sql.append("?");
                            }
                        }
                        break;
                }                
//                sql.append("\t-- put the ").append(parameter.getName())
//                    .append(" parameter value instead of '").append(parameter.getName()).append("' (").append(typeName).append(")");
                first = false;
            }
        }
        sql.append(")");
        String callEndClause = getProcedureCallEndClause(proc);
        if (!CommonUtils.isEmpty(callEndClause)) {
            sql.append(" ").append(callEndClause);
        }
        if (!useBrackets) {
            sql.append(";");
        } else {
            sql.append(" }");
        }
        sql.append("\n\n");
    }

    protected boolean isStoredProcedureCallIncludesOutParameters() {
        return true;
    }

    @Override
    public boolean isDisableScriptEscapeProcessing() {
        return false;
    }

    @Override
    public boolean supportsAlterTableStatement() {
        return true;
    }

    @Override
    public boolean supportsIndexCreateAndDrop() {
        return supportsAlterTableStatement();
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return false;
    }

    @Override
    public boolean supportsUuid() {
        return true;
    }

    @NotNull
    @Override
    public SQLTokenPredicateSet getSkipTokenPredicates() {
        return EmptyTokenPredicateSet.INSTANCE;
    }

    @Override
    public boolean isStripCommentsBeforeBlocks() {
        return false;
    }

    @Override
    public boolean hasCaseSensitiveFiltration() {
        return false;
    }

}


