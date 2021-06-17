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

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.osgi.framework.Version;

import java.util.Collection;
import java.util.Map;

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
     * Detailed server information.
     * @return server version details or null
     */
    Map<String, Object> getDatabaseProductDetails();

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
     * Retrieves whether this database supports transactions for DDLs. If not, then
     * DDL commands will use transaction mode from the session rather than using
     * transactions on their own.
     *
     * @return {@code true} if the transactions inside DDLs are supported, {@code false} otherwise
     */
    boolean supportsTransactionsForDDL();

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

    boolean supportsResultSetOrdering();

    boolean supportsNullableUniqueConstraints();

    /**
     * Dynamic metadata means that each execution of the same query may produce different results.
     */
    boolean isDynamicMetadata();

    /**
     * Checks whether this data source supports multiple results for a single statement
     */
    boolean supportsMultipleResults();

    /**
     * Workaround for broken drivers (#2792)
     */
    boolean isMultipleResultsFetchBroken();

    DBSObjectType[] getSupportedObjectTypes();

    boolean needsTableMetaForColumnResolution();
}
