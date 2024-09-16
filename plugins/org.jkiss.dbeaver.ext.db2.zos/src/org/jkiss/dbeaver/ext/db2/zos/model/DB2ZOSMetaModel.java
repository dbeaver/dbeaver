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
package org.jkiss.dbeaver.ext.db2.zos.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.Map;

/**
 * DB2ZOSMetaModel
 */
public class DB2ZOSMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(DB2ZOSMetaModel.class);

    public DB2ZOSMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new DB2ZOSDataSource(monitor, container, this);
    }

    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase sourceObject, @NotNull Map<String, Object> options) throws DBException {
        if (sourceObject instanceof GenericView view) {
            return getViewDDL(monitor, view, options);
        } else {
            return super.getTableDDL(monitor, sourceObject, options);
        }
    }

    @Override
    public boolean supportsTableDDLSplit(GenericTableBase sourceObject) {
        return false;
    }

    @Override
    public boolean isTrimObjectNames() {
        return true;
    }

    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject, @NotNull Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read DB2 z/OS view source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT STATEMENT FROM SYSIBM.SYSVIEWS\n" +
                "WHERE CREATOR = ? AND NAME = ?\n" +
                "WITH UR"))
            {
                dbStat.setString(1, sourceObject.getSchema().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        Clob ddlStmt = dbResult.getClob(1);
                        try {
                            return ddlStmt.getSubString(1, (int) ddlStmt.length());
                        } finally {
                            try {
                                ddlStmt.free();
                            } catch (Throwable e) {
                                log.debug("Error freeing CLOB: " + e.getMessage());
                            }
                        }
                    } else {
                        return "-- View definition not found in system catalog";
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

}
