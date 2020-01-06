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
package org.jkiss.dbeaver.ext.ocient.model.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Ocient execution plan node based on JSON format
 */
public class OcientPlanNodeJson extends OcientPlanNode implements DBPPropertySource {

    private OcientPlanNodeJson parent;
    private String name;
    private JsonObject object;
    private Map<String, String> nodeProps = new LinkedHashMap<>();
    private List<OcientPlanNodeJson> nested = new ArrayList<>();

    public OcientPlanNodeJson(OcientPlanNodeJson parent, String name, JsonObject object) {
        this.parent = parent;
        this.name = name;
        this.object = object;

        parseObject(name, object);
    }

    public OcientPlanNodeJson(OcientPlanNodeJson parent,  Map<String, String> attributes) {
        this.parent = parent;
        this.nodeProps.putAll(attributes);
    }

    public Map<String, String> getNodeProps() {
        return nodeProps;
    }

    private void parseObject(String objName, JsonObject object) {
        for (Map.Entry<String, JsonElement> prop : object.entrySet()) {
            String propName = prop.getKey();
            JsonElement value = prop.getValue();
            if (value instanceof JsonObject) {
                    addNested(propName, (JsonObject) value);
            } else if (value instanceof JsonArray) {
                boolean isProp = false;
                int itemIndex = 0;
                for (JsonElement item : (JsonArray) value) {
                    if (item instanceof JsonObject) {
                        itemIndex++;
                        addNested(propName + "#" + itemIndex, (JsonObject) item);
                    } else {
                        isProp = true;
                        break;
                    }
                }
                if (isProp) {
                    nodeProps.put(propName, value.toString());
                }
            } else {
            	if ("type".equals(propName)) {
            		this.name = value.getAsString();
            	}
        		nodeProps.put(propName, value.getAsString());
            }
        }
    }

    private void addNested(String name, JsonObject value) {
        if (nested == null) {
            nested = new ArrayList<>();
        }
        nested.add(
            new OcientPlanNodeJson(this, name, value)
        );
    }

    @Property(order = 0, viewable = true)
    @Override
    public String getNodeType() {
        return name;
    }

    @Property(order = 1, viewable = true)
    @Override
    public String getNodeName() {
        return nodeProps.get("id");
    }

    @Property(order = 10, viewable = true)
    @Override
    public Number getNodeCost() {
        Object readCost = nodeProps.get("cost");
        return CommonUtils.toDouble(readCost);
    }

    @Override
    public Number getNodePercent() {
        return null;
    }

    @Override
    public Number getNodeDuration() {
        return null;
    }

    @Property(order = 11, viewable = true)
    @Override
    public Number getNodeRowCount() {
        Object rowCount = nodeProps.get("outputCardinality");
        return CommonUtils.toLong(rowCount);
    }

    @Override
    public OcientPlanNodeJson getParent() {
        return parent;
    }

    @Override
    public Collection<OcientPlanNodeJson> getNested() {
        return nested;
    }

    public Object getProperty(String name) {
        return nodeProps.get(name);
    }

    @Override
    public String toString() {
        return object == null ? nodeProps.toString() : object.toString();
    }

    //////////////////////////////////////////////////////////
    // Properties

    @Override
    public Object getEditableValue() {
        return this;
    }

    @Override
    public DBPPropertyDescriptor[] getPropertyDescriptors2() {
        DBPPropertyDescriptor[] props = new DBPPropertyDescriptor[nodeProps.size()];
        int index = 0;
        for (Map.Entry<String, String> attr : nodeProps.entrySet()) {
            props[index++] = new PropertyDescriptor(
                "Details",
                attr.getKey(),
                attr.getKey(),
                null,
                String.class,
                false,
                null,
                null,
                false);
        }
        return props;
    }

    @Override
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, Object id) {
        return nodeProps.get(id.toString());
    }

    @Override
    public boolean isPropertySet(Object id) {
        return false;//attributes.containsKey(id.toString());
    }

    @Override
    public boolean isPropertyResettable(Object id) {
        return false;
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, Object id) {

    }

    @Override
    public void resetPropertyValueToDefault(Object id) {

    }

    @Override
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, Object id, Object value) {

    }

    @Override
    public boolean isDirty(Object id) {
        return false;
    }

}
