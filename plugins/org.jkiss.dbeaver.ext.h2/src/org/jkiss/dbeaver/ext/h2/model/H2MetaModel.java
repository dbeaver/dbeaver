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
package org.jkiss.dbeaver.ext.h2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * H2MetaModel
 */
public class H2MetaModel extends GenericMetaModel
{
    public H2MetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new H2DataSource(monitor, container, new H2MetaModel());
    }

    @Override
    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read H2 view source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS " +
                    "WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"))
            {
                dbStat.setString(1, sourceObject.getContainer().getName());
                dbStat.setString(2, sourceObject.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        return dbResult.getString(1);
                    }
                    return "-- H2 view definition not found";
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.SEQUENCES")) {
                List<GenericSequence> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME");
                        if (name == null) {
                            continue;
                        }
                        String description = JDBCUtils.safeGetString(dbResult, "REMARKS");
                        GenericSequence sequence = new GenericSequence(
                            container,
                            name,
                            description,
                            JDBCUtils.safeGetLong(dbResult, "CURRENT_VALUE"),
                            JDBCUtils.safeGetLong(dbResult, "MIN_VALUE"),
                            JDBCUtils.safeGetLong(dbResult, "MAX_VALUE"),
                            JDBCUtils.safeGetLong(dbResult, "INCREMENT")
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

    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "AUTO_INCREMENT";
/*
        if (!column.isPersisted()) {
            return "AUTO_INCREMENT";
        } else {
            // For existing columns auto-increment will in DEFAULT clause
            return null;
        }
*/
    }

    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    @Override
    public boolean isTableColumnCommentEditable() {
        return true;
    }

    @Override
    public void loadProcedures(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load functions aliases")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.FUNCTION_ALIASES WHERE ALIAS_SCHEMA = ?")) {
                dbStat.setString(1, container.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.nextRow()) {
                        String aliasName = JDBCUtils.safeGetString(dbResult, "ALIAS_NAME");
                        if (CommonUtils.isEmpty(aliasName)) {
                            continue;
                        }
                        String description = JDBCUtils.safeGetString(dbResult, "REMARKS");
                        int procType = JDBCUtils.safeGetInt(dbResult, "RETURNS_RESULT");
                        DBSProcedureType type = DBSProcedureType.PROCEDURE;
                        GenericFunctionResultType resultType = null;
                        if (procType == 2) {
                            type = DBSProcedureType.FUNCTION;
                            resultType = GenericFunctionResultType.UNKNOWN;
                        }
                        H2RoutineAlias routineAlias = new H2RoutineAlias(container, aliasName, description, type, resultType, dbResult);
                        container.addProcedure(routineAlias);
                    }
                }
            } catch (SQLException e) {
                throw new DBException(e, container.getDataSource());
            }
        }
    }
}
