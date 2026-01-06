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

package org.jkiss.dbeaver.ext.gbase8s.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableTrigger;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

/**
 * @author Chao Tian
 */
public class GBase8sTableTrigger extends GenericTableTrigger {

    public enum TriggerEventType {
        D("DELETE"), I("INSERT"), U("UPDATE"), S("SELECT"), d("INSTEAD OF Delete"), i("INSTEAD OF Insert"),
        u("INSTEAD OF Update");

        private final String eventType;

        private TriggerEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getEventType() {
            return eventType;
        }
    }

    private TriggerEventType eventType;
    private final String collation;

    public GBase8sTableTrigger(@NotNull GenericTableBase container, String name, @NotNull JDBCResultSet resultSet) {
        super(container, name, null);
        String eventTypeLetter = JDBCUtils.safeGetString(resultSet, "event");
        if (CommonUtils.isNotEmpty(eventTypeLetter)) {
            this.eventType = CommonUtils.valueOf(TriggerEventType.class, eventTypeLetter);
        }
        this.collation = JDBCUtils.safeGetString(resultSet, "collation");
    }

    @Property(viewable = true, order = 5)
    public String getEventType() {
        return eventType.getEventType();
    }

    @Property(viewable = true, order = 6)
    public String getCollation() {
        return collation;
    }

    @Nullable
    @Override
    public String getDescription() {
        return super.getDescription();
    }
}
