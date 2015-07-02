/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model;

import org.osgi.framework.Version;

import java.util.Collection;

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
     * Database server version
     * @return server version
     */
    Version getDatabaseVersion();

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
     * Retrieves whether this database supports stored code (procedures, functions, packages, etc).
     * @return true or false
     */
    boolean supportsStoredCode();

    /**
     * Retrieves list of supported transaction isolation levels
     * @return list of supported transaction isolation levels
     */
    Collection<DBPTransactionIsolation> getSupportedTransactionsIsolation();

    boolean supportsBatchUpdates();

    boolean supportsResultSetLimit();

    boolean supportsResultSetScroll();

    /**
     * Dynamic metadata means that each execution of the same query may produce different results.
     */
    boolean isDynamicMetadata();

    /**
     * Checks whether this data source supports multiple results for a single statement
     */
    boolean supportsMultipleResults();
}
