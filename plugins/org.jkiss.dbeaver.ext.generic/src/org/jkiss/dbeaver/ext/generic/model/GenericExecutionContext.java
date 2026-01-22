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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LocalCacheProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;

/**
 * GenericExecutionContext
 */
public class GenericExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<GenericCatalog, GenericSchema> {
    private static final Log log = Log.getLog(GenericExecutionContext.class);

    private String selectedEntityName;

    public GenericExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource() {
        return (GenericDataSource) super.getDataSource();
    }

    @Nullable
    @Override
    public DBCExecutionContextDefaults<?,?> getContextDefaults() {
        return this;
    }

    void determineSelectedEntity(DBRProgressMonitor monitor) {
        GenericDataSource dataSource = this.getDataSource();

        // Get selected entity (catalog or schema)
        selectedEntityName = null;
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Determine default catalog/schema")) {
            // Note: both catalog and schema might exist. And the active object must be the most concrete one (schema if exists)
            if (CommonUtils.isEmpty(dataSource.getQueryGetActiveDB())) {
                String catalogName;
                String schemaName;
                try {
                    catalogName = session.getCatalog();
                } catch (Throwable e) {
                    catalogName = null; // Seems to be not supported
                    log.debug(e);
                }
                try {
                    schemaName = session.getSchema();
                } catch (Throwable e) {
                    schemaName = null; // Seems to be not supported
                    log.debug(e);
                }

                String selectedObjectType = null;
                if (CommonUtils.isNotEmpty(catalogName)) {
                    selectedEntityName = catalogName;
                    selectedObjectType = GenericConstants.ENTITY_TYPE_CATALOG;
                }
                if (CommonUtils.isNotEmpty(schemaName)) {
                    selectedEntityName = schemaName;
                    selectedObjectType = GenericConstants.ENTITY_TYPE_SCHEMA;
                }

                if (CommonUtils.isNotEmpty(selectedObjectType)) {
                    dataSource.setSelectedEntityType(selectedObjectType);
                    dataSource.setSelectedEntityFromAPI(true);
                }
            } else {
                try {
                    try (JDBCPreparedStatement dbStat = session.prepareStatement(dataSource.getQueryGetActiveDB())) {
                        try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                            resultSet.next();
                            selectedEntityName = JDBCUtils.safeGetStringTrimmed(resultSet, 1);
                            if (!CommonUtils.isEmpty(selectedEntityName)) {
                                // [PostgreSQL]
                                int divPos = selectedEntityName.lastIndexOf(',');
                                if (divPos != -1) {
                                    selectedEntityName = selectedEntityName.substring(divPos + 1);
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    log.debug(e);
                    selectedEntityName = null;
                }
            }
        }

        if (CommonUtils.isEmpty(selectedEntityName)) {
            // If we have only one catalog then it is our selected entity
            if (dataSource.hasCatalogs() && dataSource.getCatalogs().size() == 1) {
                dataSource.setSelectedEntityType(GenericConstants.ENTITY_TYPE_CATALOG);
                selectedEntityName = dataSource.getCatalogs().getFirst().getName();
            } else if (dataSource.hasSchemas() && dataSource.getSchemas().size() == 1) {
                dataSource.setSelectedEntityType(GenericConstants.ENTITY_TYPE_SCHEMA);
                selectedEntityName = dataSource.getSchemas().getFirst().getName();
            }
        }
    }

    public void initDefaultsFrom(DBRProgressMonitor monitor, GenericExecutionContext context) throws DBCException {
        GenericCatalog defaultCatalog = context.getDefaultCatalog();
        String entityName = null;
        if (defaultCatalog != null && context.supportsCatalogChange()) {
            entityName = defaultCatalog.getName();
        } else if (context.supportsSchemaChange()) {
            GenericSchema defaultSchema = context.getDefaultSchema();
            if (defaultSchema != null) {
                entityName = defaultSchema.getName();
            }
        }
        if (entityName != null) {
            GenericDataSource dataSource = getDataSource();
            DBCTransactionManager txnManager = null;
            boolean autoCommit = true;
            boolean needToSetAutocommit = false;
            try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) {
                if (dataSource.isSelectedEntityFromAPI()) {
                    // FIXME: Do not call setCatalog/Schema on legacy ODBC driver
                    if (!dataSource.getContainer().getDriver().isInternalDriver()) {
                        // Use JDBC API to change entity
                        if (context.supportsCatalogChange()) {
                            session.setCatalog(entityName);
                        } else {
                            session.setSchema(entityName);
                        }
                    } else {
                        log.debug("Catalog/schema switch is disabled for legacy drivers");
                    }
                } else {
                    if (CommonUtils.isEmpty(dataSource.getQuerySetActiveDB())) {
                        throw new DBCException("Active database can't be changed for this kind of datasource!");
                    }
                    txnManager = DBUtils.getTransactionManager(this);
                    needToSetAutocommit = txnManager != null && isSupportsTransactions() && !dataSource.supportsCatalogChangeInTransaction();
                    if (needToSetAutocommit) {
                        autoCommit = txnManager.isAutoCommit();
                        if (!autoCommit) {
                            txnManager.setAutoCommit(monitor, true);
                        }
                    }
                    String changeQuery = dataSource.getQuerySetActiveDB().replaceFirst("\\?", Matcher.quoteReplacement(entityName));
                    try (JDBCPreparedStatement dbStat = session.prepareStatement(changeQuery)) {
                        dbStat.execute();
                    }
                }
                selectedEntityName = entityName;
            } catch (SQLException e) {
                throw new DBCException(e, this);
            } finally {
                if (needToSetAutocommit && !autoCommit) {
                    txnManager.setAutoCommit(monitor, false);
                }
            }
        } else {
            selectedEntityName = context.selectedEntityName;
        }
    }

    @Override
    public GenericCatalog getDefaultCatalog() {
        if (GenericConstants.ENTITY_TYPE_CATALOG.equals(getDataSource().getSelectedEntityType())
            && getDefaultObject() instanceof GenericCatalog catalog
        ) {
            return catalog;
        }
        return getDataSource().getDefaultCatalog();
    }

    @Override
    public GenericSchema getDefaultSchema() {
        if (GenericConstants.ENTITY_TYPE_SCHEMA.equals(getDataSource().getSelectedEntityType())
            && getDefaultObject() instanceof GenericSchema schema
        ) {
            return schema;
        }
        return getDataSource().getDefaultSchema();
    }

    @Override
    public boolean supportsCatalogChange() {
        GenericDataSource dataSource = getDataSource();
        if (!(dataSource.getInfo() instanceof GenericDataSourceInfo info)) {
            if (dataSource.isSelectedEntityFromAPI()) {
                return !dataSource.getContainer().getDriver().isInternalDriver();
            } else {
                return CommonUtils.isNotEmpty(dataSource.getQuerySetActiveDB());
            }
        }
        if (dataSource.isSelectedEntityFromAPI() || !CommonUtils.isEmpty(dataSource.getQuerySetActiveDB())) {
            if (CommonUtils.isEmpty(dataSource.getSelectedEntityType())) {
                return dataSource.hasCatalogs() && info.supportsCatalogSelection();
            }
            if (dataSource.hasCatalogs()) {
                return (GenericConstants.ENTITY_TYPE_CATALOG.equals(dataSource.getSelectedEntityType()) || !dataSource.hasSchemas()) && info.supportsCatalogSelection();
            }
        }
        return false;
    }

    @Override
    public boolean supportsSchemaChange() {
        GenericDataSource dataSource = getDataSource();
        if (!(dataSource.getInfo() instanceof GenericDataSourceInfo info)) {
            if (dataSource.isSelectedEntityFromAPI()) {
                return !dataSource.getContainer().getDriver().isInternalDriver();
            } else {
                return CommonUtils.isNotEmpty(dataSource.getQuerySetActiveDB());
            }
        }
        if (dataSource.isSelectedEntityFromAPI() || !CommonUtils.isEmpty(dataSource.getQuerySetActiveDB())) {
            if (CommonUtils.isEmpty(dataSource.getSelectedEntityType())) {
                return !dataSource.hasCatalogs() && dataSource.hasSchemas() && info.supportsSchemaSelection();
            }
            if (dataSource.hasSchemas()) {
                return (GenericConstants.ENTITY_TYPE_SCHEMA.equals(dataSource.getSelectedEntityType()) || !dataSource.hasCatalogs()) && info.supportsSchemaSelection();
            }
        }
        return false;
    }

    @Override
    public void setDefaultCatalog(DBRProgressMonitor monitor, GenericCatalog catalog, GenericSchema schema) throws DBCException {
        if (catalog == null) {
            log.debug("Null current catalog");
            return;
        }
        GenericDataSource dataSource = getDataSource();
        GenericCatalog oldSelectedCatalog = getDefaultCatalog();
        DBCTransactionManager txnManager = null;
        boolean autoCommit = true;
        boolean needToSetAutocommit = false;
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) {
            if (dataSource.isSelectedEntityFromAPI()) {
                if (!dataSource.getContainer().getDriver().isInternalDriver()) {
                    session.setCatalog(catalog.getName());
                } else {
                    log.debug("Catalog change is disabled for legacy drivers");
                }
            } else {
                if (CommonUtils.isEmpty(dataSource.getQuerySetActiveDB())) {
                    throw new DBCException("Active catalog can't be changed for this kind of datasource!");
                }
                txnManager = DBUtils.getTransactionManager(this);
                needToSetAutocommit = txnManager != null && isSupportsTransactions() && !dataSource.supportsCatalogChangeInTransaction();
                if (needToSetAutocommit) {
                    autoCommit = txnManager.isAutoCommit();
                    if (!autoCommit) {
                        txnManager.setAutoCommit(monitor, true);
                    }
                }
                String changeQuery = dataSource.getQuerySetActiveDB().replaceFirst("\\?", Matcher.quoteReplacement(catalog.getName()));
                try (JDBCPreparedStatement dbStat = session.prepareStatement(changeQuery)) {
                    dbStat.execute();
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        } finally {
            if (needToSetAutocommit && !autoCommit) {
                txnManager.setAutoCommit(monitor, false);
            }
        }
        selectedEntityName = catalog.getName();
        dataSource.setSelectedEntityType(GenericConstants.ENTITY_TYPE_CATALOG);

        if (oldSelectedCatalog != null) {
            DBUtils.fireObjectSelect(oldSelectedCatalog, false, this);
        }
        DBUtils.fireObjectSelect(catalog, true, this);
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, GenericSchema schema) throws DBCException {
        if (schema == null) {
            log.debug("Null current schema");
            return;

        }
        GenericSchema oldSelectedSchema = getDefaultSchema();

        setDefaultSchema(monitor, schema.getName());

        if (oldSelectedSchema != null) {
            DBUtils.fireObjectSelect(oldSelectedSchema, false, this);
        }
        DBUtils.fireObjectSelect(schema, true, this);
    }

    private void setDefaultSchema(DBRProgressMonitor monitor, String schemaName) throws DBCException {
        GenericDataSource dataSource = getDataSource();
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE_SET_SCHEMA)) {
            if (dataSource.isSelectedEntityFromAPI()) {
                session.setSchema(schemaName);
            } else {
                if (CommonUtils.isEmpty(dataSource.getQuerySetActiveDB())) {
                    throw new DBCException("Active schema can't be changed for this kind of datasource!");
                }
                String changeQuery = dataSource.getQuerySetActiveDB().replaceFirst("\\?", Matcher.quoteReplacement(schemaName));
                try (JDBCPreparedStatement dbStat = session.prepareStatement(changeQuery)) {
                    dbStat.execute();
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        selectedEntityName = schemaName;
        dataSource.setSelectedEntityType(GenericConstants.ENTITY_TYPE_SCHEMA);
    }

    @Override
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {

        if (useBootstrapSettings) {
            DBPConnectionBootstrap bootstrap = getBootstrapSettings();
            if (!CommonUtils.isEmpty(bootstrap.getDefaultSchemaName()) && this.supportsSchemaChange()) {
                setDefaultSchema(monitor, bootstrap.getDefaultSchemaName());
            }
        }

        String oldEntityName = selectedEntityName;
        DBSObject oldDefaultObject = getDefaultObject();

        try {
            determineSelectedEntity(monitor);
        } catch (Throwable e) {
            log.debug("Error detecting active object", e);
            return false;
        }

        if (!CommonUtils.equalObjects(oldEntityName, selectedEntityName)) {
            final DBSObject newDefaultObject = getDefaultObject();
            if (newDefaultObject != null) {
                DBUtils.fireObjectSelectionChange(oldDefaultObject, newDefaultObject, this);
                return true;
            }
        }
        return false;
    }

    public GenericObjectContainer getDefaultObject() {
        if (!CommonUtils.isEmpty(selectedEntityName)) {
            GenericDataSource dataSource = getDataSource();
            if (dataSource.hasCatalogs() && GenericConstants.ENTITY_TYPE_CATALOG.equals(dataSource.getSelectedEntityType())) {
                return dataSource.getCatalog(selectedEntityName);
            } else if (GenericConstants.ENTITY_TYPE_SCHEMA.equals(dataSource.getSelectedEntityType())) {
                if (dataSource.hasSchemas()) {
                    return dataSource.getSchema(selectedEntityName);
                } else if (dataSource.hasCatalogs()) {
                    List<GenericCatalog> catalogs = dataSource.getCatalogs();
                    if (catalogs.size() == 1) {
                        try {
                            return catalogs.getFirst().getSchema(
                                new LocalCacheProgressMonitor(new VoidProgressMonitor()), selectedEntityName);
                        } catch (DBException e) {
                            log.debug("Error reading schema in the first catalog: " + e.getMessage());
                        }
                    }
                }
            }
            if (dataSource.hasCatalogs()) {
                if (dataSource.getSelectedEntityType() == null || dataSource.getSelectedEntityType().equals(GenericConstants.ENTITY_TYPE_CATALOG)) {
                    return dataSource.getCatalog(selectedEntityName);
                }
            } else if (dataSource.hasSchemas()) {
                // Some drivers (eg. Clickhouse) provide both default catalog and schema (the same value)
                // So when reading schema do not check entity type
                /*if (selectedEntityType == null || selectedEntityType.equals(GenericConstants.ENTITY_TYPE_SCHEMA)) */{
                    return dataSource.getSchema(selectedEntityName);
                }
            }
        }
        return null;
    }

    public String getDefaultCatalogCached() {
        if (!CommonUtils.isEmpty(selectedEntityName)) {
            GenericDataSource dataSource = getDataSource();
            if (dataSource.hasCatalogs()) {
                if (dataSource.getSelectedEntityType() == null ||
                    dataSource.getSelectedEntityType().equals(GenericConstants.ENTITY_TYPE_CATALOG) ||
                    !dataSource.hasSchemas()) {
                    return selectedEntityName;
                }
            }
        }
        return null;
    }

    public String getDefaultSchemaCached() {
        if (!CommonUtils.isEmpty(selectedEntityName)) {
            GenericDataSource dataSource = getDataSource();
            if (!dataSource.hasCatalogs() && dataSource.hasSchemas()) {
                if (dataSource.getSelectedEntityType() == null ||
                    dataSource.getSelectedEntityType().equals(GenericConstants.ENTITY_TYPE_SCHEMA) ||
                    !dataSource.hasCatalogs()) {
                    return selectedEntityName;
                }
            }
        }
        return null;
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(getDefaultCatalogCached(), getDefaultSchemaCached());
    }
}
