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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTrigger;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * MySQLTrigger
 */
public class MySQLTrigger extends AbstractTrigger implements MySQLSourceObject, DBPRefreshableObject
{
    private MySQLDatabase catalog;
    private MySQLTable table;
    private String body;
    private String charsetClient;
    private String sqlMode;

    public MySQLTrigger(
        MySQLDatabase catalog,
        MySQLTable table,
        ResultSet dbResult)
    {
        super(JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME"), null, true);
        this.catalog = catalog;
        this.table = table;

        setManipulationType(DBSManipulationType.getByName(JDBCUtils.safeGetString(dbResult, "EVENT_MANIPULATION")));
        setActionTiming(DBSActionTiming.getByName(JDBCUtils.safeGetString(dbResult, "ACTION_TIMING")));
        this.charsetClient = JDBCUtils.safeGetString(dbResult, "CHARACTER_SET_CLIENT");
        this.sqlMode = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TRIGGER_SQL_MODE);
    }

    public MySQLTrigger(
        MySQLDatabase catalog,
        MySQLTable table,
        String name)
    {
        super(name, null, false);
        this.catalog = catalog;
        this.table = table;

        setActionTiming(DBSActionTiming.AFTER);
        setManipulationType(DBSManipulationType.INSERT);
        this.body = "";
    }

    public MySQLTrigger(MySQLDatabase catalog, MySQLTable table, MySQLTrigger source) {
        super(source.name, source.getDescription(), false);
        this.catalog = catalog;
        this.table = table;
        this.body = source.body;
        this.charsetClient = source.charsetClient;
        this.sqlMode = source.sqlMode;
    }

    public String getBody()
    {
        return body;
    }

    public MySQLDatabase getCatalog() {
        return catalog;
    }

    @Override
    @Property(viewable = true, order = 4)
    public MySQLTable getTable()
    {
        return table;
    }

    @Property(order = 5)
    public String getCharsetClient()
    {
        return charsetClient;
    }

    @Property(order = 6)
    public String getSqlMode()
    {
        return sqlMode;
    }

    @Override
    public MySQLTable getParentObject()
    {
        return table;
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return catalog.getDataSource();
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        if (body == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read trigger declaration")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CREATE TRIGGER " + getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.next()) {
                            body = JDBCUtils.safeGetString(dbResult, "SQL Original Statement");
                        } else {
                            body = "-- Trigger definition not found in catalog";
                        }
                    }
                }
            } catch (SQLException e) {
                body = "-- " + e.getMessage();
                throw new DBException(e, getDataSource());
            }
        }
        return body;
    }

    @Override
    public void setObjectDefinitionText(String sourceText)
    {
        body = sourceText;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            catalog,
            this);
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        return getCatalog().triggerCache.refreshObject(monitor, getCatalog(), this);
    }
}
