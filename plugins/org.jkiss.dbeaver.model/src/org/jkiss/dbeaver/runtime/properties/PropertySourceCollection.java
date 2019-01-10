/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple property source which store properties in map
 */
public class PropertySourceCollection implements DBPPropertySource {

    private List<DBPPropertyDescriptor> props = new ArrayList<>();

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
