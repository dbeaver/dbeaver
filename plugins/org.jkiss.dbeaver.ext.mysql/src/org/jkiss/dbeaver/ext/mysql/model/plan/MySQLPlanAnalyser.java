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


import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.dbeaver.model.impl.plan.ExecutionPlanDeserializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * MySQL execution plan analyser
 */
public class MySQLPlanAnalyser extends AbstractExecutionPlanSerializer implements DBCQueryPlanner   {

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
    public void serialize(Writer planData, DBCPlan plan) throws IOException, InvocationTargetException {
        if (plan instanceof MySQLPlanClassic) {
            serializeJson(planData, plan,dataSource.getInfo().getDriverName(),(MySQLPlanClassic) plan);
        } else if (plan instanceof MySQLPlanJSON) {
            serializeJson(planData, plan,dataSource.getInfo().getDriverName(),(MySQLPlanJSON) plan);
        }
        
    }
    
    private Map<String,String> getNodeAttributes(JsonObject nodeObject){
        Map<String,String> attributes = new HashMap<>();

        JsonArray attrs =  nodeObject.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_ATTRIBUTES);
        for(JsonElement attr : attrs) {
            for (Map.Entry<String, JsonElement> p : attr.getAsJsonObject().entrySet()) {
                attributes.put(p.getKey(), p.getValue().getAsString());
            }
        }

        return attributes;
    }

    @Override
    public DBCPlan deserialize(Reader planData) throws IOException, InvocationTargetException {
        
        JsonObject jo = new JsonParser().parse(planData).getAsJsonObject();
        String saved_version = jo.get(AbstractExecutionPlanSerializer.PROP_VERSION).getAsString();
        String query = jo.get(AbstractExecutionPlanSerializer.PROP_SQL).getAsString();
        
        
        if (saved_version.equals(MySQLPlanClassic.FORMAT_VERSION)) {
            ExecutionPlanDeserializer<MySQLPlanNodePlain> loader = new ExecutionPlanDeserializer<>();
            List<MySQLPlanNodePlain> rootNodes = loader.loadRoot(dataSource, jo, (datasource, node, parent) -> {
                return new MySQLPlanNodePlain(parent, getNodeAttributes(node));
            });
            return new MySQLPlanClassic(dataSource,query,rootNodes);
        } else if (saved_version.equals(MySQLPlanJSON.FORMAT_VERSION)) {
            ExecutionPlanDeserializer<MySQLPlanNodeJSON> loader = new ExecutionPlanDeserializer<>();
            List<MySQLPlanNodeJSON> rootNodes = loader.loadRoot(dataSource, jo, (datasource, node, parent) -> {
                return new MySQLPlanNodeJSON(parent,node);
            });
            return new MySQLPlanJSON(dataSource,query,rootNodes);
        }
        throw new InvocationTargetException(new Exception("Unsuported version"));
    }
    

}
