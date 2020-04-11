/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHexNative;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLStateType;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.*;

/**
 * Abstract SQL Dialect
 */
public abstract class AbstractSQLDialect implements SQLDialect {

    public static final String ID = "basic";

    private static final String[] DEFAULT_LINE_COMMENTS = { "//"};
    private static final String[] QUERY_KEYWORDS = new String[] { SQLConstants.KEYWORD_SELECT };
    private static final String[] EXEC_KEYWORDS = new String[0];
    private static final String[] DDL_KEYWORDS = new String[0];

    public static final String[][] DEFAULT_IDENTIFIER_QUOTES = {{"\"", "\""}};
    public static final String[][] DEFAULT_STRING_QUOTES = {{"'", "'"}};
    private static final String[][] DEFAULT_BEGIN_END_BLOCK = new String[0][];
    private static final String[] CORE_NON_TRANSACTIONAL_KEYWORDS = new String[0];
    public static final String[] DML_KEYWORDS = new String[0];

    // Keywords
    private TreeMap<String, DBPKeywordType> allKeywords = new TreeMap<>();

    private final TreeSet<String> reservedWords = new TreeSet<>();
    protected final TreeSet<String> functions = new TreeSet<>();
    protected final TreeSet<String> types = new TreeSet<>();
    protected final TreeSet<String> tableQueryWords = new TreeSet<>();
    protected final TreeSet<String> columnQueryWords = new TreeSet<>();
    // Comments
    private Pair<String, String> multiLineComments = new Pair<>(SQLConstants.ML_COMMENT_START, SQLConstants.ML_COMMENT_END);
    private Map<String, Integer> keywordsIndent = new HashMap<>();

    protected AbstractSQLDialect() {
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
        reservedWords.add(keyword);
        allKeywords.put(keyword, DBPKeywordType.KEYWORD);
    }

    protected void removeSQLKeyword(String keyword) {
        reservedWords.remove(keyword);
        allKeywords.remove(keyword);
    }

    protected void addSQLKeywords(Collection<String> allKeywords) {
        for (String kw : allKeywords) {
            addSQLKeyword(kw);
        }
    }

    protected void setKeywordIndent(String ketyword, int indent) {
        keywordsIndent.put(ketyword, indent);
    }

    protected void addFunctions(Collection<String> allFunctions) {
        functions.addAll(allFunctions);
        addKeywords(allFunctions, DBPKeywordType.FUNCTION);
    }

    protected void addDataTypes(Collection<String> allTypes) {
        for (String type : allTypes) {
            types.add(type.toUpperCase(Locale.ENGLISH));
        }
        addKeywords(allTypes, DBPKeywordType.TYPE);
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
                keyword = keyword.toUpperCase(Locale.ENGLISH);
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
    public Set<String> getReservedWords() {
        return reservedWords;
    }

    @NotNull
    @Override
    public Set<String> getFunctions(@Nullable DBPDataSource dataSource) {
        return functions;
    }

    @NotNull
    @Override
    public TreeSet<String> getDataTypes(@Nullable DBPDataSource dataSource) {
        return types;
    }

    @Override
    public DBPKeywordType getKeywordType(@NotNull String word) {
        return allKeywords.get(word.toUpperCase(Locale.ENGLISH));
    }

    @NotNull
    @Override
    public List<String> getMatchedKeywords(@NotNull String word) {
        word = word.toUpperCase(Locale.ENGLISH);
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
    public boolean isKeywordStart(@NotNull String word) {
        SortedMap<String, DBPKeywordType> map = allKeywords.tailMap(word.toUpperCase(Locale.ENGLISH));
        return !map.isEmpty() && map.firstKey().startsWith(word);
    }

    @Override
    public boolean isEntityQueryWord(@NotNull String word) {
        return tableQueryWords.contains(word.toUpperCase(Locale.ENGLISH));
    }

    @Override
    public boolean isAttributeQueryWord(@NotNull String word) {
        return columnQueryWords.contains(word.toUpperCase(Locale.ENGLISH));
    }

    @Override
    public int getKeywordNextLineIndent(@NotNull String word) {
        Integer indent = keywordsIndent.get(word.toUpperCase(Locale.ENGLISH));
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
    public String getScriptDelimiter() {
        return ";"; //$NON-NLS-1$
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

    @NotNull
    @Override
    public String getTypeCastClause(DBSAttributeBase attribute, String expression) {
        return expression;
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
    public String escapeScriptValue(DBSAttributeBase attribute, @NotNull Object value, @NotNull String strValue) {
        if (value instanceof UUID) {
            return '\'' + escapeString(strValue) + '\'';
        }
        return strValue;
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        return MultiValueInsertMode.NOT_SUPPORTED;
    }

    @Override
    public String addFiltersToQuery(DBPDataSource dataSource, String query, DBDDataFilter filter) {
        return SQLSemanticProcessor.addFiltersToQuery(dataSource, query, filter);
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
    public boolean supportsCommentQuery() {
        return false;
    }

    @Override
    public boolean supportsNullability() {
        return true;
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
        firstKeyword = firstKeyword.toUpperCase(Locale.ENGLISH);
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
        if (SQLConstants.KEYWORD_SELECT.equals(firstKeyword) ||
            "SHOW".equals(firstKeyword) ||
            "USE".equals(firstKeyword) ||
            "SET".equals(firstKeyword))
        {
            return false;
        }
        return true;
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

    @Override
    public boolean isQuoteReservedWords() {
        return true;
    }

    @Override
    public boolean isCRLFBroken() {
        return false;
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column, @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        typeName = CommonUtils.notEmpty(typeName).toUpperCase(Locale.ENGLISH);
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
                if (maxLength > 0 && maxLength != Integer.MAX_VALUE && maxLength != Long.MAX_VALUE) {
                    Object maxStringLength = dataSource.getDataSourceFeature(DBConstants.FEATURE_MAX_STRING_LENGTH);
                    if (maxStringLength instanceof Number) {
                        int lengthLimit = ((Number) maxStringLength).intValue();
                        if (lengthLimit < 0) {
                            return null;
                        } else if (lengthLimit < maxLength) {
                            maxLength = lengthLimit;
                        }
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

    protected boolean useBracketsForExec() {
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

    @Override
    public void generateStoredProcedureCall(StringBuilder sql, DBSProcedure proc, Collection<? extends DBSProcedureParameter> parameters) {
        List<DBSProcedureParameter> inParameters = new ArrayList<>();
        if (parameters != null) {
            inParameters.addAll(parameters);
        }
        //getMaxParameterLength(parameters, inParameters);
        boolean useBrackets = useBracketsForExec();
        if (useBrackets) sql.append("{ ");
        sql.append(getStoredProcedureCallInitialClause(proc)).append("(");
        if (!inParameters.isEmpty()) {
            boolean first = true;
            for (int i = 0; i < inParameters.size(); i++) {
                DBSProcedureParameter parameter = inParameters.get(i);
                if (!first) {
                    sql.append(",");
                }
                switch (parameter.getParameterKind()) {
                    case IN:
                        sql.append(":").append(CommonUtils.escapeIdentifier(parameter.getName()));
                        break;
                    case RETURN:
                        continue;
                    default:
                        sql.append("?");
                }
                String typeName = parameter.getParameterType().getFullTypeName();
//                sql.append("\t-- put the ").append(parameter.getName())
//                    .append(" parameter value instead of '").append(parameter.getName()).append("' (").append(typeName).append(")");
                first = false;
            }
        }
        sql.append(")");
        if (!useBrackets) {
            sql.append(";");
        } else {
            sql.append(" }");
        }
        sql.append("\n\n");
    }

    @Override
    public boolean isDisableScriptEscapeProcessing() {
        return false;
    }

    @Override
    public boolean supportsAlterTableConstraint() {
        return true;
    }
}
