/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.dpi.DPIContainer;
import org.jkiss.dbeaver.model.dpi.DPIElement;
import org.jkiss.dbeaver.model.dpi.DPIObject;
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
@DPIObject
public interface DBPDataSource extends DBSInstanceContainer, DBPContextWithAttributes
{
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



    /**
     * Datasource container
     * @return container implementation
     */
    @DPIContainer(root = true)
    @NotNull
    DBPDataSourceContainer getContainer();

    /**
     * Datasource information/options
     * Info SHOULD be read at datasource initialization stage and should be cached and available
     * at the moment of invocation of this function.
     * @return datasource info.
     */
    @DPIElement
    @NotNull
    DBPDataSourceInfo getInfo();

    /**
     * Data source feature
     */
    @DPIElement
    Object getDataSourceFeature(String featureId);

    /**
     * SQL dialect
     */
    @DPIElement
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
