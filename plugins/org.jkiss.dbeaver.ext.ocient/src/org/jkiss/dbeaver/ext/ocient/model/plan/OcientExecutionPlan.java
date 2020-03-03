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
package org.jkiss.dbeaver.ext.ocient.model.plan;


import org.jkiss.dbeaver.ext.ocient.model.OcientDataSource;
import org.jkiss.dbeaver.ext.ocient.model.plan.OcientPlanNodeJson;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OcientExecutionPlan extends AbstractExecutionPlan {


    private String query;
    private List<OcientPlanNodeJson> rootNodes;
    private OcientDataSource dataSource;


    private static final Gson gson = new Gson();

    public OcientExecutionPlan(String query)
    {
        this.query = query;
    }

    public OcientExecutionPlan(String query, List<OcientPlanNodeJson> rootNodes) {
        this.query = query;
        this.rootNodes = rootNodes;
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

    @Override
    public String getQueryString()
    {
        return query;
    }

    @Override
    public String getPlanQueryString() {
        return "explain json" + query;
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options)
    {
        return rootNodes;
    }

    public void explain(DBCSession session) throws DBCException
    {
        String explainString = getExplainString(session, getPlanQueryString());

        JsonObject planObject = gson.fromJson(explainString, JsonObject.class);
        JsonObject planRoot = planObject.getAsJsonObject("rootNode");
        JsonObject planHeader = planObject.getAsJsonObject("header");
        rootNodes = new ArrayList<>();
        
        OcientPlanNodeJson headerNode = new OcientPlanNodeJson(null, "header", planHeader);
        OcientPlanNodeJson rootNode = new OcientPlanNodeJson(null, "Root", planRoot);
        rootNodes.add(headerNode);
        rootNodes.add(rootNode);

    }

    private String getExplainString(DBCSession session, String sql) throws DBCException
    {
        JDBCSession connection = (JDBCSession) session;
        String plan = "";
        try {
            JDBCPreparedStatement dbStat = connection.prepareStatement(getPlanQueryString());
            JDBCResultSet dbResult = dbStat.executeQuery();
            while (dbResult.next()) {
                String planLine = dbResult.getString(1);
                plan += planLine;
            }
            dbResult.close();

        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
        return plan;
    }
}
