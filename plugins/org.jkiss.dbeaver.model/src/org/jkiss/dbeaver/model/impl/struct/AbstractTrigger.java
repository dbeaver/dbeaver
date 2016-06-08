/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
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

    protected void setName(String tableName)
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
    @Property(viewable = true, order = 100)
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
