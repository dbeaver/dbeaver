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

package org.jkiss.dbeaver.ext.oceanbase.model.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class OceanbasePlanNodeJSON extends AbstractExecutionPlanNode implements DBCPlanCostNode, DBPPropertySource {

    private final OceanbasePlanNodeJSON parent;
    private final Map<String, String> nodeProps = new LinkedHashMap<>();
    private final List<OceanbasePlanNodeJSON> nested = new ArrayList<>();

    private String name;
    private JsonObject object;

    OceanbasePlanNodeJSON(OceanbasePlanNodeJSON parent, String name, JsonObject object) {
        this.parent = parent;
        this.name = name;
        this.object = object;

        parseObject(name, object);
    }

    OceanbasePlanNodeJSON(OceanbasePlanNodeJSON parent, Map<String, String> attributes) {
        this.parent = parent;
        this.nodeProps.putAll(attributes);
    }

    Map<String, String> getNodeProps() {
        return Collections.unmodifiableMap(nodeProps);
    }
    

    Object getProperty(String name) {
        return nodeProps.get(name);
    }

    private void parseObject(String objName, JsonObject object) {
        for (Map.Entry<String, JsonElement> prop : object.entrySet()) {
            String propName = prop.getKey();
            JsonElement value = prop.getValue();
            if (value instanceof JsonObject) {
                if ("cost_info".equals(propName)) {
                    parseObject(propName, (JsonObject) value);
                } else if ("query_block".equals(propName)) {
                    this.name = "query_block";
                    parseObject(propName, (JsonObject) value);
                } else if ("table".equals(propName) && "query_block".equals(objName)) {
                    this.name = "table";
                    parseObject(propName, (JsonObject) value);
                } else {
                    addNested(propName, (JsonObject) value);
                }
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
                nodeProps.put(propName, value.getAsString());
            }
        }
    }

    private void addNested(String name, JsonObject value) {
        nested.add(new OceanbasePlanNodeJSON(this, name, value));
    }

    @Property(order = 0, viewable = true)
    @Override
    public String getNodeType() {
        return name;
    }

    @Property(order = 1, viewable = true)
    @Override
    public String getNodeName() {
        Object nodeName = nodeProps.get("table_name");
        if (nodeName != null) {
            Object accessType = nodeProps.get("access_type");
            if (accessType != null) {
                return nodeName + " (" + accessType + ")";
            }
        }
        return nodeName == null ? null : String.valueOf(nodeName);
    }

    @Property(order = 10, viewable = true)
    @Override
    public Number getNodeCost() {
        Object readCost = nodeProps.get("COST");
        if (readCost == null) {
            if (nested != null) {
                long totalCost = 0;
                for (OceanbasePlanNodeJSON child : nested) {
                    Number childCost = child.getNodeCost();
                    if (childCost != null) {
                        totalCost += childCost.longValue();
                    }
                }
                return totalCost;
            }
            return null;
        }
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
        Object rowCount = nodeProps.get("EST.ROWS");
        if (rowCount == null && nested != null) {
            long totalRC = 0;
            for (OceanbasePlanNodeJSON child : nested) {
                Number childRC = child.getNodeRowCount();
                if (childRC != null) {
                    totalRC += childRC.longValue();
                }
            }
            return totalRC;
        }
        return rowCount == null ? null : CommonUtils.toLong(rowCount);
    }

    @Override
    public OceanbasePlanNodeJSON getParent() {
        return parent;
    }

    @Override
    public Collection<OceanbasePlanNodeJSON> getNested() {
        return nested;
    }

    @Override
    public String toString() {
        return object == null ? nodeProps.toString() : object.toString();
    }

    @Override
    public Object getEditableValue() {
        return this;
    }

    @Override
    public DBPPropertyDescriptor[] getProperties() {
        DBPPropertyDescriptor[] props = new DBPPropertyDescriptor[nodeProps.size()];
        int index = 0;
        for (Map.Entry<String, String> attr : nodeProps.entrySet()) {
            props[index++] = new PropertyDescriptor("Details", attr.getKey(), attr.getKey(), null, String.class, false,
                    null, null, false);
        }
        return props;
    }

    @Override
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, String id) {
        return nodeProps.get(id);
    }

    @Override
    public boolean isPropertySet(String id) {
        return false;// attributes.containsKey(id.toString());
    }

    @Override
    public boolean isPropertyResettable(String id) {
        return false;
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, String id) {
        // noting to do
    }

    @Override
    public void resetPropertyValueToDefault(String id) {
        // noting to do
    }

    @Override
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, String id, Object value) {
        // noting to do
    }

}
