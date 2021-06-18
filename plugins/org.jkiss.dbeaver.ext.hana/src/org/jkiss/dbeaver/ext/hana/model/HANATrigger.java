/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableTrigger;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.util.Date;

public class HANATrigger extends GenericTableTrigger {

    private final String triggerActionTime;
    private final String triggerEvent;
    private final String triggeredActionLevel;
    private final Date createTime;
    private final String definition;
    private final boolean isValid;
    private final boolean isEnabled;

    public HANATrigger(@NotNull GenericTableBase container, String name, @NotNull JDBCResultSet resultSet) {
        super(container, name, null);
        triggerActionTime = JDBCUtils.safeGetString(resultSet, "TRIGGER_ACTION_TIME");
        triggerEvent = JDBCUtils.safeGetString(resultSet, "TRIGGER_EVENT");
        triggeredActionLevel = JDBCUtils.safeGetString(resultSet, "TRIGGERED_ACTION_LEVEL");
        createTime = JDBCUtils.safeGetDate(resultSet, "CREATE_TIME");
        definition = JDBCUtils.safeGetString(resultSet, "DEFINITION");
        isValid = JDBCUtils.safeGetBoolean(resultSet, "IS_VALID");
        isEnabled = JDBCUtils.safeGetBoolean(resultSet, "IS_ENABLED");
    }

    @Property(viewable = true, order = 5)
    public String getTriggerActionTime() {
        return triggerActionTime;
    }

    @Property(viewable = true, order = 6)
    public String getTriggerEvent() {
        return triggerEvent;
    }

    @Property(viewable = true, order = 7)
    public String getTriggeredActionLevel() {
        return triggeredActionLevel;
    }

    @Property(viewable = true, order = 8)
    public Date getCreateTime() {
        return createTime;
    }

    @Property(viewable = true, order = 9)
    public boolean isValid() {
        return isValid;
    }

    @Property(viewable = true, order = 10)
    public boolean isEnabled() {
        return isEnabled;
    }

    public String getDefinition() {
        return definition;
    }

    // Hide property
    @Nullable
    @Override
    public String getDescription() {
        return super.getDescription();
    }
}
