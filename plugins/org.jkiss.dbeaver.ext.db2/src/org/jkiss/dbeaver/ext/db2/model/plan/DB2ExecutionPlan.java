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
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanSourceFormat;
import org.jkiss.dbeaver.model.impl.local.CachedResultSet;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DB2 execution plan analyser
 * 
 * @author Denis Forveille
 */
public class DB2ExecutionPlan extends AbstractExecutionPlan {

    private static final Log LOG = Log.getLog(DB2ExecutionPlan.class);

    // See init below
    private static final String PT_DELETE = """
            DELETE FROM %s.EXPLAIN_INSTANCE I
             WHERE EXISTS (SELECT 1
             FROM %s.EXPLAIN_STATEMENT S
             WHERE S.EXPLAIN_TIME = I.EXPLAIN_TIME
              AND S.SOURCE_NAME = I.SOURCE_NAME
              AND S.SOURCE_SCHEMA = I.SOURCE_SCHEMA
              AND S.SOURCE_VERSION = I.SOURCE_VERSION
              AND QUERYNO = ?)""";
    private static final String PT_EXPLAIN = "EXPLAIN PLAN SET QUERYNO = %d FOR %s";
    private static final String SEL_STMT = "SELECT * FROM %s.EXPLAIN_STATEMENT WHERE QUERYNO = ? AND EXPLAIN_LEVEL = 'P' WITH UR";

    private static final AtomicInteger STMT_NO_GEN = new AtomicInteger(Long.valueOf(System.currentTimeMillis() / 10000000L).intValue());

    private final String query;
    private final String planTableSchema;

    private List<DB2PlanNode> listNodes;
    private String planText;
    private CachedResultSet planStreamsRS;
    private CachedResultSet planOperatorRS;

    // ------------
    // Constructors
    // ------------

    public DB2ExecutionPlan(String query, String planTableSchema)
    {
        this.query = query;
        this.planTableSchema = planTableSchema;
    }

    @Nullable
    @Override
    public Object getPlanFeature(@NotNull String feature) {
        if (DBCPlanCostNode.FEATURE_PLAN_COST.equals(feature) ||
            DBCPlanCostNode.FEATURE_PLAN_DURATION.equals(feature) ||
            DBCPlanCostNode.FEATURE_PLAN_ROWS.equals(feature))
        {
            return true;
        } else if (DBCPlanCostNode.PLAN_DURATION_MEASURE.equals(feature)) {
            return "ms";
        }

        return super.getPlanFeature(feature);
    }

    // ----------------
    // Standard Getters
    // ----------------

    @NotNull
    @Override
    public String getQueryString()
    {
        return query;
    }

    @NotNull
    @Override
    public String getPlanQueryString() {
        return String.format(PT_EXPLAIN, STMT_NO_GEN.get(), query);
    }

    @NotNull
    @Override
    public DBCPlanSourceFormat getPlanSourceDataFormat() {
        return DBCPlanSourceFormat.RESULT_SET;
    }

    @Nullable
    @Override
    public Object getPlanSourceData() {
        return List.of(planOperatorRS, planStreamsRS);
    }

    @NotNull
    @Override
    public List<? extends DBCPlanNode> getPlanNodes(@NotNull Map<String, Object> options)
    {
        return listNodes;
    }

    // ----------------
    // Business Methods
    // ----------------

    public void explain(JDBCSession session) throws DBCException
    {
        Integer stmtNo = STMT_NO_GEN.incrementAndGet();

        String explainStmt = String.format(PT_EXPLAIN, stmtNo, query);
        LOG.debug("Schema=" + planTableSchema + " : " + explainStmt);

        try {

            // Start by cleaning old rows for safety
            cleanExplainTables(session, stmtNo, planTableSchema);

            // Explain
            try (JDBCPreparedStatement dbStat = session.prepareStatement(explainStmt)) {
                dbStat.execute();
            }

            // Build Node Structure
            DB2PlanStatement db2PlanStatement;
            try (JDBCPreparedStatement dbStat = session.prepareStatement(String.format(SEL_STMT, planTableSchema))) {
                dbStat.setInt(1, stmtNo);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    dbResult.next();
                    db2PlanStatement = new DB2PlanStatement(session, dbResult, planTableSchema);
                }
            }

            planOperatorRS = db2PlanStatement.getOperatorRS();
            planStreamsRS = db2PlanStatement.getStreamsRS();
            listNodes = db2PlanStatement.buildNodes();

            // Clean afterward
            cleanExplainTables(session, stmtNo, planTableSchema);

        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    // ----------------
    // Helpers
    // ----------------
    private void cleanExplainTables(JDBCSession session, Integer stmtNo, String planTableSchema) throws SQLException
    {
        // Delete previous statement rows
        try (JDBCPreparedStatement dbStat = session.prepareStatement(String.format(PT_DELETE, planTableSchema, planTableSchema))) {
            dbStat.setInt(1, stmtNo);
            dbStat.execute();
        }
    }

}
