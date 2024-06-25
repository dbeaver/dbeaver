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
package org.jkiss.dbeaver.ext.denodo.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Map;

/**
 * DenodoMetaModel
 */
public class DenodoMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(DenodoMetaModel.class);

    private static final String vqlDescriptionParameter =
            " ('includeDependencies' = 'no', 'dropElements' = 'no')";

    public DenodoMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new DenodoDataSource(monitor, container, this);
    }

    @Override
    public boolean supportsTableDDLSplit(GenericTableBase sourceObject) {
        return false;
    }

    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject, @NotNull Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();

        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Denodo object DDL")) {

            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "DESC VQL VIEW " + sourceObject.getFullyQualifiedName(DBPEvaluationContext.DDL) + vqlDescriptionParameter))
            {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.nextRow()) {
                        sql.append(dbResult.getString(1));
                    }
                    String result = sql.toString().trim();
                    while (result.endsWith(";")) {
                        result = result.substring(0, result.length() - 1);
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();

        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Denodo object DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "DESC VQL PROCEDURE " + sourceObject.getFullyQualifiedName(DBPEvaluationContext.DDL) + vqlDescriptionParameter))
            {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.nextRow()) {
                        sql.append(dbResult.getString(1));
                    }
                    return sql.toString();
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public boolean isTableCommentEditable() {
        return false;
    }

    @Override
    public boolean isTableColumnCommentEditable() {
        return false;
    }
}
