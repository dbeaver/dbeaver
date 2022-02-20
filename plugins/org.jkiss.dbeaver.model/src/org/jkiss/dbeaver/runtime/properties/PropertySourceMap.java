/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Map-based property source
 */
public class PropertySourceMap implements DBPPropertySource {

    private List<DBPPropertyDescriptor> props = new ArrayList<>();

    private Map<?, ?> items;

    public PropertySourceMap(Map<String, ?> map)
    {
        items = new LinkedHashMap<>(map);
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            props.add(new ItemPropertyDescriptor(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public Object getEditableValue()
    {
        return this;
    }

    @Override
    public DBPPropertyDescriptor[] getProperties() {
        return props.toArray(new DBPPropertyDescriptor[props.size()]);
    }

    @Override
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, String id)
    {
        return items.get(id);
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
        return "<...>";
    }

    private static class ItemPropertyDescriptor implements DBPPropertyDescriptor {
        private final String name;
        private final Object value;

        ItemPropertyDescriptor(String name, Object value) {
            this.name = name;
            this.value = value;
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
        public PropertyLength getLength() {
            return PropertyLength.LONG;
        }

        @Nullable
        @Override
        public String[] getFeatures() {
            return null;
        }

        @Override
        public boolean hasFeature(@NotNull String feature) {
            return false;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return DBUtils.getObjectShortName(name);
        }

        @NotNull
        @Override
        public String getId() {
            return name;
        }
    }
}
