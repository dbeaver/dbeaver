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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
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
        PLAIN
    }

    /**
     * Dialect name
     * @return SQL dialect name
     */
    @NotNull
    String getDialectName();

    /**
     * Retrieves the string used to quote SQL identifiers.
     * This method returns a space " " if identifier quoting is not supported.
     *
     * @return the quoting string or a space if quoting is not supported
     */
    @Nullable
    String getIdentifierQuoteString();

    /**
     * Retrieves a list of execute keywords. If database doesn't support implicit execute returns empty list or null.
     * @return the list of execute keywords.
     */
    @NotNull
    Collection<String> getExecuteKeywords();

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
    Set<String> getFunctions();
    @NotNull
    Set<String> getTypes();
    @Nullable
    DBPKeywordType getKeywordType(@NotNull String word);
    @NotNull
    List<String> getMatchedKeywords(@NotNull String word);

    boolean isKeywordStart(@NotNull String word);

    boolean isEntityQueryWord(@NotNull String word);

    boolean isAttributeQueryWord(@NotNull String word);

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

    /**
     * Script delimiter character
     * @return script delimiter mark
     */
    @NotNull
    String getScriptDelimiter();

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

    /**
     * Checks that specified character is a valid identifier part. Non-valid characters should be quoted in queries.
     * @param c character
     * @return true or false
     */
    boolean validUnquotedCharacter(char c);

    boolean supportsUnquotedMixedCase();

    boolean supportsQuotedMixedCase();

    boolean supportsSubqueries();

    boolean supportsAliasInSelect();

    boolean supportsAliasInUpdate();

    @NotNull
    DBPIdentifierCase storesUnquotedCase();

    @NotNull
    DBPIdentifierCase storesQuotedCase();

    /**
     * Escapes string to make usable inside of SQL queries.
     * Basically it has to escape only ' character which delimits strings.
     * @param string string to escape
     * @return escaped string
     */
    @NotNull
    String escapeString(String string);

    @NotNull
    MultiValueInsertMode getMultiValueInsertMode();

    String addFiltersToQuery(DBPDataSource dataSource, String query, DBDDataFilter filter) throws DBException;

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
    boolean isDelimiterAfterBlock();
}
