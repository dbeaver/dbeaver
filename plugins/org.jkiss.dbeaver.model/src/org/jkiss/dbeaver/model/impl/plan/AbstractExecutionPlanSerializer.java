/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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


import com.google.gson.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerialInfo;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerializable;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

public abstract class AbstractExecutionPlanSerializer  implements DBCQueryPlannerSerializable{

    public static final String PROP_DESC = "desc";
    public static final String PROP_COND = "cond";
    public static final String PROP_TYPE = "type";
    public static final String PROP_KIND = "kind";
    public static final String PROP_NAME = "name";

    public static final String PROP_DATE = "date";
    public static final String PROP_SIGNATURE = "signature";
    public static final String PROP_VERSION = "version";

    public static final String PROP_SQL = "sql";

    public static final String PROP_CHILD = "child";

    public static final String PROP_NODES = "root";

    public static final String PROP_ATTRIBUTES = "attributes";

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private JsonElement serializeNode(DBCPlanNode node,DBCQueryPlannerSerialInfo info) {

        JsonObject nodeJson = new JsonObject();

        nodeJson.add(PROP_NAME, new JsonPrimitive(CommonUtils.notEmpty(node.getNodeName())));
        nodeJson.add(PROP_KIND, node.getNodeKind() == null ? new JsonPrimitive("") : new JsonPrimitive(CommonUtils.notEmpty(node.getNodeKind().getTitle())));
        nodeJson.add(PROP_TYPE, new JsonPrimitive(CommonUtils.notEmpty(node.getNodeType())));
        nodeJson.add(PROP_COND, new JsonPrimitive(CommonUtils.notEmpty(node.getNodeCondition())));
        nodeJson.add(PROP_DESC, new JsonPrimitive(CommonUtils.notEmpty(node.getNodeDescription())));

        info.addNodeProperties(node,nodeJson);

        if (!CommonUtils.isEmpty(node.getNested())) {
            JsonArray nodes = new JsonArray();
            for(DBCPlanNode childNode : node.getNested()) {
                nodes.add(serializeNode(childNode,info));
            }
            nodeJson.add(PROP_CHILD, nodes);
        }


        return nodeJson;
    }


    protected void serializeJson(Writer writer, DBCPlan plan, String signature, DBCQueryPlannerSerialInfo info) throws IOException {

        JsonObject root = new JsonObject();

        root.add(PROP_VERSION, new JsonPrimitive(info.version()));
        root.add(PROP_SIGNATURE,  new JsonPrimitive(signature));
        root.add(PROP_DATE, new JsonPrimitive(LocalDateTime.now().toString()));
        root.add(PROP_SQL, new JsonPrimitive(plan.getQueryString()));

        JsonArray nodes = new JsonArray();


        for(DBCPlanNode node : plan.getPlanNodes(null)) {
            nodes.add(serializeNode(node,info));
        }


        root.add(PROP_NODES, nodes);

        writer.write(gson.toJson(root));
    }
    
     protected String getVersion(@NotNull JsonObject o)  throws InvocationTargetException {
        
        JsonElement queryElement = o.get(AbstractExecutionPlanSerializer.PROP_VERSION);
        
        if (queryElement == null) {
            
            throw new InvocationTargetException(new Exception("Incorrect file format"));
            
        }

        return queryElement.getAsString();
    }
    
    protected String getQuery(@NotNull JsonObject o)  throws InvocationTargetException {
        
        JsonElement queryElement = o.get(AbstractExecutionPlanSerializer.PROP_SQL);
        
        if (queryElement == null) {
            
            throw new InvocationTargetException(new Exception("Incorrect file format"));
            
        }

        return queryElement.getAsString();
    }


}
