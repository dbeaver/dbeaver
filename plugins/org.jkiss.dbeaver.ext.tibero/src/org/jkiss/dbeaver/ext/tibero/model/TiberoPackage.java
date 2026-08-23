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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OraclePackage;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedurePackaged;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public class TiberoPackage extends OraclePackage implements DBSObjectContainer, DBSPackage, DBSProcedureContainer {

    private final TiberoProceduresCache tiberoProceduresCache = new TiberoProceduresCache();

    public TiberoPackage(OracleSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    public TiberoPackage(OracleSchema schema, String name) {
        super(schema, name);
    }

    @Association
    @Override
    public Collection<OracleProcedurePackaged> getProceduresOnly(DBRProgressMonitor monitor) throws DBException {
        return getProcedures(monitor).stream()
                                     .filter(proc -> proc.getProcedureType() == DBSProcedureType.PROCEDURE)
                                     .collect(Collectors.toList());
    }

    @Association
    @Override
    public Collection<OracleProcedurePackaged> getFunctionsOnly(DBRProgressMonitor monitor) throws DBException {
        return getProcedures(monitor).stream()
                                     .filter(proc -> proc.getProcedureType() == DBSProcedureType.FUNCTION)
                                     .collect(Collectors.toList());
    }

    @Association
    @Override
    public Collection<OracleProcedurePackaged> getProcedures(DBRProgressMonitor monitor) throws DBException {
        return new ArrayList<>(tiberoProceduresCache.getAllObjects(monitor, this));
    }

    @Override
    public OracleProcedurePackaged getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        return tiberoProceduresCache.getObject(monitor, this, uniqueName);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tiberoProceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return tiberoProceduresCache.getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return TiberoProcedurePackaged.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        tiberoProceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        tiberoProceduresCache.clearCache();
        return super.refreshObject(monitor);
    }

    private class TiberoProceduresCache extends JDBCObjectCache<OraclePackage, TiberoProcedurePackaged> {

        @NotNull
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OraclePackage owner)
            throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT P.* \n" +
                "     , CASE WHEN A.DATA_TYPE IS NULL THEN 'PROCEDURE' ELSE 'FUNCTION' END as PROCEDURE_TYPE \n" +
                "FROM ALL_PROCEDURES P\n" +
                "LEFT JOIN ALL_ARGUMENTS A \n" +
                "  ON A.OWNER = P.OWNER\n" +
                " AND A.PACKAGE_NAME = P.OBJECT_NAME\n" +
                " AND A.OBJECT_NAME = P.PROCEDURE_NAME\n" +
                " AND A.ARGUMENT_NAME IS NULL\n" +
                " AND A.DATA_LEVEL = 0\n" +
                "WHERE P.OWNER=? AND P.OBJECT_NAME=?\n" +
                "ORDER BY P.PROCEDURE_NAME");
            dbStat.setString(1, owner.getSchema().getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        @Override
        protected TiberoProcedurePackaged fetchObject(
            @NotNull JDBCSession session,
            @NotNull OraclePackage owner,
            @NotNull JDBCResultSet dbResult
        ) throws SQLException, DBException {
            return new TiberoProcedurePackaged(owner, dbResult);
        }

        @Override
        protected void invalidateObjects(
            DBRProgressMonitor monitor,
            OraclePackage owner,
            Iterator<TiberoProcedurePackaged> objectIter
        ) {
            Map<String, TiberoProcedurePackaged> overloads = new HashMap<>();
            while (objectIter.hasNext()) {
                TiberoProcedurePackaged proc = objectIter.next();
                if (CommonUtils.isEmpty(proc.getName())) {
                    objectIter.remove();
                    continue;
                }
                TiberoProcedurePackaged overload = overloads.get(proc.getName());
                if (overload == null) {
                    overloads.put(proc.getName(), proc);
                } else {
                    if (overload.getOverloadNumber() == null) {
                        overload.setOverload(1);
                    }
                    proc.setOverload(overload.getOverloadNumber() + 1);
                    overloads.put(proc.getName(), proc);
                }
            }
        }
    }
}
