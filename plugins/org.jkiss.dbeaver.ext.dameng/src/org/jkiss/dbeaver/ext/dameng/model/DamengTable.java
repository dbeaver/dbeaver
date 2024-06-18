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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableTrigger;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shengkai Bai
 */
public class DamengTable extends GenericTable implements DBPObjectStatistics {

    private long tableSize = -1;

    public DamengTable(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    public DamengTable(GenericStructContainer container, String tableName, String tableCatalogName, String tableSchemaName) {
        super(container, tableName, tableCatalogName, tableSchemaName);
    }

    @Override
    public boolean hasStatistics() {
        return tableSize != -1;
    }

    @Override
    public long getStatObjectSize() {
        return tableSize;
    }

    void fetchStatistics(JDBCResultSet dbResult) throws SQLException {
        tableSize = dbResult.getLong("DISK_SIZE");
    }

    @Override
    public List<? extends GenericTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this.getSchema(), "Read table triggers")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT TABTRIG_OBJ_INNER.NAME " +
                    "FROM " +
                    "SYSOBJECTS TABTRIG_OBJ_INNER, " +
                    "SYSOBJECTS TAB_OBJ_INNER, " +
                    "SYSOBJECTS SCH_OBJ_INNER " +
                    "WHERE " +
                    "TABTRIG_OBJ_INNER.SUBTYPE$ = 'TRIG' " +
                    "AND TABTRIG_OBJ_INNER.PID = TAB_OBJ_INNER.ID " +
                    "AND SCH_OBJ_INNER.ID = TABTRIG_OBJ_INNER.SCHID " +
                    "AND SCH_OBJ_INNER.NAME = ? " +
                    "AND TAB_OBJ_INNER.NAME = ?")) {
                dbStat.setString(1, this.getSchema().getName());
                dbStat.setString(2, this.getName());
                List<GenericTrigger> result = new ArrayList<>();
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        result.add(new GenericTableTrigger(this, name, null));
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBException("Read table triggers failed", e);
        }
    }

    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }
}
