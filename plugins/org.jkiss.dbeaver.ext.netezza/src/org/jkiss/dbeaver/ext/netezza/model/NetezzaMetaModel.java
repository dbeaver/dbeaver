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
package org.jkiss.dbeaver.ext.netezza.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

/**
 * NetezzaMetaModel
 */
public class NetezzaMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(NetezzaMetaModel.class);

    public NetezzaMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new NetezzaDataSource(monitor, container, this);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        GenericSchema schema = sourceObject.getSchema();
        GenericCatalog catalog = sourceObject.getCatalog();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Netezza view source")) {
            // Set session catalog. View definition can't be accessed in cross-database mode
            String curSessionCatalog = session.getCatalog();
            if (catalog.getName() != null && !CommonUtils.equalObjects(curSessionCatalog, catalog.getName())) {
                session.setCatalog(catalog.getName());
            } else {
                curSessionCatalog = null;
            }
            String sql = "SELECT v.definition " +
                    "FROM " + DBUtils.getQuotedIdentifier(catalog) + ".DEFINITION_SCHEMA._V_VIEW v " +
                    "WHERE v.VIEWNAME=?" + (schema != null ? " AND v.SCHEMA=?" : "");
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql))
            {
                dbStat.setString(1, sourceObject.getName());
                if (schema != null) {
                    dbStat.setString(2, schema.getName());
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return
                            "CREATE OR REPLACE VIEW " + sourceObject.getName() + " AS\n" +
                            dbResult.getString(1);
                    }
                    return "-- Netezza view definition not found";
                }
            } finally {
                if (curSessionCatalog != null) {
                    try {
                        session.setCatalog(curSessionCatalog);
                    } catch (Exception e) {
                        log.debug("Can't set default catalog.", e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        GenericSchema schema = sourceObject.getSchema();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Netezza procedure source")) {
            String sql = "SELECT p.proceduresignature,p.returns,p.proceduresource " +
                    "FROM " + DBUtils.getQuotedIdentifier(sourceObject.getCatalog()) + ".DEFINITION_SCHEMA._V_PROCEDURE p " +
                    "WHERE p.procedure=?" + (schema != null ? " AND p.SCHEMA=?" : "");
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql))
            {
                dbStat.setString(1, sourceObject.getName());
                if (schema != null) {
                    dbStat.setString(2, schema.getName());
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return
                            "CREATE OR REPLACE PROCEDURE " + dbResult.getString(1) + " RETURNS " + dbResult.getString(2) +
                            " LANGUAGE NZPLSQL AS BEGIN_PROC\n" + dbResult.getString(3).trim() + "\nEND_PROC;";
                    }
                    return "-- Netezza procedure source not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

}
