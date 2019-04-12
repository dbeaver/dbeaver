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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerialInfo;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.utils.CommonUtils;

/**
 * PostgreQueryPlaner
 */
public class PostgreQueryPlaner extends AbstractExecutionPlanSerializer implements DBCQueryPlanner 
{
    private final PostgreDataSource dataSource;
    
	private final static String FORMAT_VERSION = "1";

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
	public void serialize(Writer planData, DBCPlan plan) throws IOException {
		JsonElement e = serializeJson(plan, new DBCQueryPlannerSerialInfo() {
			
			@Override
			public String version() {
				return FORMAT_VERSION;
			}
			
			@Override
			public void addNodeProperties(DBCPlanNode node, JsonObject nodeJson) {
				
				JsonArray attributes = new JsonArray();
				if (node instanceof PostgrePlanNodeBase) {
				   PostgrePlanNodeBase<?> pgNode = (PostgrePlanNodeBase<?>) node;
				   for(Object attrVal : pgNode.attributes.entrySet()) {
					   Map.Entry<String, String>  e = (Map.Entry<String, String>) attrVal;
					   JsonObject attr = new JsonObject();
					   attr.add((String) e.getKey(), new JsonPrimitive(CommonUtils.notEmpty((String) e.getValue())));
					   attributes.add(attr);
				   }
				}
				nodeJson.add(PROP_ATTRIBUTES, attributes);
				
			}
		});
		
		planData.write(e.toString());
		
	}


}
