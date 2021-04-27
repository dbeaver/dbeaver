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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

/**
 * AbstractTrigger
 */
public abstract class AbstractTrigger implements DBSTrigger, DBPQualifiedObject, DBPSaveableObject
{
    protected String name;
    private DBSActionTiming actionTiming;
    private DBSManipulationType manipulationType;
    private String description;
    private boolean persisted;

    protected AbstractTrigger(boolean persisted)
    {
        this.persisted = persisted;
    }

    protected AbstractTrigger(String name, String description, boolean persisted)
    {
        this.name = name;
        this.description = description;
        this.persisted = persisted;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String tableName)
    {
        this.name = tableName;
    }

    @Property(viewable = true, editable = true, order = 2, listProvider = TriggerTimingListProvider.class)
    public DBSActionTiming getActionTiming()
    {
        return actionTiming;
    }

    public void setActionTiming(DBSActionTiming actionTiming)
    {
        this.actionTiming = actionTiming;
    }

    @Property(viewable = true, editable = true, order = 3, listProvider = TriggerTypeListProvider.class)
    public DBSManipulationType getManipulationType()
    {
        return manipulationType;
    }

    public void setManipulationType(DBSManipulationType manipulationType)
    {
        this.manipulationType = manipulationType;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription()
    {
        return description;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    public static class TriggerTimingListProvider implements IPropertyValueListProvider {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(Object object) {
            return new Object[] {
                DBSActionTiming.BEFORE,
                DBSActionTiming.AFTER
            };
        }
    }

    public static class TriggerTypeListProvider implements IPropertyValueListProvider {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(Object object) {
            return new Object[] {
                DBSManipulationType.INSERT,
                DBSManipulationType.UPDATE,
                DBSManipulationType.DELETE
            };
        }
    }

}
