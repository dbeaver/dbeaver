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
package org.jkiss.dbeaver.model.impl.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerDeSerialInfo;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExecutionPlanDeserializer<NODE extends DBCPlanNode> {

    public List<NODE> loadRoot(DBPDataSource datasource, JsonObject plan, DBCQueryPlannerDeSerialInfo<NODE> info) throws InvocationTargetException {
        
        final String signature = plan.get(AbstractExecutionPlanSerializer.PROP_SIGNATURE).getAsString();
        final String currSignature = datasource.getInfo().getDriverName();
        
        if (!signature.equals(currSignature)) {
            throw new InvocationTargetException(new Throwable(String.format("Incorrect plan signature found - %s, expected - %s", signature,currSignature)));
        }
        
        final List<NODE> nodes = new ArrayList<>(1);
        plan.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_NODES).forEach((e) -> {
            nodes.add(loadNode(datasource, e.getAsJsonObject(), null, info));
        });
        return nodes;
    }

    private NODE loadNode(DBPDataSource dataSource, JsonObject nodeObject, NODE parent,
            DBCQueryPlannerDeSerialInfo<NODE> info) {
        
        NODE node = info.createNode(dataSource, nodeObject, parent);
        JsonArray childs = nodeObject.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_CHILD);
        
        if (childs != null) {
            childs.forEach((e) -> {
                ((Collection<NODE>) node.getNested()).add(loadNode(dataSource, e.getAsJsonObject(), node, info));
            });
        }
        
        return node;
    }

}
