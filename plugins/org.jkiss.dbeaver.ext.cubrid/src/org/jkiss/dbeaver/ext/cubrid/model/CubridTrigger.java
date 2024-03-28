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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableTrigger;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class CubridTrigger extends GenericTableTrigger
{
    private String owner;
    private String targetOwner;
    private Double priority;
    private Integer event;
    private Integer conditionTime;
    private String condition;
    private Integer actionTime;
    private Integer actionType;
    private String actionDefinition;

    public CubridTrigger(
            @NotNull GenericTableBase container,
            @NotNull String name,
            @Nullable String description,
            @NotNull JDBCResultSet dbResult) {
        super(container, name, description);
        this.owner = JDBCUtils.safeGetString(dbResult, "owner.name");
        this.targetOwner = JDBCUtils.safeGetString(dbResult, "target_owner_name");
        this.priority = JDBCUtils.safeGetDouble(dbResult, "priority");
        this.event = JDBCUtils.safeGetInteger(dbResult, "event");
        this.conditionTime = JDBCUtils.safeGetInteger(dbResult, "condition_time");
        this.condition = JDBCUtils.safeGetString(dbResult, "condition");
        this.actionTime = JDBCUtils.safeGetInteger(dbResult, "action_time");
        this.actionType = JDBCUtils.safeGetInteger(dbResult, "action_type");
        this.actionDefinition = JDBCUtils.safeGetString(dbResult, "action_definition");
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public String getOwner() {
        return owner;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public String getTargetOwner() {
        return targetOwner == null ? this.getTable().getSchema().getName() : targetOwner ;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 4)
    public CubridTable getTable() {
        return (CubridTable) super.getTable();
    }

    @NotNull
    @Property(viewable = true, order = 10)
    public Number getPriority() {
        return priority;
    }

    @Nullable
    @Property(viewable = true, order = 20)
    public String getEvent() {
        if (event != null) {
            switch (event) {
                case 0:
                    return "UPDATE";
                case 1:
                    return "UPDATE STATEMENT";
                case 2:
                    return "DELETE";
                case 3:
                    return "DELETE STATEMENT";
                case 4:
                    return "INSERT";
                case 5:
                    return "INSERT STATEMENT";
                case 8:
                    return "COMMIT";
                case 9:
                    return "ROLLBACK";
                default:
                    return "";
            }
        } else {
            return "";
        }
    }

    @Nullable
    @Property(viewable = true, order = 30)
    public String getConditionTime() {
        return getSpecificTime(conditionTime);
    }

    @NotNull
    @Property(viewable = true, order = 40)
    public String getCondition() {
        return condition;
    }

    @Nullable
    @Property(viewable = true, order = 50)
    public String getActionTime() {
        return getSpecificTime(actionTime);
    }

    @Nullable
    @Property(viewable = true, order = 60)
    public String getActionType() {
        if (actionType != null) {
            switch (actionType) {
                case 1:
                    return "INSERT, UPDATE, DELETE, CALL";
                case 2:
                    return "REJECT";
                case 3:
                    return "INVALIDATE_TRANSACTION";
                case 4:
                    return "PRINT";
                default:
                    return "";
            }
        } else {
            return "";
        }
    }

    @NotNull
    @Property(viewable = true, order = 70)
    public String getActionDefinition() {
        return actionDefinition;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return super.getFullyQualifiedName(context);
    }

    @Nullable
    private String getSpecificTime(@Nullable Integer time) {
        if (time != null) {
            switch (time) {
                case 1:
                    return "BEFORE";
                case 2:
                    return "AFTER";
                case 3:
                    return "DEFERRED";
                default:
                    return "";
            }
        } else {
            return "";
        }
    }
}