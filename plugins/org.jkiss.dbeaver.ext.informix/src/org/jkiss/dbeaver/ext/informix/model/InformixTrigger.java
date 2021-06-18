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
package org.jkiss.dbeaver.ext.informix.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableTrigger;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

public class InformixTrigger extends GenericTableTrigger {

    public enum TriggerEventType {
        D("DELETE"),
        I("INSERT"),
        U("UPDATE"),
        S("SELECT"),
        d("INSTEAD OF Delete"),
        i("INSTEAD OF Insert"),
        u("INSTEAD OF Update");

        private final String eventType;

        TriggerEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getEventType() {
            return eventType;
        }
    }

    private TriggerEventType eventType;
    private final String beforeValue;
    private final String afterValue;
    private final String collation;

    public InformixTrigger(@NotNull GenericTableBase container, String name, @NotNull JDBCResultSet resultSet) {
        super(container, name, null);
        String eventTypeLetter = JDBCUtils.safeGetString(resultSet, "event");
        if (CommonUtils.isNotEmpty(eventTypeLetter)) {
            this.eventType = CommonUtils.valueOf(TriggerEventType.class, eventTypeLetter);
        }
        this.beforeValue = JDBCUtils.safeGetString(resultSet, "old");
        this.afterValue = JDBCUtils.safeGetString(resultSet, "new");
        this.collation = JDBCUtils.safeGetString(resultSet, "collation");
    }

    @Property(viewable = true, order = 5)
    public String getEventType() {
        return eventType.getEventType();
    }

    @Property(viewable = true, order = 6)
    public String getBeforeValue() {
        return beforeValue;
    }

    @Property(viewable = true, order = 7)
    public String getAfterValue() {
        return afterValue;
    }

    @Property(viewable = true, order = 8)
    public String getCollation() {
        return collation;
    }

    // Hide property
    @Nullable
    @Override
    public String getDescription() {
        return super.getDescription();
    }
}
