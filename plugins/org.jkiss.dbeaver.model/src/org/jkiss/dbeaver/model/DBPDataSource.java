/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSInstanceContainer;

/**
 * Data Source.
 * Root object of all database structure and data objects.
 * Usually represents a database server.
 *
 * Note: do not store direct references on datasource objects in any GUI components -
 * datasource instance may be refreshed at any moment. Obtain references on datasource only
 * from DBSObject or DBPContextProvider interfaces.
 */
public interface DBPDataSource extends DBSInstanceContainer, DBPContextWithAttributes {
    /**
     * Row limit (setMaxSize) affects DML (UPDATE, INSERT, etc) statements.
     */
    String FEATURE_LIMIT_AFFECTS_DML = "datasource.limit-affects-dml";
    /**
     * LOB value operations require enabled transactions. I.e. LOB locator life time is 1 transaction.
     */
    String FEATURE_LOB_REQUIRE_TRANSACTIONS = "datasource.lob-require-transactions";
    /**
     * Max string length. Used by data transfer/compare/migration tools.
     * null means "unknown", -1 means any length (i.e. explicit length is not needed)
     */
    String FEATURE_MAX_STRING_LENGTH = "datasource.max-string-type-length";
    /**
     * Document data source result set representation.
     */
    String FEATURE_DOCUMENT_DATA_SOURCE = "datasource.document-data-source";
    /** False if this data source does not support data transfer imports. */
    String FEATURE_DATA_IMPORT = "datasource.data-import";
    /**
     * The driver needs result-column metadata resolved before opening a cursor-backed table read.
     * This works around drivers for which the normal
     * {@code JDBCFactory.createResultSetMetaData(JDBCResultSet)} path performs metadata lookups
     * while the cursor is active, potentially interfering with the fetch.
     * For streaming exports with a positive fetch size, when the data source advertises this feature,
     * the transfer producer registers
     * {@link org.jkiss.dbeaver.model.struct.DBSDataContainer#CONTEXT_READ_PREPARED_COLUMN_METADATA}
     * on its execution source for the duration of the read. JDBC table reads prepare a
     * {@code JDBCResultSetMetaDataSnapshot} before the first cursor opens and reuse it for
     * matching segments of that transfer, using
     * {@code JDBCFactory.createResultSetMetaData(JDBCSession, JDBCStatement, ResultSetMetaData)} to
     * resolve driver-specific columns. When the result set opens, the snapshot supplies the prepared
     * descriptions to {@code JDBCFactory.createResultSetMetaData(JDBCResultSet, ResultSetMetaData)}.
     * Other reads and result-set operations are unchanged unless the caller requests this behavior.
     */
    String FEATURE_JDBC_TABLE_READ_REQUIRES_PREPARED_COLUMN_METADATA =
        "datasource.jdbc-table-read-requires-prepared-column-metadata";
    /**
     * For paginated JDBC table reads, attempt the dialect's SQL limit transformer instead of JDBC row limits.
     * A successful transformation also disables JDBC fetching for that bounded statement. If it cannot be
     * applied, the read logs an error and falls back to ordinary statement creation and fetch-size handling.
     */
    String FEATURE_JDBC_ATTEMPT_LIMIT_TRANSFORMER_FOR_PAGINATED_TABLE_READ =
        "datasource.jdbc-attempt-limit-transformer-for-paginated-table-read";



    /**
     * Datasource container
     * @return container implementation
     */
    @NotNull
    DBPDataSourceContainer getContainer();

    /**
     * Datasource information/options
     * Info SHOULD be read at datasource initialization stage and should be cached and available
     * at the moment of invocation of this function.
     * @return datasource info.
     */
    @NotNull
    DBPDataSourceInfo getInfo();

    /**
     * Data source feature
     */
    Object getDataSourceFeature(String featureId);

    /**
     * SQL dialect
     */
    @NotNull
    SQLDialect getSQLDialect();

    /**
     * Reads base metadata from remote database or do any necessarily initialization routines.
     * @throws DBException on any DB error
     * @param monitor progress monitor
     */
    void initialize(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Indicates whether the connection is currently refreshing.
     * This method returns a default value of {@code false}, meaning that the connection is not refreshing
     * unless explicitly overridden by an implementing class.
     *
     * @return {@code true} if the connection is being refreshed; otherwise {@code false}.
     */
    default boolean isConnectionRefreshing() {
        return false;
    }
}
