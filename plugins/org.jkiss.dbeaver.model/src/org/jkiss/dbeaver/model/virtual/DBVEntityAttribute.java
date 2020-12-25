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
package org.jkiss.dbeaver.model.virtual;

import org.apache.commons.jexl3.JexlExpression;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt2;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Virtual attribute
 */
public class DBVEntityAttribute implements DBSEntityAttribute, DBPNamedObject2, DBPImageProvider, DBSTypedObjectExt2
{
    private final DBVEntity entity;
    private final DBVEntityAttribute parent;
    private final List<DBVEntityAttribute> children = new ArrayList<>();
    private String name;
    private String defaultValue;
    private String description;

    private boolean custom;
    private String expression;
    private DBPDataKind dataKind = DBPDataKind.UNKNOWN;
    private String typeName;
    private long maxLength = -1;
    private Integer precision;
    private Integer scale;

    private DBVTransformSettings transformSettings;
    private Map<String, Object> properties;
    private JexlExpression parsedExpression;

    public DBVEntityAttribute(DBVEntity entity, DBVEntityAttribute parent, String name) {
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

        this.custom = copy.custom;
        this.expression = copy.expression;
        this.dataKind = copy.dataKind;
        this.typeName = copy.typeName;

        this.transformSettings = copy.transformSettings == null ? null : new DBVTransformSettings(copy.transformSettings);
        if (!CommonUtils.isEmpty(copy.properties)) {
            this.properties = new LinkedHashMap<>(copy.properties);
        }
    }

    DBVEntityAttribute(DBVEntity entity, DBVEntityAttribute parent, String name, Map<String, Object> map) {
        this(entity, parent, name);
        this.custom = JSONUtils.getBoolean(map, "custom");
        if (this.custom) {
            this.expression = JSONUtils.getString(map, "expression");
            this.dataKind = CommonUtils.valueOf(DBPDataKind.class, JSONUtils.getString(map, "dataKind"), DBPDataKind.UNKNOWN);
            this.typeName = JSONUtils.getString(map, "typeName");
        }
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

    @Property(editable = true)
    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Property(editable = true)
    @Override
    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String getFullTypeName() {
        return getTypeName();
    }

    @Override
    public int getTypeID() {
        return -1;
    }

    @Property(editable = true)
    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return dataKind;
    }

    public void setDataKind(DBPDataKind dataKind) {
        this.dataKind = dataKind;
    }

    @Override
    public Integer getScale() {
        return this.scale;
    }

    @Override
    public void setScale(Integer scale) {
        this.scale = scale;
    }

    @Override
    public Integer getPrecision() {
        return this.precision;
    }

    @Override
    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    @Override
    public long getMaxLength() {
        return this.maxLength;
    }

    @Override
    public long getTypeModifiers() {
        return 0;
    }

    @Override
    public void setMaxLength(long maxLength) {
        this.maxLength = maxLength;
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
    public void setRequired(boolean required) {

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

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    @Property(editable = true)
    @Nullable
    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
        this.parsedExpression = null;
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
        if (!CommonUtils.isEmpty(defaultValue) || !CommonUtils.isEmpty(description) || !CommonUtils.isEmpty(expression)) {
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

    public JexlExpression getParsedExpression() {
        if (parsedExpression == null) {
            if (CommonUtils.isEmpty(expression)) {
                return null;
            }
            parsedExpression = DBVUtils.parseExpression(expression);
        }
        return parsedExpression;
    }

    @Override
    public String toString() {
        return name;
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        return DBValueFormatting.getTypeImage(this);
    }
}
