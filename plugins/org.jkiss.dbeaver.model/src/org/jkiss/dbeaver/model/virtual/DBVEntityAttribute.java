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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Virtual attribute
 */
public class DBVEntityAttribute implements DBSEntityAttribute
{
    private final DBVEntity entity;
    private final DBVEntityAttribute parent;
    private final List<DBVEntityAttribute> children = new ArrayList<>();
    private String name;
    private String defaultValue;
    private String description;
    private DBVTransformSettings transformSettings;
    private Map<String, Object> properties;

    DBVEntityAttribute(DBVEntity entity, DBVEntityAttribute parent, String name) {
        this.entity = entity;
        this.parent = parent;
        this.name = name;
    }

    DBVEntityAttribute(DBVEntity entity, DBVEntityAttribute parent, DBVEntityAttribute copy) {
        this.entity = entity;
        this.parent = parent;
        this.name = copy.name;
        for (DBVEntityAttribute child : copy.children) {
            this.children.add(new DBVEntityAttribute(entity, this, child));
        }
        this.defaultValue = copy.defaultValue;
        this.description = copy.description;
        this.transformSettings = copy.transformSettings == null ? null : new DBVTransformSettings(copy.transformSettings);
        if (!CommonUtils.isEmpty(copy.properties)) {
            this.properties = new LinkedHashMap<>(copy.properties);
        }
    }

    DBVEntityAttribute(DBVEntity entity, DBVEntityAttribute parent, String name, Map<String, Object> map) {
        this(entity, parent, name);
        this.properties = JSONUtils.deserializeProperties(map, "properties");

        Map<String, Object> transformsCfg = JSONUtils.getObject(map, "transforms");
        if (!transformsCfg.isEmpty()) {
            transformSettings = new DBVTransformSettings();
            transformSettings.setCustomTransformer(JSONUtils.getString(transformsCfg, "custom"));
            for (String incTrans : JSONUtils.deserializeStringList(transformsCfg, "include")) {
                final DBDAttributeTransformerDescriptor transformer = DBWorkbench.getPlatform().getValueHandlerRegistry().getTransformer(incTrans);
                if (transformer != null) {
                    transformSettings.enableTransformer(transformer, true);
                }
            }
            for (String excTrans : JSONUtils.deserializeStringList(transformsCfg, "exclude")) {
                final DBDAttributeTransformerDescriptor transformer = DBWorkbench.getPlatform().getValueHandlerRegistry().getTransformer(excTrans);
                if (transformer != null) {
                    transformSettings.enableTransformer(transformer, false);
                }
            }
            transformSettings.setTransformOptions(JSONUtils.deserializeProperties(transformsCfg, "properties"));
        }
        properties = JSONUtils.deserializeProperties(transformsCfg, "properties");
    }

    @NotNull
    @Override
    public DBVEntity getParentObject() {
        return entity;
    }

    @NotNull
    public DBVEntity getEntity() {
        return entity;
    }

    @Nullable
    public DBVEntityAttribute getParent() {
        return parent;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return entity.getDataSource();
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTypeName() {
        return "void";
    }

    @Override
    public String getFullTypeName() {
        return getTypeName();
    }

    @Override
    public int getTypeID() {
        return -1;
    }

    @Override
    public DBPDataKind getDataKind() {
        return DBPDataKind.UNKNOWN;
    }

    @Override
    public Integer getScale() {
        return -1;
    }

    @Override
    public Integer getPrecision() {
        return -1;
    }

    @Override
    public long getMaxLength() {
        return -1;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public int getOrdinalPosition() {
        return 0;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean isAutoGenerated() {
        return false;
    }

    @Nullable
    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DBVEntityAttribute> getChildren() {
        return children;
    }

    public DBVEntityAttribute getChild(String name) {
        return DBUtils.findObject(children, name);
    }

    public void addChild(DBVEntityAttribute child) {
        this.children.add(child);
    }

    public DBVTransformSettings getTransformSettings() {
        return transformSettings;
    }

    void setTransformSettings(DBVTransformSettings transformSettings) {
        this.transformSettings = transformSettings;
    }

    @NotNull
    public Map<String, Object> getProperties() {
        return properties == null ? Collections.emptyMap() : properties;
    }

    @Nullable
    public Object getProperty(String name)
    {
        return CommonUtils.isEmpty(properties) ? null : properties.get(name);
    }

    public void setProperty(String name, @Nullable Object value)
    {
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }

    public boolean hasValuableData() {
        if (!CommonUtils.isEmpty(defaultValue) || !CommonUtils.isEmpty(description)) {
            return true;
        }
        if (!children.isEmpty()) {
            for (DBVEntityAttribute child : children) {
                if (child.hasValuableData()) {
                    return true;
                }
            }
        }
        return transformSettings != null && transformSettings.hasValuableData() || !CommonUtils.isEmpty(properties);
    }
}
