/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * MySQLEvent
 */
public class MySQLEvent extends MySQLInformation implements MySQLSourceObject {

    private static final String CAT_MAIN = "Main";
    private static final String CAT_DETAILS = "Details";
    private static final String CAT_STATS = "Statistics";

    private String name;
    private String definer;
    private String timeZone;
    private String eventBody;
    private String eventDefinition;
    private String eventType;
    private Date executeAt;
    private String intervalValue;
    private String intervalField;
    private String sqlMode;
    private Date starts;
    private Date ends;
    private String status;
    private String onCompletion;
    private Date created;
    private Date lastAltered;
    private Date lastExecuted;
    private String eventComment;
    private long originator;
    private MySQLCharset characterSetClient;
    private MySQLCollation collationConnection;
    private MySQLCollation databaseCollation;

    public MySQLEvent(MySQLCatalog catalog, ResultSet dbResult)
        throws SQLException
    {
        super(catalog.getDataSource());
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, "EVENT_NAME");
        this.definer = JDBCUtils.safeGetString(dbResult, "DEFINER");
        this.timeZone = JDBCUtils.safeGetString(dbResult, "TIME_ZONE");
        this.eventBody = JDBCUtils.safeGetString(dbResult, "EVENT_BODY");
        this.eventDefinition = JDBCUtils.safeGetString(dbResult, "EVENT_DEFINITION");
        this.eventType = JDBCUtils.safeGetString(dbResult, "EVENT_TYPE");
        this.executeAt = JDBCUtils.safeGetTimestamp(dbResult, "EXECUTE_AT");
        this.intervalValue = JDBCUtils.safeGetString(dbResult, "INTERVAL_VALUE");
        this.intervalField = JDBCUtils.safeGetString(dbResult, "INTERVAL_FIELD");
        this.sqlMode = JDBCUtils.safeGetString(dbResult, "SQL_MODE");
        this.starts = JDBCUtils.safeGetTimestamp(dbResult, "STARTS");
        this.ends = JDBCUtils.safeGetTimestamp(dbResult, "ENDS");
        this.status = JDBCUtils.safeGetString(dbResult, "STATUS");
        this.onCompletion = JDBCUtils.safeGetString(dbResult, "ON_COMPLETION");
        this.created = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
        this.lastAltered = JDBCUtils.safeGetTimestamp(dbResult, "LAST_ALTERED");
        this.lastExecuted = JDBCUtils.safeGetTimestamp(dbResult, "LAST_EXECUTED");
        this.eventComment = JDBCUtils.safeGetString(dbResult, "EVENT_COMMENT");
        this.originator = JDBCUtils.safeGetLong(dbResult, "ORIGINATOR");
        this.characterSetClient = getDataSource().getCharset(JDBCUtils.safeGetString(dbResult, "CHARACTER_SET_CLIENT"));
        this.collationConnection = getDataSource().getCollation(JDBCUtils.safeGetString(dbResult, "COLLATION_CONNECTION"));
        this.databaseCollation = getDataSource().getCollation(JDBCUtils.safeGetString(dbResult, "DATABASE_COLLATION"));
    }

    @NotNull
    @Override
    @Property(viewable = true, category = CAT_MAIN, order = 1)
    public String getName()
    {
        return name;
    }

    @Nullable
    @Override
    @Property(viewable = true, category = CAT_DETAILS, order = 100)
    public String getDescription()
    {
        return eventComment;
    }

    @Property(viewable = true, category = CAT_MAIN, order = 10)
    public String getEventType() {
        return eventType;
    }

    @Property(viewable = true, category = CAT_MAIN, order = 11)
    public Date getExecuteAt() {
        return executeAt;
    }

    @Property(viewable = true, category = CAT_MAIN, order = 12)
    public String getIntervalValue() {
        return intervalValue;
    }

    @Property(viewable = true, category = CAT_MAIN, order = 13)
    public String getIntervalField() {
        return intervalField;
    }

    @Property(viewable = true, category = CAT_DETAILS, order = 14)
    public String getEventBody() {
        return eventBody;
    }

    @Property(category = CAT_DETAILS, order = 30)
    public String getDefiner() {
        return definer;
    }

    @Property(category = CAT_DETAILS, order = 31)
    public String getTimeZone() {
        return timeZone;
    }

    @Property(category = CAT_DETAILS, order = 32)
    public String getSqlMode() {
        return sqlMode;
    }

    @Property(category = CAT_DETAILS, order = 33)
    public Date getStarts() {
        return starts;
    }

    @Property(category = CAT_DETAILS, order = 34)
    public Date getEnds() {
        return ends;
    }

    @Property(category = CAT_STATS, order = 35)
    public String getStatus() {
        return status;
    }

    @Property(category = CAT_DETAILS, order = 36)
    public String getOnCompletion() {
        return onCompletion;
    }

    @Property(category = CAT_STATS, order = 37)
    public Date getCreated() {
        return created;
    }

    @Property(category = CAT_STATS, order = 38)
    public Date getLastAltered() {
        return lastAltered;
    }

    @Property(category = CAT_STATS, order = 39)
    public Date getLastExecuted() {
        return lastExecuted;
    }

    @Property(category = CAT_DETAILS, order = 40)
    public long getOriginator() {
        return originator;
    }

    @Property(category = CAT_DETAILS, order = 41)
    public MySQLCharset getCharacterSetClient() {
        return characterSetClient;
    }

    @Property(category = CAT_DETAILS, order = 42)
    public MySQLCollation getCollationConnection() {
        return collationConnection;
    }

    @Property(category = CAT_DETAILS, order = 43)
    public MySQLCollation getDatabaseCollation() {
        return databaseCollation;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return eventDefinition;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        eventDefinition = sourceText;
    }
}
