/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.teradata.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableTrigger;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.utils.CommonUtils;

import java.util.Date;
import java.util.Map;

public class TeradataTrigger extends GenericTableTrigger implements DBPQualifiedObject {

    private DBSActionTiming actionTime;
    private DBSManipulationType eventType;
    private String enabledStatus;
    private String triggerType;
    private Date createDate;
    private String description;

    private String definition;

    public TeradataTrigger(GenericTableBase table, String name, String description, JDBCResultSet dbResult) {
        super(table, name, description);

        String actTime = JDBCUtils.safeGetString(dbResult, "ActionTime");
        if (!CommonUtils.isEmpty(actTime)) {
            switch (actTime) {
                case "A":
                    actionTime = DBSActionTiming.AFTER;
                    break;
                case "B":
                    actionTime = DBSActionTiming.BEFORE;
                    break;
                default:
                    actionTime = DBSActionTiming.UNKNOWN;
                    break;
            }
        }

        String event = JDBCUtils.safeGetString(dbResult, "Event");
        if (!CommonUtils.isEmpty(event)) {
            switch (event) {
                case "U":
                    eventType = DBSManipulationType.UPDATE;
                    break;
                case "I":
                    eventType = DBSManipulationType.INSERT;
                    break;
                case "D":
                    eventType = DBSManipulationType.DELETE;
                    break;
                default:
                    eventType = DBSManipulationType.UNKNOWN;
                    break;
            }
        }

        this.enabledStatus = JDBCUtils.safeGetString(dbResult, "status");
        this.triggerType = JDBCUtils.safeGetString(dbResult, "triggerKind");
        this.createDate = JDBCUtils.safeGetTimestamp(dbResult, "createDate");

        this.definition = JDBCUtils.safeGetString(dbResult, "definition");
        this.description = description;
    }

    @Property(viewable = true, order = 3)
    public DBSActionTiming getActionTime() {
        return actionTime;
    }

    @Property(viewable = true, order = 4)
    public DBSManipulationType getEventType() {
        return eventType;
    }

    @Property(viewable = true, order = 5)
    public String getEnabledStatus() {
        return enabledStatus;
    }

    @Property(viewable = true, order = 6)
    public String getTriggerType() {
        return triggerType;
    }

    @Property(order = 7)
    public Date getCreateDate() {
        return createDate;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) {
        return definition;
    }

    public GenericSchema getSchema() {
        return getTable().getSchema();
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getSchema(),
            this);
    }
}
