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
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple property source which store properties in map
 */
public class PropertySourceCollection implements DBPPropertySource {

    private List<DBPPropertyDescriptor> props = new ArrayList<DBPPropertyDescriptor>();

    private List<Object> items;

    public PropertySourceCollection(Collection<?> collection)
    {
        items = new ArrayList<Object>(collection);
        for (int i = 0; i < items.size(); i++) {
            //props.addAll(ObjectPropertyDescriptor.extractAnnotations(this, item.getClass(), null));
            props.add(new ItemPropertyDescriptor(i, items.get(i)));
        }
    }

    @Override
    public Object getEditableValue()
    {
        return this;
    }

    @Override
    public DBPPropertyDescriptor[] getPropertyDescriptors2() {
        return props.toArray(new DBPPropertyDescriptor[props.size()]);
    }

    @Override
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, Object id)
    {
        return items.get((Integer) id);
    }

    @Override
    public boolean isPropertySet(Object id)
    {
        return false;
    }

    @Override
    public boolean isPropertyResettable(Object id) {
        return false;
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, Object id)
    {

    }

    @Override
    public void resetPropertyValueToDefault(Object id) {

    }

    @Override
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, Object id, Object value)
    {
    }

    @Override
    public boolean isDirty(Object id) {
        return false;
    }

    private class ItemPropertyDescriptor implements DBPPropertyDescriptor {
        private Integer id;
        private Object item;

        private ItemPropertyDescriptor(Integer id, Object item) {
            this.id = id;
            this.item = item;
        }

        @Override
        public String getCategory() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public Class<?> getDataType() {
            return Object.class;
        }

        @Override
        public boolean isRequired() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public Object getDefaultValue() {
            return null;
        }

        @Override
        public boolean isEditable(Object object) {
            return false;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return DBUtils.getObjectShortName(item);
        }

        @NotNull
        @Override
        public Object getId() {
            return id;
        }
    }
}
