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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

public class MySQLSequence implements DBSSequence, MySQLSourceObject, DBPQualifiedObject {

    private static final Log log = Log.getLog(MySQLSequence.class);

    private final long MAX_SEQUENCE_VALUE = 9223372036854775806L;
    private final long MIN_SEQUENCE_VALUE = -9223372036854775807L;

    private MySQLCatalog sequenceCatalog;
    private String name;
    private String body;
    private Number incrementBy;
    private Number minValue;
    private Number maxValue;
    private Number startValue;
    private Number cacheSize;
    private boolean isCycle;

    private boolean isPersisted;

    private boolean isInfoLoaded;

    public MySQLSequence(@NotNull MySQLCatalog mySQLCatalog, String sequenceName) {
        this.sequenceCatalog = mySQLCatalog;
        this.name = sequenceName;
        this.isPersisted = true;
    }

    public MySQLSequence(@NotNull MySQLCatalog mySQLCatalog, String sequenceName, boolean persisted) {
        this.sequenceCatalog = mySQLCatalog;
        this.name = sequenceName;
        this.isPersisted = persisted;
        incrementBy = 1;
        minValue = 1;
        maxValue = MAX_SEQUENCE_VALUE;
        cacheSize = 1000;
        isCycle = false;
    }

    private void loadInfo(DBRProgressMonitor monitor) {
        if (!isInfoLoaded && !CommonUtils.isEmpty(name)) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load sequence info")) {
                try (JDBCStatement dbStat = session.createStatement()) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT * FROM " + getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                        if (dbResult != null && dbResult.nextRow()) {
                            incrementBy = CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "increment"));
                            minValue = CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "minimum_value"));
                            maxValue = CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "maximum_value"));
                            startValue = CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "start_value"));
                            cacheSize = CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "cache_size"));
                            int cycleOption = JDBCUtils.safeGetInt(dbResult, "cycle_option");
                            if (cycleOption == 1) {
                                isCycle = true;
                            }
                            isInfoLoaded = true;
                        }
                    }
                }
            } catch (SQLException | DBCException e) {
                log.debug("Error reading sequence info", e);
            }
        }
    }

    @Override
    public Number getLastValue() {
        // SELECT LASTVAL(name)
        return null;
    }

    @Override
    public Number getIncrementBy() {
        return incrementBy;
    }

    @Property(viewable = true, order = 2)
    public Number getIncrementBy(DBRProgressMonitor monitor) {
        if (incrementBy == null) {
            loadInfo(monitor);
        }
        return incrementBy;
    }

    @Override
    public Number getMinValue() {
        return minValue;
    }

    @Property(viewable = true, order = 3)
    public Number getMinValue(DBRProgressMonitor monitor) {
        if (minValue == null) {
            loadInfo(monitor);
        }
        return minValue;
    }

    @Override
    public Number getMaxValue() {
        return maxValue;
    }

    @Property(viewable = true, order = 4)
    public Number getMaxValue(DBRProgressMonitor monitor) {
        if (maxValue == null) {
            loadInfo(monitor);
        }
        return maxValue;
    }

    @Property(viewable = true, order = 5)
    public Number getCache(DBRProgressMonitor monitor) {
        if (cacheSize == null) {
            loadInfo(monitor);
        }
        return cacheSize;
    }

    @Property(viewable = true, order = 6)
    public Number getStartValue(DBRProgressMonitor monitor) {
        if (startValue == null) {
            loadInfo(monitor);
        }
        return startValue;
    }

    @Property(viewable = true, order = 7)
    public boolean isCycle(DBRProgressMonitor monitor) {
        return isCycle;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return sequenceCatalog;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return sequenceCatalog.getDataSource();
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return isPersisted;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
                sequenceCatalog,
                this);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        body = sourceText;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (body == null && isPersisted) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read sequence declaration")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CREATE SEQUENCE " + getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.next()) {
                            String sequenceDDL = JDBCUtils.safeGetString(dbResult, "Create Table");
                            if (!CommonUtils.isEmpty(sequenceDDL)) {
                                body = sequenceDDL.replaceAll("CREATE SEQUENCE", "CREATE OR REPLACE SEQUENCE");
                            }
                        } else {
                            body = "-- Sequence definition not found in catalog";
                        }
                    }
                }
            } catch (SQLException e) {
                body = "-- " + e.getMessage();
                throw new DBDatabaseException(e, getDataSource());
            }
        } else if (body == null && !CommonUtils.isEmpty(name)) {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE OR REPLACE SEQUENCE ").append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");
            if (incrementBy != null) {
                sb.append("INCREMENT BY ").append(getIncrementBy()).append(" ");
            }
            if (minValue != null && minValue.longValue() != MIN_SEQUENCE_VALUE) {
                sb.append("MINVALUE ").append(getMinValue()).append(" ");
            }
            if (maxValue != null && maxValue.longValue() != MAX_SEQUENCE_VALUE) {
                sb.append("MAXVALUE ").append(getMaxValue()).append(" ");
            }
            if (isCycle) {
                sb.append("CYCLE ");
            } else {
                sb.append("NOCYCLE ");
            }
            if (cacheSize != null && cacheSize.longValue() != 1000) {
                sb.append("CACHE ").append(cacheSize).append(" ");
            }
            return sb.toString();
        }
        return body;
    }

    public MySQLCatalog getCatalog() {
        return sequenceCatalog;
    }
}
