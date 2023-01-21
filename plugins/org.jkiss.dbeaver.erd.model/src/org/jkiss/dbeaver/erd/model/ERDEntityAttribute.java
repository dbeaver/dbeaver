/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Column entry in model Table
 *
 * @author Serge Rider
 */
public class ERDEntityAttribute extends ERDObject<DBSEntityAttribute> {
    private boolean isChecked;
    private int order = -1;
    private boolean inPrimaryKey;
    private boolean inForeignKey;
    private String alias;

    public ERDEntityAttribute(DBSEntityAttribute attribute, boolean inPrimaryKey) {
        super(attribute);
        this.inPrimaryKey = inPrimaryKey;
    }

    public String getLabelText() {
        return object.getName();
    }

    public DBPImage getLabelImage() {
        return DBValueFormatting.getObjectImage(object);
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isInPrimaryKey() {
        return inPrimaryKey;
    }

    public boolean isInForeignKey() {
        return inForeignKey;
    }

    public void setInForeignKey(boolean inForeignKey) {
        this.inForeignKey = inForeignKey;
    }

    @NotNull
    @Override
    public String getName() {
        return getObject().getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void fromMap(@NotNull ERDContext context, Map<String, Object> attrMap) {
        alias = JSONUtils.getString(attrMap, "alias");
        isChecked = JSONUtils.getBoolean(attrMap, "checked");
        inPrimaryKey = JSONUtils.getBoolean(attrMap, "inPrimaryKey");
        inForeignKey = JSONUtils.getBoolean(attrMap, "inForeignKey");
    }

    @Override
    public Map<String, Object> toMap(@NotNull ERDContext context, boolean fullInfo) {
        Map<String, Object> attrMap = new LinkedHashMap<>();
        attrMap.put("name", this.getName());
        if (!CommonUtils.isEmpty(this.getAlias())) {
            attrMap.put("alias", this.getAlias());
        }
        DBSEntityAttribute entityAttribute = this.getObject();
        if (entityAttribute != null && fullInfo) {
            attrMap.put("dataKind", entityAttribute.getDataKind().name());
            attrMap.put("typeName", entityAttribute.getTypeName());

            if (!entityAttribute.isRequired()) {
                attrMap.put("optional", true);
            }

            int iconIndex = context.getIconIndex(DBValueFormatting.getObjectImage(entityAttribute));

            attrMap.put("iconIndex", iconIndex);
            attrMap.put("fullTypeName", entityAttribute.getFullTypeName());
            if (!CommonUtils.isEmpty(entityAttribute.getDefaultValue())) {
                attrMap.put("defaultValue", entityAttribute.getDefaultValue());
            }
            if (!CommonUtils.isEmpty(entityAttribute.getDescription())) {
                attrMap.put("description", entityAttribute.getDescription());
            }
        }
        if (this.isChecked()) {
            attrMap.put("checked", true);
        }
        if (fullInfo) {
            if (this.isInPrimaryKey()) {
                attrMap.put("inPrimaryKey", true);
            }
            if (this.isInForeignKey()) {
                attrMap.put("inForeignKey", true);
            }
        }
        return attrMap;
    }
}