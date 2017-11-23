/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.vertica.VerticaUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * VerticaMetaModel
 */
public class VerticaMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    private static final Log log = Log.getLog(VerticaMetaModel.class);

    public VerticaMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new VerticaDataSource(monitor, container, this);
    }

    @Override
    public GenericSchema createSchemaImpl(GenericDataSource dataSource, GenericCatalog catalog, String schemaName) throws DBException {
        return new VerticaSchema(dataSource, catalog, schemaName);
    }

    @Override
    public GenericTable createTableImpl(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        return new VerticaTable(container, tableName, tableType, dbResult);
    }

    @Override
    public String getTableDDL(DBRProgressMonitor monitor, GenericTable sourceObject, Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        if (sourceObject.isPersisted()) {
            return VerticaUtils.getObjectDDL(monitor, dataSource, sourceObject);
        } else {
            return super.getTableDDL(monitor, sourceObject, options);
        }
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject, Map<String, Object> options) throws DBException {
        return getTableDDL(monitor, sourceObject, options);
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read Vertica procedure source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT function_definition FROM v_catalog.user_functions WHERE schema_name=? AND function_name=?")) {
                dbStat.setString(1, sourceObject.getSchema().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.nextRow()) {
                        sql.append(dbResult.getString(1));
                    }
                    return sql.toString();
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public boolean supportsSequences(GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(DBRProgressMonitor monitor, GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container.getDataSource(), "Read system sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM v_catalog.sequences WHERE sequence_schema=? ORDER BY sequence_name")) {
                dbStat.setString(1, container.getSchema().getName());
                List<GenericSequence> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, "sequence_name");
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        GenericSequence sequence = new GenericSequence(
                            container,
                            name,
                            null,
                            JDBCUtils.safeGetLong(dbResult, "current_value"),
                            JDBCUtils.safeGetLong(dbResult, "minimum"),
                            JDBCUtils.safeGetLong(dbResult, "maximum"),
                            JDBCUtils.safeGetLong(dbResult, "increment_by")
                        );
                        result.add(sequence);
                    }
                }
                return result;

            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false);
        }
        return null;
    }
}
