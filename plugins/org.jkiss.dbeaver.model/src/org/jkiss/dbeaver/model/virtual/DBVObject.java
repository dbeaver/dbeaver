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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Virtual model object
 */
public abstract class DBVObject implements DBSObject {

    static final Log log = Log.getLog(DBVObject.class);

    private DBVTransformSettings transformSettings;
    private Map<String, Object> properties;

    @Override
    public abstract DBVContainer getParentObject();

    @Override
    public boolean isPersisted() {
        return true;
    }

    public DBVTransformSettings getTransformSettings() {
        return transformSettings;
    }

    public void setTransformSettings(DBVTransformSettings transformSettings) {
        this.transformSettings = transformSettings;
    }

    abstract public boolean hasValuableData();

    /**
     * Property value can be String, Number, Boolean, List or Map
     * @param name property name
     */
    @Nullable
    public <T> T getProperty(@NotNull String name) {
        return CommonUtils.isEmpty(properties) ? null : (T) properties.get(name);
    }

    public void setProperty(String name, @Nullable Object value) {
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }

    @NotNull
    public Map<String, Object> getProperties() {
        return properties == null ? Collections.emptyMap() : properties;
    }

    protected void copyFrom(@NotNull DBVObject src) {
        if (!CommonUtils.isEmpty(src.properties)) {
            this.properties = new LinkedHashMap<>(src.properties);
        }
    }

    protected void loadPropertiesFrom(@NotNull Map<String, Object> map, String elemName) {
        properties = JSONUtils.deserializeProperties(map, elemName);
    }


    public void persistConfiguration() {
        DBPDataSourceContainer dataSource = getDataSourceContainer();
        if (dataSource != null) {
            dataSource.persistConfiguration();
        }
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        DBVContainer parentObject = getParentObject();
        return parentObject == null ? null : parentObject.getDataSourceContainer();
    }

    public DBPProject getProject() {
        DBPDataSourceContainer ds = getDataSourceContainer();
        return ds == null ? null : ds.getProject();
    }
}
