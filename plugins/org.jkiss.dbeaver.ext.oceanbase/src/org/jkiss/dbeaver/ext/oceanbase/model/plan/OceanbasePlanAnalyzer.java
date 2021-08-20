/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oceanbase.model.plan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.oceanbase.mysql.model.OceanbaseMySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.*;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.dbeaver.model.impl.plan.ExecutionPlanDeserializer;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OceanbasePlanAnalyzer extends AbstractExecutionPlanSerializer implements DBCQueryPlanner {
    private final OceanbaseMySQLDataSource dataSource;

    public OceanbasePlanAnalyzer(OceanbaseMySQLDataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static OceanbasePlanJSON explain(JDBCSession session, String query) throws DBCException {
        final SQLDialect dialect = SQLUtils.getDialectFromObject(session.getDataSource());
        final String plainQuery = SQLUtils.stripComments(dialect, query).toUpperCase();
        final String firstKeyword = SQLUtils.getFirstKeyword(dialect, plainQuery);
        if (!"SELECT".equalsIgnoreCase(firstKeyword) && !"WITH".equalsIgnoreCase(firstKeyword)) {
            throw new DBCException("Only SELECT statements could produce execution plan");
        }
        return new OceanbasePlanJSON(session, query);
    }

    @Override
    public void serialize(@NotNull Writer planData, @NotNull DBCPlan plan) throws IOException {
        serializeJson(planData, plan, dataSource.getInfo().getDriverName(), new DBCQueryPlannerSerialInfo() {

            @Override
            public String version() {
                return MySQLConstants.TYPE_JSON;
            }

            @Override
            public void addNodeProperties(DBCPlanNode node, JsonObject nodeJson) {
                JsonObject attributes = new JsonObject();
                OceanbasePlanNodeJSON jsNode = (OceanbasePlanNodeJSON) node;
                for (Map.Entry<String, String> e : jsNode.getNodeProps().entrySet()) {
                    attributes.add(e.getKey(), new JsonPrimitive(CommonUtils.notEmpty(e.getValue())));
                }
                nodeJson.add(PROP_ATTRIBUTES, attributes);
            }
        });
    }

    @Override
    public DBCPlan deserialize(@NotNull Reader planData) throws InvocationTargetException {
        JsonObject jo = new JsonParser().parse(planData).getAsJsonObject();

        String query = getQuery(jo);

        ExecutionPlanDeserializer<OceanbasePlanNodeJSON> loader = new ExecutionPlanDeserializer<>();
        List<OceanbasePlanNodeJSON> rootNodes = loader.loadRoot(dataSource, jo,
                (datasource, node, parent) -> new OceanbasePlanNodeJSON(parent, getNodeAttributes(node)));
        return new OceanbasePlanJSON(dataSource, query, rootNodes);
    }

    private static Map<String, String> getNodeAttributes(JsonObject nodeObject) {
        Map<String, String> attributes = new HashMap<>();

        JsonObject attrs = nodeObject.getAsJsonObject(PROP_ATTRIBUTES);
        for (Map.Entry<String, JsonElement> attr : attrs.entrySet()) {
            attributes.put(attr.getKey(), attr.getValue().getAsString());
        }

        return attributes;
    }

    @Override
    public DBPDataSource getDataSource() {
        return this.dataSource;
    }

    @NotNull
    @Override
    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query, @NotNull DBCQueryPlannerConfiguration configuration)
            throws DBCException {
        return explain((JDBCSession) session, query);
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }

}
