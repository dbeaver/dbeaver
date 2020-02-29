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

package org.jkiss.dbeaver.ext.postgresql.model.plan;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Plan node loaded from external JSON file
 */
public class PostgrePlanNodeExternal extends PostgrePlanNodeBase<PostgrePlanNodeExternal> {

    private PostgrePlanNodeExternal(PostgreDataSource dataSource, PostgrePlanNodeExternal parent) {
        super(dataSource, parent);
    }

    protected PostgrePlanNodeExternal(PostgreDataSource dataSource, JsonObject data, PostgrePlanNodeExternal parent) {
        super(dataSource, parent);

        Map<String, String> attributes = new HashMap<String, String>(); 
        JsonArray attrs =  data.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_ATTRIBUTES);

        attributes.put(PostgrePlanNodeBase.ATTR_NODE_TYPE, data.get(AbstractExecutionPlanSerializer.PROP_TYPE).getAsString());

        for(JsonElement attr : attrs) {
            Object[] props =   attr.getAsJsonObject().entrySet().toArray();
            if (props.length > 0) {
                Entry<String, JsonElement> p = (Entry<String, JsonElement>) props[0];
                attributes.put(p.getKey(), p.getValue().getAsString());
            }

        }

        setAttributes(attributes);
    }



}
