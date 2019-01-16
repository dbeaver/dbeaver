/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
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
 * HANAMetaModel
 */
public class HANAMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(HANAMetaModel.class);

    public HANAMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new HANADataSource(monitor, container, this);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject, Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read HANA view source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT DEFINITION\n" +
                    "FROM SYS.VIEWS\n" +
                    "WHERE SCHEMA_NAME=? and VIEW_NAME=?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return
                            "CREATE VIEW " + sourceObject.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS\n" +
                            dbResult.getString(1);
                    }
                    return "-- HANA view definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read HANA procedure source")) {
            String procedureType = sourceObject.getProcedureType().name();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT SCHEMA_NAME,"+ procedureType + "_NAME,DEFINITION FROM SYS."+ procedureType + "S\n" +
                    "WHERE SCHEMA_NAME = ? AND " + procedureType + "_NAME = ?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return dbResult.getString(3);
                    }
                    return "-- HANA procedure source not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public String getTableDDL(DBRProgressMonitor monitor, GenericTable sourceObject, Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read HANA table DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareCall(
                "CALL get_object_definition(?,?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder ddl = new StringBuilder();
                    while (dbResult.nextRow()) {
                        ddl.append(dbResult.getString(1));
                    }
                    if (ddl.length() > 0) {
                        return ddl.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error reading DDL from HANA server", e);
        }

        return super.getTableDDL(monitor, sourceObject, options);
    }
}
