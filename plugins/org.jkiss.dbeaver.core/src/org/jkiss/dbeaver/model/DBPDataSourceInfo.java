/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.exec.DBCStateType;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.List;

/**
 * DBPDataSourceInfo
 */
public interface DBPDataSourceInfo
{
    /**
     * Retrieves whether this database is in read-only mode.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    boolean isReadOnly();

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
    List<String> getSQLKeywords();

    /**
     * Retrieves a list of math functions available with
     * this database.  These are the Open /Open CLI math function names used in
     * the function escape clause.
     *
     * @return the list of math functions supported by this database
     */
    List<String> getNumericFunctions();

    /**
     * Retrieves a list of string functions available with
     * this database.  These are the  Open Group CLI string function names used
     * in the function escape clause.
     *
     * @return the list of string functions supported by this database
     */
    List<String> getStringFunctions();

    /**
     * Retrieves a list of system functions available with
     * this database.  These are the  Open Group CLI system function names used
     * in the function escape clause.
     *
     * @return a list of system functions supported by this database
     */
    List<String> getSystemFunctions();

    /**
     * Retrieves a list of the time and date functions available
     * with this database.
     *
     * @return the list of time and date functions supported by this database
     */
    List<String> getTimeDateFunctions();

    /**
     * Retrieves a list of execute keywords. If database doesn't support implicit execute returns empty list or null.
     * @return the list of execute keywords.
     */
    List<String> getExecuteKeywords();

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
     * Retrieves the <code>String</code> that this database uses as the
     * separator between a catalog and table name.
     *
     * @return the separator string
     */
    String getCatalogSeparator();

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
     * Retrieves list of supported transaction isolation levels
     * @return list of supported transaction isolation levels
     */
    List<DBPTransactionIsolation> getSupportedTransactionIsolations();

    /**
     * Retrieves list of supported datatypes
     * @return list of types
     */
    List<DBSDataType> getSupportedDataTypes();

    /**
     * Gets data type with specified name
     * @param typeName type name
     * @return datatype of null
     */
    DBSDataType getSupportedDataType(String typeName);

    /**
     * Script delimiter character
     * @return
     */
    String getScriptDelimiter();
}
