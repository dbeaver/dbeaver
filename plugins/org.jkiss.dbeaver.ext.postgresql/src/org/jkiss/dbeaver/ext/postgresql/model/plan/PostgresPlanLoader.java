/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerDeSerialInfo;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.dbeaver.model.impl.plan.ExecutionPlanDeserializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PostgresPlanLoader extends AbstractExecutionPlan   {

    //private static final Log log = Log.getLog(PostgresPlanLoader.class);

    private String query;
    private List<PostgresPlanNodeExternal> rootNodes ; 

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public String getPlanQueryString() throws DBException {
        return "LOADED" + query;
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options) {
        return rootNodes;
    }

    @Override
    public Object getPlanFeature(String feature) {
        if (DBCPlanCostNode.FEATURE_PLAN_COST.equals(feature) ||
                DBCPlanCostNode.FEATURE_PLAN_DURATION.equals(feature) ||
                DBCPlanCostNode.FEATURE_PLAN_ROWS.equals(feature))
        {
            return true;
        }
        return super.getPlanFeature(feature);
    }
 

    public void deserialize(PostgreDataSource dataSource, Reader planData) throws InvocationTargetException{
        
        try {
            
            JsonObject jo = new JsonParser().parse(planData).getAsJsonObject();
            
            query = jo.get(AbstractExecutionPlanSerializer.PROP_SQL).getAsString();
            
            ExecutionPlanDeserializer<PostgresPlanNodeExternal> loader = new ExecutionPlanDeserializer<>();
            
            rootNodes = loader.loadRoot(dataSource, jo, new DBCQueryPlannerDeSerialInfo<PostgresPlanNodeExternal>() {
                
                @Override
                public PostgresPlanNodeExternal createNode(DBPDataSource datasource, JsonObject node,
                        PostgresPlanNodeExternal parent) {
                    return new PostgresPlanNodeExternal((PostgreDataSource) datasource, node, parent);
                }
             });
            
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
        
 
    }

}
