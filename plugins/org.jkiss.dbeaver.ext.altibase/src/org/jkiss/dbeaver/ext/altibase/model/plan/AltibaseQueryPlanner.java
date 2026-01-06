/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.*;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.dbeaver.model.impl.plan.ExecutionPlanDeserializer;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IntKeyMap;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Altibase execution plan node
 */
public class AltibaseQueryPlanner extends AbstractExecutionPlanSerializer implements DBCQueryPlanner {

    private static final String FORMAT_VERSION = "1";

    private final AltibaseDataSource dataSource;    

    public AltibaseQueryPlanner(AltibaseDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public AltibaseDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public DBCPlan planQueryExecution(
            @NotNull DBCSession session, 
            @NotNull String query, 
            @NotNull DBCQueryPlannerConfiguration configuration) throws DBException {
        AltibaseExecutionPlan plan = new AltibaseExecutionPlan(dataSource, (JDBCSession) session, query);
        plan.explain();
        return plan;
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }

    private JsonObject createAttr(String key, String value) {
        JsonObject attr = new JsonObject();
        attr.add(key, new JsonPrimitive(CommonUtils.notEmpty(value)));
        return attr; 
    }

    private JsonObject createAttr(String key, int value) {
        JsonObject attr = new JsonObject();
        attr.add(key, new JsonPrimitive(value));
        return attr; 
    }

    @Override
    public void serialize(Writer planData, DBCPlan plan) throws IOException {
        serializeJson(planData, plan, dataSource.getInfo().getDriverName(), new DBCQueryPlannerSerialInfo() {

            @Override
            public String version() {
                return FORMAT_VERSION;
            }

            @Override
            public void addNodeProperties(DBCPlanNode node, JsonObject nodeJson) {

                JsonArray attributes = new JsonArray();
                if (node instanceof AltibasePlanNode) {                 
                    AltibasePlanNode altiNode = (AltibasePlanNode) node;
                    attributes.add(createAttr("id", altiNode.getId()));
                    attributes.add(createAttr("parent_id", altiNode.getParentId()));
                    attributes.add(createAttr("depth", altiNode.getDepth()));
                    attributes.add(createAttr("plan", altiNode.getPlan()));
                }
                nodeJson.add(PROP_ATTRIBUTES, attributes);
            }
        });
    }

    @Override
    public DBCPlan deserialize(@NotNull Reader planData) throws IOException, InvocationTargetException {
        try {

            @SuppressWarnings("deprecation")
            JsonObject jo = new JsonParser().parse(planData).getAsJsonObject();

            String query = getQuery(jo);

            ExecutionPlanDeserializer<AltibasePlanNode> loader = new ExecutionPlanDeserializer<>();

            IntKeyMap<AltibasePlanNode> allNodes = new IntKeyMap<>();

            List<AltibasePlanNode> rootNodes = loader.loadRoot(dataSource, jo, (datasource, node, parent) -> {
                AltibasePlanNode altiNode = new AltibasePlanNode(dataSource, allNodes, getNodeAttributes(node));
                allNodes.put(altiNode.getId(), altiNode);
                return altiNode;
            });
            return new AltibaseExecutionPlan(query, rootNodes);

        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

}