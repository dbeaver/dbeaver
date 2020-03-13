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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.*;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.dbeaver.model.impl.plan.ExecutionPlanDeserializer;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL execution plan analyser
 */
public class MySQLPlanAnalyser extends AbstractExecutionPlanSerializer implements DBCQueryPlanner {

    private MySQLDataSource dataSource;

    public MySQLPlanAnalyser(MySQLDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public MySQLPlanAbstract explain(JDBCSession session, String query) throws DBCException {
        if (supportsExplainJSON()) {
            return new MySQLPlanJSON(session, query);
        } else {
            return new MySQLPlanClassic(session, query);
        }
    }

    private boolean supportsExplainJSON() {
        if (dataSource.isMariaDB()) {
            return dataSource.isServerVersionAtLeast(10, 1);
        } else {
            return dataSource.isServerVersionAtLeast(5, 6);
        }
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query) throws DBCException {
        return explain((JDBCSession) session, query);
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }

    @Override
    public void serialize(@NotNull Writer writer, @NotNull DBCPlan plan) throws IOException, InvocationTargetException {

        serializeJson(writer, plan, dataSource.getInfo().getDriverName(), new DBCQueryPlannerSerialInfo() {

            @Override
            public String version() {
                return plan instanceof MySQLPlanClassic ? "classic" : "json";
            }

            @Override
            public void addNodeProperties(DBCPlanNode node, JsonObject nodeJson) {

                JsonObject attributes = new JsonObject();
                if (node instanceof MySQLPlanNodePlain) {
                    MySQLPlanNodePlain plainNode = (MySQLPlanNodePlain) node;
                    attributes.add("id", new JsonPrimitive(plainNode.getId()));
                    attributes.add("select_type", new JsonPrimitive(plainNode.getSelectType()));
                    attributes.add("table", new JsonPrimitive(plainNode.getTable()));
                    attributes.add("type", new JsonPrimitive(plainNode.getNodeType()));
                    attributes.add("possible_keys", new JsonPrimitive(plainNode.getPossibleKeys()));
                    attributes.add("key", new JsonPrimitive(plainNode.getKey()));
                    attributes.add("key_len", new JsonPrimitive(plainNode.getKeyLength()));
                    attributes.add("ref", new JsonPrimitive(plainNode.getRef()));
                    attributes.add("rows", new JsonPrimitive(plainNode.getRowCount()));
                    attributes.add("filtered", new JsonPrimitive(plainNode.getFiltered()));
                    attributes.add("extra", new JsonPrimitive(plainNode.getExtra()));
                } else if (node instanceof MySQLPlanNodeJSON) {
                    MySQLPlanNodeJSON jsNode = (MySQLPlanNodeJSON) node;
                    for(Map.Entry<String, String>  e : jsNode.getNodeProps().entrySet()) {
                        attributes.add(e.getKey(), new JsonPrimitive(CommonUtils.notEmpty(e.getValue())));
                    }
                }
                nodeJson.add(PROP_ATTRIBUTES, attributes);
            }
        });

/*
        if (plan instanceof MySQLPlanClassic) {
            serializeJson(planData, plan,dataSource.getInfo().getDriverName(),(MySQLPlanClassic) plan);
        } else if (plan instanceof MySQLPlanJSON) {
            serializeJson(planData, plan,dataSource.getInfo().getDriverName(),(MySQLPlanJSON) plan);
        }
*/

    }

    private static Map<String, String> getNodeAttributes(JsonObject nodeObject){
        Map<String,String> attributes = new HashMap<>();

        JsonObject attrs =  nodeObject.getAsJsonObject(PROP_ATTRIBUTES);
        for(Map.Entry<String, JsonElement> attr : attrs.entrySet()) {
            attributes.put(attr.getKey(), attr.getValue().getAsString());
        }

        return attributes;
    }

    @Override
    public DBCPlan deserialize(@NotNull Reader planData) throws IOException, InvocationTargetException {

        JsonObject jo = new JsonParser().parse(planData).getAsJsonObject();
 
        String savedVersion = getVersion(jo);
        
        String query = getQuery(jo);

        if (savedVersion.equals("classic")) {
            ExecutionPlanDeserializer<MySQLPlanNodePlain> loader = new ExecutionPlanDeserializer<>();
            List<MySQLPlanNodePlain> rootNodes = loader.loadRoot(dataSource, jo,
                (datasource, node, parent) -> new MySQLPlanNodePlain(parent, getNodeAttributes(node)));
            return new MySQLPlanClassic(dataSource, query, rootNodes);
        } else {
            ExecutionPlanDeserializer<MySQLPlanNodeJSON> loader = new ExecutionPlanDeserializer<>();
            List<MySQLPlanNodeJSON> rootNodes = loader.loadRoot(dataSource, jo,
                (datasource, node, parent) -> new MySQLPlanNodeJSON(parent, getNodeAttributes(node)));
            return new MySQLPlanJSON(dataSource,query,rootNodes);
        }

    }

}
