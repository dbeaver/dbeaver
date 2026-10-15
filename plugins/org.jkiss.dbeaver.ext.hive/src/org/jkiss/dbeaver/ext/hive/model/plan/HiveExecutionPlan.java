/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hive.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanSourceFormat;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

final class HiveExecutionPlan extends AbstractExecutionPlan {
    private static final String EXPLAIN_PREFIX = "EXPLAIN FORMATTED ";

    private final String query;
    private final HivePlanParser parser;
    private String planSource;
    private List<HivePlanNode> rootNodes = List.of();
    private boolean hasRows;

    HiveExecutionPlan(@NotNull String query, @NotNull HivePlanParser parser) {
        this.query = query;
        this.parser = parser;
    }

    void explain(@NotNull JDBCSession session) throws DBCException {
        StringBuilder plan = new StringBuilder();
        try (JDBCPreparedStatement statement = session.prepareStatement(getPlanQueryString());
             JDBCResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String line = resultSet.getString(1);
                if (line == null) {
                    continue;
                }
                if (!plan.isEmpty()) {
                    plan.append('\n');
                }
                plan.append(line);
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading Hive execution plan", e, session.getExecutionContext());
        }
        if (plan.isEmpty()) {
            throw new DBCException("Hive returned an empty execution plan");
        }

        planSource = plan.toString();
        try {
            rootNodes = parser.parse(planSource);
        } catch (RuntimeException e) {
            throw new DBCException("Invalid JSON returned by Hive EXPLAIN FORMATTED", e, session.getExecutionContext());
        }
        hasRows = hasRows(rootNodes);
    }

    private static boolean hasRows(@NotNull List<HivePlanNode> nodes) {
        for (HivePlanNode node : nodes) {
            if (node.getNodeRowCount() != null) {
                return true;
            }
            if (hasRows(node.getNested())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public String getQueryString() {
        return query;
    }

    @NotNull
    @Override
    public String getPlanQueryString() {
        return EXPLAIN_PREFIX + query;
    }

    @NotNull
    @Override
    public DBCPlanSourceFormat getPlanSourceDataFormat() {
        return DBCPlanSourceFormat.JSON;
    }

    @Nullable
    @Override
    public Object getPlanSourceData() {
        return planSource;
    }

    @Nullable
    @Override
    public Object getPlanFeature(@NotNull String feature) {
        if (DBCPlanCostNode.FEATURE_PLAN_ROWS.equals(feature)) {
            return hasRows;
        }
        return super.getPlanFeature(feature);
    }

    @NotNull
    @Override
    public List<? extends DBCPlanNode> getPlanNodes(@NotNull Map<String, Object> options) {
        return rootNodes;
    }
}
