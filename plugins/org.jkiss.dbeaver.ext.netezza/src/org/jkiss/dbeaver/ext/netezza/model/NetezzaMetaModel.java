/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.sql.SQLException;
import java.util.Map;

/**
 * NetezzaMetaModel
 */
public class NetezzaMetaModel extends GenericMetaModel
{
    public NetezzaMetaModel() {
        super();
    }

    @Override
    public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forParent) throws SQLException {
        JDBCPreparedStatement dbStat;
        if (forParent != null) {
            dbStat = session.prepareStatement("SELECT"
                    + " RELATION AS TABLE_NAME, ATTNAME as COLUMN_NAME, CONSTRAINTNAME AS PK_NAME, CONTYPE"
                    + " FROM _v_relation_keydata"
                    + " WHERE DATABASE=? AND SCHEMA=? AND RELATION=? AND (CONTYPE='u' OR CONTYPE='p')");
            dbStat.setString(1, forParent.getDataSource().getName());
            dbStat.setString(2, forParent.getSchema().getName());
            dbStat.setString(3, forParent.getName());
        } else {
            dbStat = session.prepareStatement("SELECT"
                    + " RELATION AS TABLE_NAME, ATTNAME as COLUMN_NAME, CONSTRAINTNAME AS PK_NAME, CONTYPE"
                    + " FROM _v_relation_keydata");
        }
        return dbStat;
    }

    @Override
    public DBSEntityConstraintType getUniqueConstraintType(JDBCResultSet dbResult) {
        String type = JDBCUtils.safeGetString(dbResult, "CONTYPE");
        if (type != null && type.equals("u")) {
            return DBSEntityConstraintType.UNIQUE_KEY;
        }
        return DBSEntityConstraintType.PRIMARY_KEY;
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Netezza view source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT v.definition " +
                "FROM _v_view v " +
                "WHERE v.VIEWNAME=?"))
            {
                //dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(1, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return
                            "CREATE OR REPLACE VIEW " + sourceObject.getName() + " AS\n" +
                            dbResult.getString(1);
                    }
                    return "-- Netezza view definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Netezza procedure source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p.proceduresignature,p.returns,p.proceduresource " +
                "FROM _v_procedure p " +
                "WHERE p.procedure=?"))
            {
                //dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(1, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return
                            "CREATE OR REPLACE PROCEDURE " + dbResult.getString(1) + " RETURNS " + dbResult.getString(2) +
                            "LANGUAGE NZPLSQL AS BEGIN_PROC\n" + dbResult.getString(3) + "\nEND_PROC;";
                    }
                    return "-- Netezza procedure source not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

}
