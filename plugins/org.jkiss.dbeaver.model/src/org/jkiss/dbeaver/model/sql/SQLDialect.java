/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.utils.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * SQL dialect
 */
public interface SQLDialect {

    int USAGE_NONE = 0;
    int USAGE_DML = 1;
    int USAGE_DDL = 2;
    int USAGE_PROC = 4;
    int USAGE_INDEX = 8;
    int USAGE_PRIV = 8;
    int USAGE_ALL = Integer.MAX_VALUE;

    enum MultiValueInsertMode {
        NOT_SUPPORTED,
        GROUP_ROWS,
        PLAIN,
        INSERT_ALL
    }

    @NotNull
    String getDialectId();

    /**
     * Dialect name
     * @return SQL dialect name
     */
    @NotNull
    String getDialectName();

    /**
     * Retrieves strings used to quote SQL identifiers.
     * This method returns null or empty array if identifier quoting is not supported.
     *
     * @return the array of string pairs
     */
    @Nullable
    String[][] getIdentifierQuoteStrings();

    /**
     * Retrieves strings used to quote SQL strings.
     *
     * @return the array of string pairs
     */
    @NotNull
    String[][] getStringQuoteStrings();

    /**
     * Data query keywords. By default it is SELECT
     */
    @NotNull
    String[] getQueryKeywords();

    /**
     * Retrieves a list of execute keywords. If database doesn't support implicit execute returns empty list or null.
     * @return the list of execute keywords.
     */
    @NotNull
    String[] getExecuteKeywords();

    /**
     * Retrieves a list of execute keywords. If database doesn't support implicit execute returns empty list or null.
     * @return the list of execute keywords.
     */
    @NotNull
    String[] getDDLKeywords();

    @NotNull
    String[] getDMLKeywords();

    /**
     * Retrieves a list of all of this database's SQL keywords
     * that are NOT also SQL92 keywords.
     *
     * @return the list of this database's keywords that are not also
     *         SQL92 keywords
     */
    @NotNull
    Set<String> getReservedWords();
    @NotNull
    Set<String> getFunctions(@Nullable DBPDataSource dataSource);
    @NotNull
    Set<String> getDataTypes(@Nullable DBPDataSource dataSource);
    @Nullable
    DBPKeywordType getKeywordType(@NotNull String word);
    @NotNull
    List<String> getMatchedKeywords(@NotNull String word);

    boolean isKeywordStart(@NotNull String word);

    boolean isEntityQueryWord(@NotNull String word);

    boolean isAttributeQueryWord(@NotNull String word);

    int getKeywordNextLineIndent(@NotNull String word);

    /**
     * Retrieves the string that can be used to escape wildcard characters.
     * This is the string that can be used to escape '_' or '%' in
     * the catalog search parameters that are a pattern (and therefore use one
     * of the wildcard characters).
     * <p/>
     * <P>The '_' character represents any single character;
     * the '%' character represents any sequence of zero or
     * more characters.
     *
     * @return the string used to escape wildcard characters
     */
    @NotNull
    String getSearchStringEscape();

    /**
     * Strings (in single or double quotes) escape character. Zero (i.e. no scape character) by default.
     * Back slash in many dialects
     */
    char getStringEscapeCharacter();

    /**
     * Catalog name usage in queries
     * @return catalog usage
     */
    int getCatalogUsage();

    /**
     * Schema name usage in queries
     * @return schema usage
     */
    int getSchemaUsage();

    /**
     * Retrieves the <code>String</code> that this database uses as the
     * separator between a catalog and table name.
     *
     * @return the separator string
     */
    @NotNull
    String getCatalogSeparator();

    /**
     * Retrieves the <code>String</code> that this database uses as the
     * separator between a structured objects (e.g. schema and table).
     *
     * @return the separator string
     */
    char getStructSeparator();

    @NotNull
    String[] getParametersPrefixes();

    /**
     * Script delimiter characters
     * @return array of possible script delimiters with first element as default delimiter
     */
    @NotNull
    String[] getScriptDelimiters();

    @Nullable
    String getScriptDelimiterRedefiner();

    /**
     * SQL block statements (BEGIN/END).
     * Null if not supported
     */
    @Nullable
    String[][] getBlockBoundStrings();

    /**
     * Script block header string.
     * Begins SQL block header (most typical: DECLARE).
     * @return block header string or null (not supported)
     */
    @Nullable
    String[] getBlockHeaderStrings();

    /**
     * Inner block prefixes strings.
     * Determines if the block is a child of the header block.
     * @return inner block prefixes or null (if not supported)
     */
    @Nullable
    String[] getInnerBlockPrefixes();

    /**
     * Retrieves whether a catalog appears at the start of a fully qualified
     * table name.  If not, the catalog appears at the end.
     *
     * @return <code>true</code> if the catalog name appears at the beginning
     *         of a fully qualified table name; <code>false</code> otherwise
     */
    boolean isCatalogAtStart();

    /**
     * SQL state type
     * @return sql state type
     */
    @NotNull
    SQLStateType getSQLStateType();

    boolean validIdentifierStart(char c);
    /**
     * Checks that specified character is a valid identifier part. Non-valid characters should be quoted in queries.
     * @param c character
     * @param quoted is identifier quoted
     * @return true or false
     */
    boolean validIdentifierPart(char c, boolean quoted);

    boolean useCaseInsensitiveNameLookup();

    boolean supportsUnquotedMixedCase();

    boolean supportsQuotedMixedCase();

    boolean supportsSubqueries();

    boolean supportsAliasInSelect();

    boolean supportsAliasInUpdate();

    boolean supportsTableDropCascade();

    boolean supportsOrderByIndex();

    boolean supportsNestedComments();

    /**
     * Check whether dialect support plain comment queries (queries which contains only comments)
     */
    boolean supportsCommentQuery();

    boolean supportsNullability();

    @Nullable
    SQLExpressionFormatter getCaseInsensitiveExpressionFormatter(@NotNull DBCLogicalOperator operator);

    @NotNull
    DBPIdentifierCase storesUnquotedCase();

    @NotNull
    DBPIdentifierCase storesQuotedCase();

    /**
     * Enables to call particular cast operator or function for special data types.
     * @param attribute   attribute data to help decide whether cast and how to cast
     * @param expression      string representation for cast
     * @return            casted string
     */
    @NotNull
    String getTypeCastClause(DBSAttributeBase attribute, String expression);

    /**
     * Quoting functions
     */

    boolean isQuotedIdentifier(String identifier);

    String getQuotedIdentifier(String identifier, boolean forceCaseSensitive, boolean forceQuotes);

    String getUnquotedIdentifier(String identifier);

    boolean isQuotedString(String string);

    String getQuotedString(String string);

    String getUnquotedString(String string);

    /**
     * Escapes string to make usable inside of SQL queries.
     * Basically it has to escape only ' character which delimits strings.
     * @param string string to escape
     * @return escaped string
     */
    @NotNull
    String escapeString(String string);

    @NotNull
    String unEscapeString(String string);

    /**
     * Encode value to string format (to use it in scripts, e.g. in INSERT/UPDATE statements)
     * @param attribute
     * @param value       original value
     * @param strValue    string representation (default result)
     */
    @NotNull
    String escapeScriptValue(DBSAttributeBase attribute, @NotNull Object value, @NotNull String strValue);

    /**
     * Default multi-value insertion mode
     * Used e.g. to SQL export
     * @return MultiValueInsertMode enum value
     */
    @NotNull
    MultiValueInsertMode getDefaultMultiValueInsertMode();

    String addFiltersToQuery(DBRProgressMonitor monitor, DBPDataSource dataSource, String query, DBDDataFilter filter);

    /**
     * Two-item array containing begin and end of multi-line comments.
     * @return string array or null if multi-line comments are not supported
     */
    @Nullable
    Pair<String, String> getMultiLineComments();

    /**
     * List of possible single-line comment prefixes
     * @return comment prefixes or null if single line comments are nto supported
     */
    String[] getSingleLineComments();

    /**
     * True if anonymous SQL blocks must be finished with delimiter
     */
    boolean isDelimiterAfterQuery();

    /**
     * True if anonymous SQL blocks must be finished with delimiter
     */
    boolean isDelimiterAfterBlock();

    /**
     * True if dialect requires delimiter for a query which starts with @firstKeyword and ends with @lastKeyword
     */
    boolean needsDelimiterFor(String firstKeyword, String lastKeyword);

    /**
     * Reports about broken CRLF. Queries mustn't contain CRLF line feeds, only LF.
     * This actually seems to be Oracle 9 and earlier JDBC driver issue.
     */
    boolean isCRLFBroken();

    @NotNull
    DBDBinaryFormatter getNativeBinaryFormatter();

    @Nullable
    String getTestSQL();

    /**
     * Dual table name.
     * Used to evaluate expressions, call procedures, etc.
     * @return fully qualified table name or null if table name is not needed.
     */
    @Nullable
    String getDualTableName();

    /**
     * Returns true if query is definitely transactional. Otherwise returns false, however it still may be transactional.
     * You need to check query results to ensure that it is not transactional.
     */
    boolean isTransactionModifyingQuery(String queryString);

    @Nullable
    String[] getTransactionCommitKeywords();

    @Nullable
    String[] getTransactionRollbackKeywords();

    @Nullable
    String getColumnTypeModifiers(DBPDataSource dataSource, @NotNull DBSTypedObject column, @NotNull String typeName, @NotNull DBPDataKind dataKind);

    /**
     * Formats stored procedure call. By default returns @sqlText.
     */
    String formatStoredProcedureCall(DBPDataSource dataSource, String sqlText);

    void generateStoredProcedureCall(StringBuilder sql, DBSProcedure proc, Collection<? extends DBSProcedureParameter> parameters);

    boolean isDisableScriptEscapeProcessing();

    boolean supportsAlterTableConstraint();

}
