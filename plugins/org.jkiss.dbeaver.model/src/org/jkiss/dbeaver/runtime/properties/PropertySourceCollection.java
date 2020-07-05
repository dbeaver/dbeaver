/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.utils.CommonUtils;

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
        items = new ArrayList<>(collection);
        for (int i = 0; i < items.size(); i++) {
            //props.addAll(ObjectPropertyDescriptor.extractAnnotations(this, item.getClass(), null));
            props.add(new ItemPropertyDescriptor(String.valueOf(i), items.get(i)));
        }
    }

    @Override
    public Object getEditableValue()
    {
        return this;
    }

    @Override
    public DBPPropertyDescriptor[] getProperties() {
        return props.toArray(new DBPPropertyDescriptor[0]);
    }

    @Override
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, String id)
    {
        return items.get(CommonUtils.toInt(id));
    }

    @Override
    public boolean isPropertySet(String id)
    {
        return false;
    }

    @Override
    public boolean isPropertyResettable(String id) {
        return false;
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, String id)
    {

    }

    @Override
    public void resetPropertyValueToDefault(String id) {

    }

    @Override
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, String id, Object value)
    {
    }

    @Override
    public String toString() {
        return "[...]";
    }

    private class ItemPropertyDescriptor implements DBPPropertyDescriptor {
        private String id;
        private Object item;

        private ItemPropertyDescriptor(String  id, Object item) {
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
        public String getId() {
            return id;
        }
    }
}
