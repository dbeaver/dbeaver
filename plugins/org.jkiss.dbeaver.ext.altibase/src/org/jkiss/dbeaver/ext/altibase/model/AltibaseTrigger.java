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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

/**
 * AltibaseTrigger
 */
public abstract class AltibaseTrigger<OWNER extends DBSObject> 
    extends GenericTrigger<DBSObject> implements DBPSystemObject, DBPScriptObject {

    protected boolean isEnable;
    protected String eventTime;
    protected String eventType;
    protected String granularity;
    protected int updateColumnCount;
    protected int refRowCount;
    protected String dmlStmtType;
    protected String dmlTableSchema;
    protected String dmlTableName;

    public AltibaseTrigger(OWNER container, String name, String description, JDBCResultSet dbResult) {
        super(container, name, description);
        this.isEnable           = (JDBCUtils.safeGetInt(dbResult, "IS_ENABLE") == 1);
        this.eventTime          = JDBCUtils.safeGetStringTrimmed(dbResult, "EVENT_TIME");
        this.eventType          = JDBCUtils.safeGetStringTrimmed(dbResult, "EVENT_TYPE");
        this.granularity        = JDBCUtils.safeGetStringTrimmed(dbResult, "GRANULARITY");
        this.updateColumnCount  = JDBCUtils.safeGetInt(dbResult, "UPDATE_COLUMN_CNT");
        this.refRowCount        = JDBCUtils.safeGetInt(dbResult, "REF_ROW_CNT");
        this.dmlStmtType        = JDBCUtils.safeGetString(dbResult, "DML_STMT_TYPE");
        this.dmlTableSchema     = JDBCUtils.safeGetString(dbResult, "DMLTABLE_SCHEMA");
        this.dmlTableName       = JDBCUtils.safeGetString(dbResult, "DMLTABLE_NAME");
    }

    @Override
    public boolean isSystem() {
        return false;
    }

    @Override
    @Property(viewable = true, order = 4)
    public DBSTable getTable() {
        return (DBSTable) getParentObject();
    }

    @Property(viewable = true, order = 5)
    public boolean isEnabled() {
        return isEnable;
    }

    @Property(viewable = true, order = 6)
    public String getEventTime() {
        return eventTime;
    }

    @Property(viewable = true, order = 7)
    public String getEventType() {
        return eventType;
    }

    @Property(viewable = true, order = 8)
    public int getUpdateColumnCount() {
        return updateColumnCount;
    }

    @Property(viewable = true, order = 9)
    public String getGranularity() {
        return granularity;
    }

    @Property(viewable = true, order = 10)
    public String getTargetTable() {
        return (dmlTableSchema == null || dmlTableName == null) ? "" : dmlTableSchema + "." + dmlTableName;
    }

    @Property(viewable = true, order = 11)
    public String getDmlType() {
        return dmlStmtType;
    }

    @Override
    @Property(viewable = false, hidden = true, order = 100)
    public String getDescription() {
        return null;
    }
    
    @Nullable
    public String getSource() {
        return source;
    }
}
