/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.*;

/**
 * Basic SQL Dialect
 */
public class BasicSQLDialect implements SQLDialect {

    private static final String[] DEFAULT_LINE_COMMENTS = {SQLConstants.SL_COMMENT};
    private static final String[] EXEC_KEYWORDS = new String[0];
    private static final String[] DDL_KEYWORDS = new String[] {
        "CREATE", "ALTER", "DROP"
    };

    private static final String[][] DEFAULT_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END}
    };
    protected static final String[] NON_TRANSACTIONAL_KEYWORDS = new String[]{
        //SQLConstants.KEYWORD_SELECT, "WITH",
        "EXPLAIN", "DESCRIBE", "DESC", "USE", "SET", "COMMIT", "ROLLBACK" };
    private static final String[] CORE_NON_TRANSACTIONAL_KEYWORDS = new String[]{
            SQLConstants.KEYWORD_SELECT,
            };
    public static final String[][] DEFAULT_QUOTE_STRINGS = {{"\"", "\""}};

    // Keywords
    private TreeMap<String, DBPKeywordType> allKeywords = new TreeMap<>();

    private final TreeSet<String> reservedWords = new TreeSet<>();
    private final TreeSet<String> functions = new TreeSet<>();
    protected final TreeSet<String> types = new TreeSet<>();
    protected final TreeSet<String> tableQueryWords = new TreeSet<>();
    private final TreeSet<String> columnQueryWords = new TreeSet<>();
    // Comments
    private Pair<String, String> multiLineComments = new Pair<>(SQLConstants.ML_COMMENT_START, SQLConstants.ML_COMMENT_END);

    public static final BasicSQLDialect INSTANCE = new BasicSQLDialect();

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
    public String[][] getIdentifierQuoteStrings()
    {
        return DEFAULT_QUOTE_STRINGS;
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

    protected void addSQLKeyword(String keyword)
    {
        reservedWords.add(keyword);
        allKeywords.put(keyword, DBPKeywordType.KEYWORD);
    }

    protected void removeSQLKeyword(String keyword)
    {
        reservedWords.remove(keyword);
        allKeywords.remove(keyword);
    }

    protected void addSQLKeywords(Collection<String> allKeywords) {
        for (String kw : allKeywords) {
            addSQLKeyword(kw);
        }
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
     * @param set     keywords. Must be in upper case.
     * @param type    keyword type
     */
    protected void addKeywords(Collection<String> set, DBPKeywordType type)
    {
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
    public boolean isKeywordStart(@NotNull String word)
    {
        SortedMap<String, DBPKeywordType> map = allKeywords.tailMap(word.toUpperCase(Locale.ENGLISH));
        return !map.isEmpty() && map.firstKey().startsWith(word);
    }

    @Override
    public boolean isEntityQueryWord(@NotNull String word)
    {
        return tableQueryWords.contains(word.toUpperCase(Locale.ENGLISH));
    }

    @Override
    public boolean isAttributeQueryWord(@NotNull String word)
    {
        return columnQueryWords.contains(word.toUpperCase(Locale.ENGLISH));
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

    @Override
    public String[][] getBlockBoundStrings() {
        return DEFAULT_BEGIN_END_BLOCK;
    }

    @Nullable
    @Override
    public String getBlockHeaderString() {
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
    public boolean supportsSubqueries()
    {
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
    public boolean supportsCommentQuery() {
        return false;
    }

    @Override
    public boolean supportsNullability() {
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
        queryString = SQLUtils.stripComments(this, queryString.toUpperCase(Locale.ENGLISH)).trim();
        if (queryString.isEmpty()) {
            // Empty query - must be some metadata reading or something
            // anyhow it shouldn't be transactional
            return false;
        }
        String[] ntk = getNonTransactionKeywords();
        for (int i = 0; i < ntk.length; i++) {
            if (queryString.startsWith(ntk[i])) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    protected String[] getNonTransactionKeywords() {
        return isStandardSQL() ? NON_TRANSACTIONAL_KEYWORDS : CORE_NON_TRANSACTIONAL_KEYWORDS;
    }

    @Override
    public boolean isQuoteReservedWords() {
        return true;
    }

    protected boolean isStandardSQL() {
        return true;
    }

    private void loadStandardKeywords()
    {
        // Add default set of keywords
        Set<String> all = new HashSet<>();
        if (isStandardSQL()) {
            Collections.addAll(all, SQLConstants.SQL2003_RESERVED_KEYWORDS);
            //Collections.addAll(reservedWords, SQLConstants.SQL2003_NON_RESERVED_KEYWORDS);
            Collections.addAll(all, SQLConstants.SQL_EX_KEYWORDS);
            Collections.addAll(functions, SQLConstants.SQL2003_FUNCTIONS);
            Collections.addAll(tableQueryWords, SQLConstants.TABLE_KEYWORDS);
            Collections.addAll(columnQueryWords, SQLConstants.COLUMN_KEYWORDS);
        }

        for (String executeKeyword : ArrayUtils.safeArray(getExecuteKeywords())) {
            addSQLKeyword(executeKeyword);
        }

        if (isStandardSQL()) {
            // Add default types
            Collections.addAll(types, SQLConstants.DEFAULT_TYPES);

            addKeywords(all, DBPKeywordType.KEYWORD);
            addKeywords(types, DBPKeywordType.TYPE);
            addKeywords(functions, DBPKeywordType.FUNCTION);
        }
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBSTypedObject column, @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        typeName = CommonUtils.notEmpty(typeName).toUpperCase(Locale.ENGLISH);
        if (column instanceof DBSObject) {
            // If type is UDT (i.e. we can find it in type list) and type precision == column precision
            // then do not use explicit precision in column definition
            final DBSDataType dataType = DBUtils.getLocalDataType(((DBSObject) column).getDataSource(), column.getTypeName());
            if (dataType != null && CommonUtils.equalObjects(dataType.getScale(), column.getScale()) &&
                    ((CommonUtils.toInt(dataType.getPrecision()) > 0 && CommonUtils.equalObjects(dataType.getPrecision(), column.getPrecision())) ||
                            (dataType.getMaxLength() > 0 && dataType.getMaxLength() == column.getMaxLength())))
            {
                return null;
            }
        }
        if (dataKind == DBPDataKind.STRING) {
            if (typeName.indexOf('(') == -1) {
                final long maxLength = column.getMaxLength();
                if (maxLength > 0) {
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
                    return "(" + precision + ',' + scale + ')';
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

    // first line of the call stored procedure SQL (to be overridden)
    protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
        return "select " + proc.getFullyQualifiedName(DBPEvaluationContext.DML) + "(\n";
    }

    @Override
    public void generateStoredProcedureCall(StringBuilder sql, DBSProcedure proc, Collection<? extends DBSProcedureParameter> parameters) {
        List<DBSProcedureParameter> inParameters = new ArrayList<>();
        getMaxParameterLength(parameters, inParameters);
        sql.append(getStoredProcedureCallInitialClause(proc));
        for (int i = 0; i < inParameters.size(); i++) {
            DBSProcedureParameter parameter = inParameters.get(i);
            sql.append("\t\t\t:").append(CommonUtils.escapeIdentifier(parameter.getName()));
            if (i < (inParameters.size() - 1)) {
                sql.append(",");
            } else {
                sql.append(" ");
            }
            String typeName = parameter.getParameterType().getFullTypeName();
            sql.append("\t-- put the ").append(parameter.getName())
                .append(" parameter value instead of '").append(parameter.getName()).append("' (").append(typeName).append(")\n");
        }
        sql.append(");\n\n");
    }
}
