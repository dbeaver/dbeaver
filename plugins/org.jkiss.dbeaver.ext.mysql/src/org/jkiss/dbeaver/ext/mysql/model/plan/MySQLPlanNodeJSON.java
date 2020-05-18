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
package org.jkiss.dbeaver.ext.mysql.model.plan;

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
 * MySQL execution plan node based on JSON format
 */
public class MySQLPlanNodeJSON extends MySQLPlanNode implements DBPPropertySource {

    private MySQLPlanNodeJSON parent;
    private String name;
    private JsonObject object;
    private Map<String, String> nodeProps = new LinkedHashMap<>();
    private List<MySQLPlanNodeJSON> nested = new ArrayList<>();

    public MySQLPlanNodeJSON(MySQLPlanNodeJSON parent, String name, JsonObject object) {
        this.parent = parent;
        this.name = name;
        this.object = object;

        parseObject(name, object);
    }

    public MySQLPlanNodeJSON(MySQLPlanNodeJSON parent,  Map<String, String> attributes) {
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
        if (nested == null) {
            nested = new ArrayList<>();
        }
        nested.add(
            new MySQLPlanNodeJSON(this, name, value)
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
        Object nodeName = nodeProps.get("table_name");
        if (nodeName == null) {

        } else {
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
        Object readCost = nodeProps.get("read_cost");
        if (readCost == null) {
            readCost = nodeProps.get("query_cost");
        }
        if (readCost == null) {
            if (nested != null) {
                long totalCost = 0;
                for (MySQLPlanNodeJSON child : nested) {
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
        Object rowCount = nodeProps.get("rows_examined_per_scan");
        if (rowCount == null) {
            rowCount = nodeProps.get("rows"); // MariaDB-specific plan
            if (rowCount == null) {
                if (nested != null) {
                    long totalRC = 0;
                    for (MySQLPlanNodeJSON child : nested) {
                        Number childRC = child.getNodeRowCount();
                        if (childRC != null) {
                            totalRC += childRC.longValue();
                        }
                    }
                    return totalRC;
                }
            }
        }
        return rowCount == null ? null : CommonUtils.toLong(rowCount);
    }

    @Override
    public MySQLPlanNodeJSON getParent() {
        return parent;
    }

    @Override
    public Collection<MySQLPlanNodeJSON> getNested() {
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
