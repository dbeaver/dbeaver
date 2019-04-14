/*
 * DBeaver - Universal Database Manager
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

package org.jkiss.dbeaver.ext.oracle.model.plan;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.utils.IntKeyMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OraclePlanLoader extends AbstractExecutionPlan{
	
	private static final Log log = Log.getLog(OraclePlanLoader.class);
	 
	 private String query;
	 private List<DBCPlanNode> rootNodes ; 
	 
	 IntKeyMap<OraclePlanNode> allNodes = new IntKeyMap<>();
	
	@Override
	public String getQueryString() {
		return query;
	}

	@Override
	public String getPlanQueryString() throws DBException {
		return "LOADED " + query;
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
   
    private Map<String,String> getNodeAttributes(JsonObject nodeObject){
    	Map<String,String> attributes = new HashMap<>(44);
    	
        JsonArray attrs =  nodeObject.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_ATTRIBUTES);
	
        for(JsonElement attr : attrs) {
			Object[] props =   attr.getAsJsonObject().entrySet().toArray();
			if (props.length > 0) {
				Entry<String, JsonElement> p = (Entry<String, JsonElement>) props[0];
				attributes.put(p.getKey(), p.getValue().getAsString());
			}
			
		}
    	
    	return attributes;
    }
	
	private OraclePlanNode loadNode(OracleDataSource dataSource,JsonObject nodeObject,OraclePlanNode parent) {
		OraclePlanNode node = new OraclePlanNode(dataSource, allNodes, getNodeAttributes(nodeObject));
		allNodes.put(node.getId(), node);
		JsonArray childs = nodeObject.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_CHILD);
		if (childs != null) {
			childs.forEach((e) -> {
				if (node.nested == null) {
					node.nested = new ArrayList<>(2);
				}
				node.nested.add(loadNode(dataSource,e.getAsJsonObject(),node));
			});				
		}

		return node;
	}
	
	public void deserialize(OracleDataSource dataSource, Reader planData) {
		JsonObject jo = new JsonParser().parse(planData).getAsJsonObject();
		rootNodes = new ArrayList<>(1);
		query = jo.get(AbstractExecutionPlanSerializer.PROP_SQL).getAsString();
		jo.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_NODES).forEach((e) -> {
			rootNodes.add(loadNode(dataSource,e.getAsJsonObject(),null));
			});
		log.info(String.format("Loaded %d nodes of saved oracle plan", allNodes.size())); 
	}
}
