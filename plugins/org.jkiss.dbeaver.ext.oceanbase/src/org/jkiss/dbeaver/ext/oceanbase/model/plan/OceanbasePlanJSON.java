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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.oceanbase.mysql.model.OceanbaseMySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.utils.CommonUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class OceanbasePlanJSON extends AbstractExecutionPlan {
    private static final Gson gson = new Gson();
    
    private final List<OceanbasePlanNodeJSON> rootNodes;
    
    private OceanbaseMySQLDataSource dataSource;
    private String query;

    OceanbasePlanJSON(JDBCSession session, String query) throws DBCException {
        this.dataSource = (OceanbaseMySQLDataSource) session.getDataSource();
        this.query = query;
        try (JDBCPreparedStatement dbStat = session.prepareStatement(getPlanQueryString())) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                List<OceanbasePlanNodeJSON> nodes = new ArrayList<>();
                dbResult.next();
                String jsonPlan = dbResult.getString(1);

                JsonObject planObject = gson.fromJson(jsonPlan, JsonObject.class);
                JsonObject queryBlock = planObject.getAsJsonObject();

                OceanbasePlanNodeJSON rootNode = new OceanbasePlanNodeJSON(null, "select", queryBlock);

                if (CommonUtils.isEmpty(rootNode.getNested()) && rootNode.getProperty("message") != null) {
                    throw new DBCException("Can't explain plan: " + rootNode.getProperty("message"));
                }
                nodes.add(rootNode);
                rootNodes = nodes;
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    OceanbasePlanJSON(MySQLDataSource dataSource, String query, List<OceanbasePlanNodeJSON> rootNodes) {
        this.dataSource = (OceanbaseMySQLDataSource) dataSource;
        this.query = query;
        this.rootNodes = rootNodes;
    }

    @Override
    public Object getPlanFeature(String feature) {
        if (DBCPlanCostNode.FEATURE_PLAN_COST.equals(feature) || DBCPlanCostNode.FEATURE_PLAN_ROWS.equals(feature)) {
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
        return "EXPLAIN FORMAT=JSON " + query + ";";
    }

    @Override
    public List<OceanbasePlanNodeJSON> getPlanNodes(Map<String, Object> options) {
        return Collections.unmodifiableList(rootNodes);
    }
    
    public DBPDataSource getDataSource() {
        return this.dataSource;
    }

}
