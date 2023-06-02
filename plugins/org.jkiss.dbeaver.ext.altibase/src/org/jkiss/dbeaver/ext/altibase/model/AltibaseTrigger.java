/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
public abstract class AltibaseTrigger<OWNER extends DBSObject> extends GenericTrigger<DBSObject> implements DBPSystemObject, DBPScriptObject {

	protected boolean is_enable;
	protected String event_time;
	protected String event_type;
	protected String granularity;
	protected int update_column_count;
	protected int ref_row_count;
	protected String dml_stmt_type;
	protected String dmltable_schema;
	protected String dmltable_table;

    public AltibaseTrigger(OWNER container, String name, String description, JDBCResultSet dbResult) {
        super(container, name, description);

        this.is_enable 			= (JDBCUtils.safeGetInt(dbResult, "IS_ENABLE") == 1);
        this.event_time 		= JDBCUtils.safeGetStringTrimmed(dbResult, "EVENT_TIME");
        this.event_type 		= JDBCUtils.safeGetStringTrimmed(dbResult, "EVENT_TYPE");
        this.granularity 		= JDBCUtils.safeGetStringTrimmed(dbResult, "GRANULARITY");
        this.update_column_count= JDBCUtils.safeGetInt(dbResult, "UPDATE_COLUMN_CNT");
        this.ref_row_count 		= JDBCUtils.safeGetInt(dbResult, "REF_ROW_CNT");
        this.dml_stmt_type 		= JDBCUtils.safeGetString(dbResult, "DML_STMT_TYPE");
        this.dmltable_schema 	= JDBCUtils.safeGetString(dbResult, "DMLTABLE_SCHEMA");
        this.dmltable_table 	= JDBCUtils.safeGetString(dbResult, "DMLTABLE_NAME");
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
        return is_enable;
    }
    
    @Property(viewable = true, order = 6)
    public String getEventTime() {
        return event_time;
    }
    
    @Property(viewable = true, order = 7)
    public String getEventType() {
        return event_type;
    }
    
    @Property(viewable = true, order = 8)
    public int getUpdateColumnCount() {
        return update_column_count;
    }
    
    @Property(viewable = true, order = 9)
    public String getGranularity() {
        return granularity;
    }
    
    @Property(viewable = true, order = 10)
    public String getTargetTable() {
        return (dmltable_schema == null || dmltable_table == null)? "":dmltable_schema + "." + dmltable_table;
    }
    
    @Property(viewable = true, order = 11)
    public String getDmlType() {
        return dml_stmt_type;
    }
    
    @Override
    @Property(viewable = false, hidden = true, order = 100)
    public String getDescription()
    {
        return null;
    }
}
