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

import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * MySQL execution plan analyser
 */
public abstract class MySQLPlanAbstract extends AbstractExecutionPlan {

    protected MySQLDataSource dataSource;
    protected String query;

    public MySQLPlanAbstract(MySQLDataSource dataSource, String query) {
        this.dataSource = dataSource;
        this.query = query;
    }
    
    protected Map<String,String> getNodeAttributes(JsonObject nodeObject){
        Map<String,String> attributes = new HashMap<>(1);

        JsonArray attrs =  nodeObject.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_ATTRIBUTES);
        for(JsonElement attr : attrs) {
            for (Entry<String, JsonElement> p : attr.getAsJsonObject().entrySet()) {
                attributes.put(p.getKey(), p.getValue().getAsString());
            }
        }

        return attributes;
    }

}
