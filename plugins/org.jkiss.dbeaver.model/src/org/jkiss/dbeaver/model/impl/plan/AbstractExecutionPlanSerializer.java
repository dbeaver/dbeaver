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

package org.jkiss.dbeaver.model.impl.plan;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;

import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerialInfo;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerializable;
import org.jkiss.utils.CommonUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public abstract class AbstractExecutionPlanSerializer  implements DBCQueryPlannerSerializable{

	private static final String PROP_DESC = "desc";
	private static final String PROP_COND = "cond";
	private static final String PROP_TYPE = "type";
	private static final String PROP_KIND = "kind";
	private static final String PROP_NAME = "name";
	
	private static final String PROP_DATE = "date";
	private static final String PROP_SIGNATURE = "signature";
	private static final String PROP_VERSION = "version";
	
	private static final String PROP_SQL = "sql";
	
	private static final String PROP_CHILD = "child";
	
	private static final String PROP_NODES = "root";
	
	public static final String PROP_ATTRIBUTES = "root";
	
	
	private JsonElement serializeNode(DBCPlanNode node,DBCQueryPlannerSerialInfo info) {
		
		JsonObject nodeJson = new JsonObject();
		
		nodeJson.add(PROP_NAME, new JsonPrimitive(CommonUtils.notEmpty(node.getNodeName())));
		nodeJson.add(PROP_KIND, node.getNodeKind() == null ? new JsonPrimitive("") : new JsonPrimitive(CommonUtils.notEmpty(node.getNodeKind().getTitle())));
		nodeJson.add(PROP_TYPE, new JsonPrimitive(CommonUtils.notEmpty(node.getNodeType())));
		nodeJson.add(PROP_COND, new JsonPrimitive(CommonUtils.notEmpty(node.getNodeCondition())));
		nodeJson.add(PROP_DESC, new JsonPrimitive(CommonUtils.notEmpty(node.getNodeDescription())));
		
		info.addNodeProperties(node,nodeJson);
		
		JsonArray nodes = new JsonArray();
		
		if (node.getNested() != null) {
			for(DBCPlanNode childNode : node.getNested()) {
				nodes.add(serializeNode(childNode,info));
			}			
		}
		

		nodeJson.add(PROP_CHILD, nodes);
		return nodeJson;
	}
	
	
	protected JsonElement serializeJson(DBCPlan plan,DBCQueryPlannerSerialInfo info) {
		
		JsonObject root = new JsonObject();
		
		root.add(PROP_VERSION, new JsonPrimitive(info.version()));
		root.add(PROP_SIGNATURE,  new JsonPrimitive(this.getClass().getName()));
		root.add(PROP_DATE, new JsonPrimitive(LocalDateTime.now().toString()));
		root.add(PROP_SQL, new JsonPrimitive(plan.getQueryString()));
		
		JsonArray nodes = new JsonArray();
		
		
		for(DBCPlanNode node : plan.getPlanNodes(null)) {
			nodes.add(serializeNode(node,info));
		}
		
		
		root.add(PROP_NODES, nodes);
		
		return root;
	}

	
}
