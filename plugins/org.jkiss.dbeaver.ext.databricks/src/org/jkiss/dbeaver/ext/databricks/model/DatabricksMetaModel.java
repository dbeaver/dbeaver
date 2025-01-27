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
package org.jkiss.dbeaver.ext.databricks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.databricks.DatabricksConstants;
import org.jkiss.dbeaver.ext.databricks.DatabricksDataSource;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabricksMetaModel extends GenericMetaModel implements DBCQueryTransformProvider {

    private static final Log log = Log.getLog(DatabricksMetaModel.class);

    private List<ViewInfo> tempViewsList = new ArrayList<>();

    public DatabricksMetaModel() {
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false, false);
        }
        return null;
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new DatabricksDataSource(monitor, container, this);
    }

    @Override
    public GenericSchema createSchemaImpl(
        @NotNull GenericDataSource dataSource,
        @Nullable GenericCatalog catalog,
        @NotNull String schemaName
    ) {
        return new DatabricksSchema(dataSource, catalog, schemaName);
    }

    @Override
    public List<GenericSchema> loadSchemas(
        JDBCSession session,
        GenericDataSource dataSource,
        GenericCatalog catalog
    ) throws DBException {
        List<GenericSchema> schemas = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SHOW DATABASES IN " + catalog.getName()
        )) {
            dbStat.executeStatement();
            try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                while (dbResult.next()) {
                    String schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, DatabricksConstants.SCHEMA_NAME);
                    schemas.add(new DatabricksSchema(dataSource, catalog, schemaName));
                }
            }
        } catch (SQLException e) {
            log.error("Cannot load schemas", e);
        }

        return schemas;
    }

    @Override
    public boolean isSystemSchema(GenericSchema schema) {
        return "global_temp".equals(schema.getName()) || "information_schema".equalsIgnoreCase(schema.getName());
    }

    @Override
    public JDBCStatement prepareTableLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase object,
        @Nullable String objectName
    ) throws SQLException {
        boolean catalogChanged = false;
        DBSCatalog originalDefaultCatalog = null;
        final DBCExecutionContextDefaults contextDefaults = session.getExecutionContext().getContextDefaults();
        try {
            // First we need to get views list, because for some reason they are in the tables list with the TABLE type.
            // But this query we can use only in the default catalog
            if (contextDefaults != null) {
                originalDefaultCatalog = contextDefaults.getDefaultCatalog();
                if (originalDefaultCatalog != owner.getCatalog()) {
                    contextDefaults.setDefaultCatalog(session.getProgressMonitor(), owner.getCatalog(), null);
                    catalogChanged = true;
                }
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW VIEWS IN " + DBUtils.getQuotedIdentifier(owner))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String namespace = JDBCUtils.safeGetString(dbResult, "namespace");
                        if (CommonUtils.isEmpty(namespace)) {
                            // Probably temporary view
                            continue;
                        }
                        String viewName = JDBCUtils.safeGetString(dbResult, "viewName");
                        if (CommonUtils.isNotEmpty(viewName)) {
                            tempViewsList.add(new ViewInfo(owner, viewName));
                        }
                    }
                }
            }
        } catch (SQLException | DBCException e) {
            log.debug("Can't read current views list", e);
        } finally {
            if (catalogChanged && originalDefaultCatalog != null) {
                try {
                    contextDefaults.setDefaultCatalog(session.getProgressMonitor(), originalDefaultCatalog, null);
                } catch (DBCException e) {
                    log.debug("Can't set original default catalog", e);
                }
            }
        }
        return super.prepareTableLoadStatement(session, owner, object, objectName);
    }

    @Override
    public GenericTableBase createTableOrViewImpl(
        GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult) {
        if ((CommonUtils.isNotEmpty(tableName) && !tempViewsList.isEmpty()
             && tempViewsList.stream().anyMatch(e -> e.name.equalsIgnoreCase(tableName))) ||
            tableType != null && isView(tableType))
        {
            return new DatabricksView(
                container,
                tableName,
                tableType,
                dbResult);
        } else {
            return new DatabricksTable(
                container,
                tableName,
                tableType,
                dbResult);
        }
    }

    @Override
    public boolean supportsTableDDLSplit(GenericTableBase sourceObject) {
        return false;
    }

    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase sourceObject, @NotNull Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Databricks view/table source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW CREATE TABLE " + sourceObject.getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.next()) {
                        String line = dbResult.getString(1);
                        if (line == null) {
                            continue;
                        }
                        sql.append(line).append("\n");
                    }
                    String ddl = sql.toString();
                    if (sourceObject.isView()) {
                        ddl = ddl.replace("CREATE VIEW", "CREATE OR REPLACE VIEW");
                        return SQLFormatUtils.formatSQL(sourceObject.getDataSource(), ddl);
                    }
                    return ddl;
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject, @NotNull Map<String, Object> options) throws DBException {
        return getTableDDL(monitor, sourceObject, options);
    }

    private static class ViewInfo {
        GenericStructContainer schema;
        String name;

        ViewInfo(GenericStructContainer schema, String name) {
            this.schema = schema;
            this.name = name;
        }
    }
}
