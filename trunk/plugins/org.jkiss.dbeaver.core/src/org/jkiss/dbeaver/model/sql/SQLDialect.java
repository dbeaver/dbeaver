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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.ICommentsSupport;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * SQL dialect
 */
public interface SQLDialect extends ICommentsSupport {

    int USAGE_NONE = 0;
    int USAGE_DML = 1;
    int USAGE_DDL = 2;
    int USAGE_PROC = 4;
    int USAGE_INDEX = 8;
    int USAGE_PRIV = 8;
    int USAGE_ALL = 256;

    /**
     * Dialect name
     * @return SQL dialect name
     */
    String getDialectName();

    /**
     * Retrieves the string used to quote SQL identifiers.
     * This method returns a space " " if identifier quoting is not supported.
     *
     * @return the quoting string or a space if quoting is not supported
     */
    String getIdentifierQuoteString();

    /**
     * Retrieves a list of execute keywords. If database doesn't support implicit execute returns empty list or null.
     * @return the list of execute keywords.
     */
    @Nullable
    Collection<String> getExecuteKeywords();

    /**
     * Retrieves a list of all of this database's SQL keywords
     * that are NOT also SQL92 keywords.
     *
     * @return the list of this database's keywords that are not also
     *         SQL92 keywords
     */
    Set<String> getReservedWords();

    Set<String> getFunctions();

    Set<String> getTypes();

    DBPKeywordType getKeywordType(String word);

    List<String> getMatchedKeywords(String word);

    boolean isKeywordStart(String word);

    boolean isEntityQueryWord(String word);

    boolean isAttributeQueryWord(String word);

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

    DBPIdentifierCase storesUnquotedCase();

    DBPIdentifierCase storesQuotedCase();

}
