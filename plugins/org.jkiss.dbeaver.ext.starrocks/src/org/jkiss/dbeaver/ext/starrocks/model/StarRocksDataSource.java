/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StarRocks DataSource - extends GenericDataSource with StarRocks-specific catalog loading
 */
public class StarRocksDataSource extends GenericDataSource {

    public static final String DEFAULT_CATALOG_NAME = "default_catalog"; //$NON-NLS-1$
    private static final String COL_CATALOG = "Catalog"; //$NON-NLS-1$
    private static final String COL_TYPE = "Type"; //$NON-NLS-1$
    private static final String COL_COMMENT = "Comment"; //$NON-NLS-1$

    /**
     * catalog name -> [type, comment]
     */
    private final Map<String, CatalogMetadata> catalogMetadataCache = new ConcurrentHashMap<>();

    public record CatalogMetadata(@Nullable String type, @Nullable String comment) {
    }

    public StarRocksDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull StarRocksMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel, new StarRocksDialect());
    }

    @NotNull
    @Override
    public StarRocksMetaModel getMetaModel() {
        return (StarRocksMetaModel) super.getMetaModel();
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new StarRocksExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCExecutionContext context,
        @Nullable JDBCExecutionContext initFrom
    ) throws DBException {
        StarRocksExecutionContext starRocksContext = (StarRocksExecutionContext) context;
        if (initFrom != null) {
            StarRocksExecutionContext starRocksInitFrom = (StarRocksExecutionContext) initFrom;
            String activeCatalog = starRocksInitFrom.getActiveCatalogName();
            String activeDatabase = starRocksInitFrom.getActiveDatabaseName();
            if (!CommonUtils.isEmpty(activeCatalog)) {
                starRocksContext.setActiveCatalogName(activeCatalog);
            }
            if (!CommonUtils.isEmpty(activeDatabase)) {
                starRocksContext.setActiveDatabaseName(activeDatabase);
            }
        } else {
            starRocksContext.refreshDefaults(monitor, true);
        }
    }

    @Override
    public List<String> getCatalogsNames(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCDatabaseMetaData metaData,
        GenericMetaObject catalogObject,
        @Nullable DBSObjectFilter catalogFilters
    ) throws DBException {
        List<String> catalogNames = new ArrayList<>();
        catalogMetadataCache.clear();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load StarRocks catalogs")) { //$NON-NLS-1$
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CATALOGS")) { //$NON-NLS-1$
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String catalogName = JDBCUtils.safeGetString(dbResult, COL_CATALOG);
                        String catalogType = JDBCUtils.safeGetString(dbResult, COL_TYPE);
                        String catalogComment = JDBCUtils.safeGetString(dbResult, COL_COMMENT);

                        if (catalogName != null) {
                            // Store metadata for later use in createCatalogImpl
                            catalogMetadataCache.put(catalogName, new CatalogMetadata(catalogType, catalogComment));

                            if (catalogFilters == null || catalogFilters.matches(catalogName)) {
                                catalogNames.add(catalogName);
                            } else {
                                catalogsFiltered = true;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Error loading StarRocks catalogs", e);
        }
        return catalogNames;
    }

    @Nullable
    public CatalogMetadata getCatalogMetadata(@NotNull String catalogName) {
        return catalogMetadataCache.get(catalogName);
    }

    @Nullable
    public StarRocksCatalog getCatalog(@NotNull String name) {
        return (StarRocksCatalog) super.getCatalog(name);
    }

    @Override
    public boolean isOmitCatalog() {
        return false;
    }

    @Override
    public boolean isOmitSchema() {
        return false;
    }
}
