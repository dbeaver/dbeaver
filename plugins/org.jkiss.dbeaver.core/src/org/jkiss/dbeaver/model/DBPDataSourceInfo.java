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

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.exec.DBCStateType;

import java.util.Collection;

/**
 * DBPDataSourceInfo
 */
public interface DBPDataSourceInfo
{
    public static final int USAGE_NONE = 0;
    public static final int USAGE_DML = 1;
    public static final int USAGE_DDL = 2;
    public static final int USAGE_PROC = 4;
    public static final int USAGE_INDEX = 8;
    public static final int USAGE_PRIV = 8;
    public static final int USAGE_ALL = 256;

    /**
     * Retrieves whether this database is in read-only mode.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    boolean isReadOnlyData();
    /**
     * Retrieves whether this database is in read-only mode.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    boolean isReadOnlyMetaData();

    /**
     * Retrieves the name of this database product.
     *
     * @return database product name
     */
    String getDatabaseProductName();

    /**
     * Retrieves the version number of this database product.
     *
     * @return database version number
     */
    String getDatabaseProductVersion();

    /**
     * Retrieves the name of this driver.
     *
     * @return driver name
     */
    String getDriverName();

    /**
     * Retrieves the version number of this driver as a <code>String</code>.
     *
     * @return driver version
     */
    String getDriverVersion();

    /**
     * Retrieves the string used to quote SQL identifiers.
     * This method returns a space " " if identifier quoting is not supported.
     *
     * @return the quoting string or a space if quoting is not supported
     */
    String getIdentifierQuoteString();

    /**
     * Retrieves a list of all of this database's SQL keywords
     * that are NOT also SQL92 keywords.
     *
     * @return the list of this database's keywords that are not also
     *         SQL92 keywords
     */
    Collection<String> getSQLKeywords();

    /**
     * Retrieves a list of math functions available with
     * this database.  These are the Open /Open CLI math function names used in
     * the function escape clause.
     *
     * @return the list of math functions supported by this database
     */
    Collection<String> getNumericFunctions();

    /**
     * Retrieves a list of string functions available with
     * this database.  These are the  Open Group CLI string function names used
     * in the function escape clause.
     *
     * @return the list of string functions supported by this database
     */
    Collection<String> getStringFunctions();

    /**
     * Retrieves a list of system functions available with
     * this database.  These are the  Open Group CLI system function names used
     * in the function escape clause.
     *
     * @return a list of system functions supported by this database
     */
    Collection<String> getSystemFunctions();

    /**
     * Retrieves a list of the time and date functions available
     * with this database.
     *
     * @return the list of time and date functions supported by this database
     */
    Collection<String> getTimeDateFunctions();

    /**
     * Retrieves a list of execute keywords. If database doesn't support implicit execute returns empty list or null.
     * @return the list of execute keywords.
     */
    Collection<String> getExecuteKeywords();

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
     * Retrieves the database vendor's preferred term for "schema".
     *
     * @return the vendor term for "schema"
     */
    String getSchemaTerm();

    /**
     * Retrieves the database vendor's preferred term for "procedure".
     *
     * @return the vendor term for "procedure"
     */
    String getProcedureTerm();

    /**
     * Retrieves the database vendor's preferred term for "catalog".
     *
     * @return the vendor term for "catalog"
     */
    String getCatalogTerm();

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
    DBCStateType getSQLStateType();


    /**
     * Retrieves whether this database supports transactions. If not, invoking the
     * method <code>commit</code> is a noop.
     *
     * @return <code>true</code> if transactions are supported;
     *         <code>false</code> otherwise
     */
    boolean supportsTransactions();

    /**
     * Retrieves whether this database supports savepoints.
     *
     * @return <code>true</code> if savepoints are supported;
     *         <code>false</code> otherwise
     */
    boolean supportsSavepoints();

    /**
     * Retrieves whether this database supports referential integrity (foreign keys and checks).
     * @return true or false
     */
    boolean supportsReferentialIntegrity();

    /**
     * Retrieves whether this database supports indexes.
     * @return true or false
     */
    boolean supportsIndexes();

    /**
     * Retrieves list of supported transaction isolation levels
     * @return list of supported transaction isolation levels
     */
    Collection<DBPTransactionIsolation> getSupportedTransactionsIsolation();

    /**
     * Script delimiter character
     * @return script delimiter mark
     */
    String getScriptDelimiter();

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
