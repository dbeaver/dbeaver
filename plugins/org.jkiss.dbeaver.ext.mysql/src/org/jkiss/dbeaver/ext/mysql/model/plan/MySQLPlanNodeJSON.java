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
 *
 * Select type:
 *
 SIMPLE – the query is a simple SELECT query without any subqueries or UNIONs
 PRIMARY – the SELECT is in the outermost query in a JOIN
 DERIVED – the SELECT is part of a subquery within a FROM clause
 SUBQUERY – the first SELECT in a subquery
 DEPENDENT SUBQUERY – a subquery which is dependent upon on outer query
 UNCACHEABLE SUBQUERY – a subquery which is not cacheable (there are certain conditions for a query to be cacheable)
 UNION – the SELECT is the second or later statement of a UNION
 DEPENDENT UNION – the second or later SELECT of a UNION is dependent on an outer query
 UNION RESULT – the SELECT is a result of a UNION

 */
public class MySQLPlanNodeJSON extends MySQLPlanNode implements DBPPropertySource {

    private MySQLPlanNodeJSON parent;
    private String name;
    private JsonObject object;
    private Map<String, Object> nodeProps = new LinkedHashMap<>();
    private JsonObject costInfo;
    private List<MySQLPlanNodeJSON> nested;

    public MySQLPlanNodeJSON(MySQLPlanNodeJSON parent, String name, JsonObject object) {
        this.parent = parent;
        this.name = name;
        this.object = object;

        for (Map.Entry<String, JsonElement> prop : object.entrySet()) {
            String propName = prop.getKey();
            JsonElement value = prop.getValue();
            if (value instanceof JsonObject) {
                if ("cost_info".equals(propName)) {
                    costInfo = (JsonObject) value;
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

    @Override
    public String getNodeName() {
        Object nodeName = nodeProps.get("table_name");
        if (nodeName == null) {

        }
        return nodeName == null ? null : String.valueOf(nodeName);
    }

    @Property(order = 0, viewable = true)
    @Override
    public String getNodeType() {
        return name;
    }

    @Override
    public Number getNodeCost() {
        if (costInfo == null) {
            return null;
        }
        JsonElement readCost = costInfo.get("read_cost");
        if (readCost == null) {
            readCost = costInfo.get("query_cost");
        }
        if (readCost == null) {
            return null;
        }
        return readCost.getAsDouble();
    }

    @Override
    public Number getNodePercent() {
        return null;
    }

    @Override
    public Number getNodeDuration() {
        return null;
    }

    @Override
    public Number getNodeRowCount() {
        Object rowCount = nodeProps.get("rows_examined_per_scan");
        if (rowCount == null) {

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
        return object.toString();
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
        for (Map.Entry<String, Object> attr : nodeProps.entrySet()) {
            props[index++] = new PropertyDescriptor(
                "Source",
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
