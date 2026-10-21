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
package org.jkiss.dbeaver.ext.generic.model.meta.hardcharger;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

/**
 * Reads metadata that Warp SQL does not expose through JDBC from Hardcharger's DuckDB catalogs.
 */
public class HardchargerMetaModel extends GenericMetaModel implements DBCQueryTransformProvider {

    @NotNull
    @Override
    public GenericDataSource createDataSourceImpl(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        return new HardchargerDataSource(monitor, container, this);
    }

    @NotNull
    @Override
    public String getViewDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericView sourceObject,
        @NotNull Map<String, Object> options
    ) throws DBException {
        GenericCatalog catalog = sourceObject.getCatalog();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, ModelMessages.model_jdbc_read_database_meta_data)) {
            String ddl = JDBCUtils.queryString(
                session,
                "SELECT sql FROM duckdb_views() WHERE database_name = coalesce(?, current_database())"
                    + " AND schema_name = ? AND view_name = ?",
                catalog == null ? null : catalog.getName(),
                sourceObject.getContainer().getName(),
                sourceObject.getName()
            );
            return CommonUtils.isEmpty(ddl) ? "-- View definition not available" : ddl;
        } catch (SQLException e) {
            throw new DBDatabaseException(e, sourceObject.getDataSource());
        }
    }

    @NotNull
    @Override
    public JDBCStatement prepareIndexesLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase forParent
    ) throws SQLException {
        // Unlike the generic wildcard, null asks Hardcharger JDBC 2.0.1+ for all tables' indexes.
        GenericCatalog catalog = owner.getCatalog();
        GenericSchema schema = owner.getSchema();
        return session.getMetaData().getIndexInfo(
            catalog == null ? null : catalog.getName(),
            schema == null ? null : schema.getName(),
            forParent == null ? null : forParent.getName(),
            false,
            true
        ).getSourceStatement();
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        return type == DBCQueryTransformType.RESULT_SET_LIMIT ? new QueryTransformerLimit(false, true) : null;
    }
}
