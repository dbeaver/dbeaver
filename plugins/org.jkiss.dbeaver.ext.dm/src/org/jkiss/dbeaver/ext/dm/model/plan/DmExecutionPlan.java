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
package org.jkiss.dbeaver.ext.dm.model.plan;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.utils.IntKeyMap;
import org.jkiss.utils.SecurityUtils;

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dm execution plan analyser
 */
public class DmExecutionPlan extends AbstractExecutionPlan {

    private static final Log log = Log.getLog(DmExecutionPlan.class);

    private DmDataSource dataSource;
    private JDBCSession session;
    private String query;
    private Object savedQueryId;
    private List<DmPlanNode> rootNodes;
    private String planStmtId;
    private String planTableName;

    DmExecutionPlan(DmDataSource dataSource, JDBCSession session, String query) {
        this.dataSource = dataSource;
        this.session = session;
        this.query = query;
    }

    DmExecutionPlan(DmDataSource dataSource, JDBCSession session, Object savedQueryId) {
        this.dataSource = dataSource;
        this.session = session;
        this.savedQueryId = savedQueryId;
    }

    public DmExecutionPlan(String query, List<DmPlanNode> nodes) {
        this.query = query;
        this.rootNodes = nodes;
    }

    @Override
    public Object getPlanFeature(String feature) {
        if (DBCPlanCostNode.FEATURE_PLAN_COST.equals(feature) ||
            DBCPlanCostNode.FEATURE_PLAN_DURATION.equals(feature) ||
            DBCPlanCostNode.FEATURE_PLAN_ROWS.equals(feature)) {
            return true;
        } else if (DBCPlanCostNode.PLAN_DURATION_MEASURE.equals(feature)) {
            return "KC";
        }

        return super.getPlanFeature(feature);
    }

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public String getPlanQueryString() throws DBException {
        if (planTableName == null) {
            // Detect plan table
            planTableName = dataSource.getPlanTableName(session);
            if (planTableName == null) {
                throw new DBCException("Plan table not found - query can't be explained");
            }
        }

        if (planStmtId == null) {
            planStmtId = SecurityUtils.generateUniqueId();
        }

        return "EXPLAIN PLAN " + "\n" +
            "SET PLAN_ID = '" + planStmtId + "'\n" +
            "INTO " + planTableName + "\n" +
            "FOR " + query;
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options) {
        return rootNodes;
    }

    public void explain() throws DBException {
        String planQuery = getPlanQueryString();
        try {

            // Delete previous statement rows
            // (actually there should be no statement with this id -
            // but let's do it, just in case)
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "DELETE FROM " + planTableName +
                    " WHERE PLAN_ID=? ");
            try {
                dbStat.setString(1, planStmtId);
                dbStat.execute();
            } finally {
                dbStat.close();
            }

            // Explain plan
            dbStat = session.prepareStatement(planQuery);
            try {
                try {
                    // Bind parameters if any
                    ParameterMetaData parameterMetaData = dbStat.getParameterMetaData();
                    if (parameterMetaData != null && parameterMetaData.getParameterCount() > 0) {
                        for (int i = 0; i < parameterMetaData.getParameterCount(); i++) {
                            dbStat.setNull(i + 1, Types.VARCHAR);
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                }
                dbStat.execute();
            } finally {
                dbStat.close();
            }

            // Read explained plan
            dbStat = session.prepareStatement(
                "SELECT * FROM " + planTableName +
                    " WHERE PLAN_ID=? ORDER BY ID");
            readPlanNodes(dbStat);

        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    public void readHistoric() throws DBException {
        try {
            // Read explained plan
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + DmUtils.getSysSchemaPrefix(dataSource) + "DBA_HIST_SQL_PLAN" +
                    " WHERE SQL_ID=? ORDER BY ID");
            readPlanNodes(dbStat);

        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    private void readPlanNodes(JDBCPreparedStatement dbStat) throws SQLException {
        try {
            dbStat.setString(1, planStmtId);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                rootNodes = new ArrayList<>();
                IntKeyMap<DmPlanNode> allNodes = new IntKeyMap<>();
                while (dbResult.next()) {
                    DmPlanNode node = new DmPlanNode(dataSource, allNodes, dbResult);
                    allNodes.put(node.getId(), node);
                    if (node.getParent() == null) {
                        rootNodes.add(node);
                    }
                }
            }
        } finally {
            dbStat.close();
        }

        // Update costs
        for (DmPlanNode node : rootNodes) {
            node.updateCosts();
        }
    }

}
