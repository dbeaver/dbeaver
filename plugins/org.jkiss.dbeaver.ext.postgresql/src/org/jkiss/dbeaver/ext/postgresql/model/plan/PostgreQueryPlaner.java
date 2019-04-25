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
package org.jkiss.dbeaver.ext.postgresql.model.plan;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.plan.*;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * PostgreQueryPlaner
 */
public class PostgreQueryPlaner extends AbstractExecutionPlanSerializer implements DBCQueryPlanner 
{
    private final PostgreDataSource dataSource;

    public final static String FORMAT_VERSION = "1";

    public PostgreQueryPlaner(PostgreDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public PostgreDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query) throws DBCException {
        PostgrePlanAnalyser plan = new PostgrePlanAnalyser(
                getPlanStyle() == DBCPlanStyle.QUERY,
                dataSource.getServerType().supportsExplainPlanVerbose(),
                query);
        plan.explain(session);
        return plan;
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return dataSource.getServerType().supportsExplainPlanXML() ? DBCPlanStyle.PLAN : DBCPlanStyle.QUERY;
    }

    @Override
    public void serialize(Writer writer, DBCPlan plan) throws IOException {
        serializeJson(writer, plan, dataSource.getInfo().getDriverName(), new DBCQueryPlannerSerialInfo() {

            @Override
            public String version() {
                return FORMAT_VERSION;
            }

            @Override
            public void addNodeProperties(DBCPlanNode node, JsonObject nodeJson) {

                JsonArray attributes = new JsonArray();
                if (node instanceof PostgrePlanNodeBase) {
                    PostgrePlanNodeBase<?> pgNode = (PostgrePlanNodeBase<?>) node;
                    for(Map.Entry<String, String>  e : pgNode.attributes.entrySet()) {
                        JsonObject attr = new JsonObject();
                        attr.add(e.getKey(), new JsonPrimitive(CommonUtils.notEmpty(e.getValue())));
                        attributes.add(attr);
                    }
                }
                nodeJson.add(PROP_ATTRIBUTES, attributes);

            }
        });
    }

    @Override
    public DBCPlan deserialize(Reader planData) throws IOException, InvocationTargetException {
        PostgresPlanLoader plan = new PostgresPlanLoader();
        plan.deserialize(dataSource, planData);
        return plan;
    }


}
