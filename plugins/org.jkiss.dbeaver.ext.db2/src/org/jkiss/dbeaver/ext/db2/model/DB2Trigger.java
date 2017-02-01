/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.editors.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TriggerDepCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerEvent;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerGranularity;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerTime;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerValid;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2 Table Trigger
 * 
 * @author Denis Forveille
 */
public class DB2Trigger extends DB2SchemaObject implements DBSTrigger, DB2SourceObject, DBPRefreshableObject {

    private final DB2TriggerDepCache triggerDepCache = new DB2TriggerDepCache();

    private DB2Table table;

    private String owner;
    private DB2OwnerType ownerType;
    private DB2TriggerTime time;
    private DB2TriggerEvent event;
    private Boolean eventUpdate;
    private Boolean eventDelete;
    private Boolean eventInsert;
    private DB2TriggerGranularity granularity;
    private DB2TriggerValid valid;
    private Timestamp createTime;
    private String qualifier;
    private String funcPath;
    private String text;
    private Timestamp lastRegenTime;
    private String collationSchema;
    private String collationName;
    private String collationSchemaOrderBy;
    private String collationNameOrderBy;
    private Boolean secure;
    private Timestamp alterTime;
    private Integer libId;
    private String precompileOptions;
    private String compileOptions;
    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2Trigger(DBRProgressMonitor monitor, DB2Schema schema, DB2Table table, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "TRIGNAME"), true);

        this.table = table;

        DB2DataSource db2DataSource = table.getDataSource();

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.time = CommonUtils.valueOf(DB2TriggerTime.class, JDBCUtils.safeGetString(dbResult, "TRIGTIME"));
        this.event = CommonUtils.valueOf(DB2TriggerEvent.class, JDBCUtils.safeGetString(dbResult, "TRIGEVENT"));
        this.granularity = CommonUtils.valueOf(DB2TriggerGranularity.class, JDBCUtils.safeGetString(dbResult, "GRANULARITY"));
        this.valid = CommonUtils.valueOf(DB2TriggerValid.class, JDBCUtils.safeGetString(dbResult, "VALID"));
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.qualifier = JDBCUtils.safeGetString(dbResult, "QUALIFIER");
        this.funcPath = JDBCUtils.safeGetString(dbResult, "FUNC_PATH");
        this.text = JDBCUtils.safeGetString(dbResult, "TEXT");
        this.lastRegenTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REGEN_TIME");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (db2DataSource.isAtLeastV9_5()) {
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
            this.collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATIONSCHEMA");
            this.collationName = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME");
            this.collationSchemaOrderBy = JDBCUtils.safeGetString(dbResult, "COLLATIONSCHEMA_ORDERBY");
            this.collationNameOrderBy = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME_ORDERBY");
        }
        if (db2DataSource.isAtLeastV10_1()) {
            this.eventUpdate = JDBCUtils.safeGetBoolean(dbResult, "EVENTUPDATE", DB2YesNo.Y.name());
            this.eventDelete = JDBCUtils.safeGetBoolean(dbResult, "EVENTDELETE", DB2YesNo.Y.name());
            this.eventInsert = JDBCUtils.safeGetBoolean(dbResult, "EVENTINSERT", DB2YesNo.Y.name());
            this.secure = JDBCUtils.safeGetBoolean(dbResult, "SECURE", DB2YesNo.Y.name());
            this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
            this.libId = JDBCUtils.safeGetInteger(dbResult, "LIB_ID");
            this.precompileOptions = JDBCUtils.safeGetString(dbResult, "PRECOMPILE_OPTIONS");
            this.compileOptions = JDBCUtils.safeGetString(dbResult, "COMPILE_OPTIONS");
        }
    }

    public DB2Trigger(DB2Schema schema, DB2Table table, String name)
    {
        super(schema, name, false);

        this.ownerType = DB2OwnerType.U;
        this.time = DB2TriggerTime.B;
        this.event = DB2TriggerEvent.I;
        this.granularity = DB2TriggerGranularity.R;
        this.valid = DB2TriggerValid.X;

    }

    // -----------------
    // Business contract
    // -----------------

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid.getState();
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
    }

    // -----------------
    // Source
    // -----------------

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        return SQLUtils.formatSQL(getDataSource(), text);
    }

    // -----------------
    // Association
    // -----------------

    @Association
    public Collection<DB2TriggerDep> getTriggerDeps(DBRProgressMonitor monitor) throws DBException
    {
        return triggerDepCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        triggerDepCache.clearCache();
        return this;
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2, id = "Schema")
    public DB2Schema getSchema()
    {
        return super.getParentObject();
    }

    @Override
    @Property(viewable = true, order = 3)
    public DB2Table getTable()
    {
        return table;
    }

    @Property(viewable = true, order = 4)
    public DB2TriggerValid getValid()
    {
        return valid;
    }

    @Property(viewable = true, order = 5)
    public DB2TriggerEvent getEvent()
    {
        return event;
    }

    @Property(viewable = true, order = 6)
    public DB2TriggerTime getTime()
    {
        return time;
    }

    @Property(viewable = true, order = 7)
    public Boolean getEventUpdate()
    {
        return eventUpdate;
    }

    @Property(viewable = true, order = 8)
    public Boolean getEventDelete()
    {
        return eventDelete;
    }

    @Property(viewable = true, order = 9)
    public Boolean getEventInsert()
    {
        return eventInsert;
    }

    @Property(viewable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = true)
    public DB2TriggerGranularity getGranularity()
    {
        return granularity;
    }

    @Property(viewable = false)
    public String getQualifier()
    {
        return qualifier;
    }

    @Property(viewable = false)
    public String getFuncPath()
    {
        return funcPath;
    }

    @Property(viewable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getLastRegenTime()
    {
        return lastRegenTime;
    }

    @Property(viewable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getAlterTime()
    {
        return alterTime;
    }

    @Property(viewable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchema()
    {
        return collationSchema;
    }

    @Property(viewable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationName()
    {
        return collationName;
    }

    @Property(viewable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchemaOrderBy()
    {
        return collationSchemaOrderBy;
    }

    @Property(viewable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationNameOrderBy()
    {
        return collationNameOrderBy;
    }

    @Property(viewable = false)
    public Boolean getSecure()
    {
        return secure;
    }

    @Property(viewable = false)
    public Integer getLibId()
    {
        return libId;
    }

    @Property(viewable = false, category = DB2Constants.CAT_COMPILER)
    public String getPrecompileOptions()
    {
        return precompileOptions;
    }

    @Property(viewable = false, category = DB2Constants.CAT_COMPILER)
    public String getCompileOptions()
    {
        return compileOptions;
    }

    @Nullable
    @Override
    @Property(viewable = false)
    public String getDescription()
    {
        return remarks;
    }

}
