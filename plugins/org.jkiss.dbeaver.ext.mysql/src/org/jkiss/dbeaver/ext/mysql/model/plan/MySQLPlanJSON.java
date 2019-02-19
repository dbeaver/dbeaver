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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MySQL JSON plan
 */
public class MySQLPlanJSON extends MySQLPlanAbstract {

    private final String[] nodePropNames = new String[] {
        "ordering_operation", "grouping_operation", "nested_loop", "table",
        "attached_subqueries", "optimized_away_subqueries", "materialized_from_subquery", "duplicates_removal"
    };

    private static final Gson gson = new Gson();

    private List<MySQLPlanNodeJSON> rootNodes;

    public MySQLPlanJSON(JDBCSession session, String query) throws DBCException {
        super((MySQLDataSource) session.getDataSource(), query);

        String plainQuery = SQLUtils.stripComments(SQLUtils.getDialectFromObject(dataSource), query).toUpperCase();
        if (!plainQuery.startsWith("SELECT")) {
            throw new DBCException("Only SELECT statements could produce execution plan");
        }
        try (JDBCPreparedStatement dbStat = session.prepareStatement(getPlanQueryString())) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                List<MySQLPlanNodeJSON> nodes = new ArrayList<>();

                dbResult.next();
                String jsonPlan = dbResult.getString(1);

                JsonObject planObject = gson.fromJson(jsonPlan, JsonObject.class);
                JsonObject queryBlock = planObject.getAsJsonObject("query_block");

                MySQLPlanNodeJSON rootNode = new MySQLPlanNodeJSON(null, "select", queryBlock);
/*
                for (Map.Entry<String, JsonElement> prop : queryBlock.entrySet()) {
                    JsonElement value = prop.getValue();
                    switch (prop.getKey()) {
                        case "select_id":
                            selectId = value.getAsLong();
                            break;
                        case "query_cost":
                            break;
                        case "message":
                            errorMessage = value.getAsString();
                            break;
                        default:
                            if (value instanceof JsonObject) {
                                MySQLPlanNodeJSON nodeJSON = new MySQLPlanNodeJSON(null, prop.getKey(), (JsonObject) value);
                                nodes.add(nodeJSON);
                            } else if (value instanceof JsonObject) {

                            }
                    }
                }
*/
                if (CommonUtils.isEmpty(rootNode.getNested()) && rootNode.getProperty("message") != null) {
                    throw new DBCException("Can't explain plan: " + rootNode.getProperty("message"));
                }
                nodes.add(rootNode);

                rootNodes = nodes;
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Override
    public Object getPlanFeature(String feature) {
        if (DBCPlanCostNode.FEATURE_PLAN_COST.equals(feature) ||
            DBCPlanCostNode.FEATURE_PLAN_ROWS.equals(feature)) {
            return true;
        }
        return super.getPlanFeature(feature);
    }

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public String getPlanQueryString() {
        return "EXPLAIN FORMAT=JSON " + query;
    }

    @Override
    public List<MySQLPlanNodeJSON> getPlanNodes(Map<String, Object> options) {
        return rootNodes;
    }

}
