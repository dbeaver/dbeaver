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
package org.jkiss.dbeaver.ext.exasol.model.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerialInfo;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.dbeaver.model.impl.plan.ExecutionPlanDeserializer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExasolQueryPlanner extends AbstractExecutionPlanSerializer implements DBCQueryPlanner {

    private final ExasolDataSource dataSource;

    public ExasolQueryPlanner(ExasolDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query, @NotNull DBCQueryPlannerConfiguration configuration) throws DBException {
        ExasolPlanAnalyser plan = new ExasolPlanAnalyser(dataSource, query);
        plan.explain(session);
        return plan;
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }

    @Override
    public void serialize(@NotNull Writer planData, @NotNull DBCPlan plan) throws IOException {
        serializeJson(planData, plan, dataSource.getInfo().getDriverName(), new DBCQueryPlannerSerialInfo() {

            @Override
            public String version() {
                return "json";
            }

            @Override
            public void addNodeProperties(DBCPlanNode node, JsonObject nodeJson) {
                JsonArray attributes = new JsonArray();
                if (node instanceof ExasolPlanNode) {
                    ExasolPlanNode planNode = (ExasolPlanNode) node;
                    for(Map.Entry<String, Object> element : planNode.getAttributes().entrySet()) {
                        JsonObject attr = new JsonObject();
                        Object value = element.getValue();
                        if (value instanceof Double) {
                            attr.add(element.getKey(), new JsonPrimitive((Double) value));
                        } else {
                            attr.add(element.getKey(), new JsonPrimitive(value.toString()));
                        }
                        attributes.add(attr);
                    }

                }
                nodeJson.add(PROP_ATTRIBUTES, attributes);
            }
        });
    }

    private Map<String, Object> getNodeAttributes(JsonObject nodeObject) {
        Map<String, Object> attributes = new HashMap<>(44);

        JsonArray attrs = nodeObject.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_ATTRIBUTES);
        for (JsonElement attr : attrs) {
            for (Map.Entry<String, JsonElement> p : attr.getAsJsonObject().entrySet()) {
                attributes.put(p.getKey(), p.getValue());
            }
        }

        return attributes;
    }

    @Override
    public DBCPlan deserialize(@NotNull Reader planData) throws InvocationTargetException {
        try {

            JsonObject jo = new JsonParser().parse(planData).getAsJsonObject();

            String query = getQuery(jo);

            ExecutionPlanDeserializer<ExasolPlanNode> loader = new ExecutionPlanDeserializer<>();

            List<ExasolPlanNode> rootNodes = loader.loadRoot(dataSource, jo, (datasource, node, parent) -> new ExasolPlanNode(parent, getNodeAttributes(node)));
            return new ExasolPlanAnalyser(dataSource, query, rootNodes);

        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }
}
