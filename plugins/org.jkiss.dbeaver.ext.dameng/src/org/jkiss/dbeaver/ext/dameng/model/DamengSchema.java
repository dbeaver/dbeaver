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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.Collection;

/**
 * @author Shengkai Bai
 */
public class DamengSchema extends GenericSchema implements DBPQualifiedObject, DBPObjectStatisticsCollector {

    @NotNull
    private String schemaName;

    private boolean persisted;

    private boolean hasStatistics;

    private SequenceCache sequenceCache = new SequenceCache();

    public DamengSchema(GenericDataSource dataSource, String schemaName, boolean persisted) {
        super(dataSource, null, schemaName);
        this.schemaName = schemaName;
        this.persisted = persisted;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return schemaName;
    }

    public void setName(@NotNull String schemaName) {
        this.schemaName = schemaName;
    }

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics || forceRefresh) {
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT TABLE_NAME,TABLE_USED_PAGES(OWNER,TABLE_NAME) * PAGE AS DISK_SIZE " +
                            "FROM ALL_TABLES " +
                            "WHERE owner = ?"
            );
            dbStat.setString(1, getName());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    String tableName = dbResult.getString(1);
                    DamengTable table = (DamengTable) getTable(monitor, tableName);
                    if (table != null) {
                        table.fetchStatistics(dbResult);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading schema relation statistics", e);
        } finally {
            this.hasStatistics = true;
        }
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), this);
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public synchronized DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        sequenceCache.clearCache();
        return super.refreshObject(monitor);
    }

    public Collection<DamengSequence> getDamengSequences(DBRProgressMonitor monitor) throws DBException {
        return sequenceCache.getAllObjects(monitor, this);
    }


    static class SequenceCache extends JDBCObjectCache<DamengSchema, DamengSequence> {

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, DamengSchema damengSchema) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT " +
                    "SEQ_OBJ.NAME, " +
                    "SEQ_OBJ.INFO4 AS INCREMENT, " +
                    "SF_SEQUENCE_GET_MAX(SCH_OBJ.NAME,SEQ_OBJ.NAME) AS MAX_VALUE," +
                    "SF_SEQUENCE_GET_MIN(SCH_OBJ.NAME,SEQ_OBJ.NAME) AS MIN_VALUE," +
                    "SF_SEQ_CURRVAL(SCH_OBJ.NAME,SEQ_OBJ.NAME) AS LAST_VALUE," +
                    "SF_SEQUENCE_GET_CACHE_NUM(SCH_OBJ.NAME,SEQ_OBJ.NAME) CACHE_SIZE," +
                    "SEQ_OBJ.INFO1 & 0x0000FF IS_CYCLE," +
                    "SEQ_OBJ.INFO1 & 0x00FF00 IS_ORDER," +
                    "SEQ_OBJ.INFO1 & 0xFF0000 IS_CACHE," +
                    "SEQ_OBJ.INFO3 " +
                    "FROM " +
                    "SYSOBJECTS SEQ_OBJ, " +
                    "SYSOBJECTS SCH_OBJ " +
                    "WHERE SEQ_OBJ.SCHID = SCH_OBJ.ID " +
                    "AND SEQ_OBJ.SUBTYPE$ = 'SEQ' " +
                    "AND SCH_OBJ.NAME = ?");
            dbStat.setString(1, damengSchema.getName());
            return dbStat;
        }

        @Override
        protected DamengSequence fetchObject(JDBCSession session, DamengSchema damengSchema, JDBCResultSet resultSet) throws SQLException, DBException {
            return new DamengSequence(damengSchema, resultSet);
        }
    }
}
