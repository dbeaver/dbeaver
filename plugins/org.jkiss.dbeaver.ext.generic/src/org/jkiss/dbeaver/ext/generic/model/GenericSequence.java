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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

/**
 * GenericSequence
 */
public class GenericSequence implements DBSSequence, DBPQualifiedObject
{
    private GenericStructContainer container;
    private String name;
    private String description;
    private Number lastValue;
    private Number minValue;
    private Number maxValue;
    private Number incrementBy;

    public GenericSequence(GenericStructContainer container, String name, String description, Number lastValue, Number minValue, Number maxValue, Number incrementBy) {
        this.container = container;
        this.name = name;
        this.description = description;
        this.lastValue = lastValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.incrementBy = incrementBy;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 10)
    public String getDescription() {
        return description;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return container;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return container.getDataSource();
    }

    @NotNull
    @Override
    public String getFullQualifiedName() {
        return DBUtils.getFullQualifiedName(getDataSource(),
            container.getCatalog(),
            container.getSchema(),
            this);
    }

    @Override
    @Property(viewable = true, order = 2)
    public Number getLastValue() {
        return lastValue;
    }

    public void setLastValue(Number lastValue) {
        this.lastValue = lastValue;
    }

    @Override
    @Property(viewable = true, order = 3)
    public Number getMinValue() {
        return minValue;
    }

    @Override
    @Property(viewable = true, order = 4)
    public Number getMaxValue() {
        return maxValue;
    }

    @Override
    @Property(viewable = true, order = 5)
    public Number getIncrementBy() {
        return incrementBy;
    }

}
