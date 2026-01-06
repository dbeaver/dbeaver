/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model.plan;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants.ExplainPlan;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataTypeDomain;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class AltibaseExecutionPlan extends AbstractExecutionPlan {

    static final Log log = Log.getLog(AltibaseExecutionPlan.class);
    
    private AltibaseDataSource dataSource;
    private JDBCSession session;
    private String query;
    private List<AltibasePlanNode> rootNodes;
    private String planQuery;

    private static final String setExplainPlan = "setExplainPlan";

    /**
     * Read from database
     */
    public AltibaseExecutionPlan(AltibaseDataSource dataSource, JDBCSession session, String query) {
        this.dataSource = dataSource;
        this.session = session;
        this.query = query;
        this.planQuery = "";
    }
    
    /** 
     * Deserialize execution plan
     */
    public AltibaseExecutionPlan(String query, List<AltibasePlanNode> nodes) {
        this.query = query;
        this.rootNodes = nodes;
    }

    /**
     * Get Execution Plan by firing query to database. 
     */
    public void explain() throws DBException {
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement(getQueryString());
            // Read explained plan
            try {
                String plan = getExplainPlan(session, query);
                rootNodes = AltibasePlanBuilder.build(dataSource, plan);
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public String getPlanQueryString() throws DBException {
        return planQuery;
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options) {
        return rootNodes;
    }

    /**
     *  Get explain plan result string using Java reflection. 
     */
    private String getExplainPlan(JDBCSession session, String query) {
        Statement stmt = null;

        Connection conn = null;
        Class<? extends Connection> clazz = null;
        Method method = null;
        ExplainPlan expPlan = null;
        
        try {
            conn = session.getOriginal();
            clazz = conn.getClass();
            
            /* 
             * There are two setExplain methods in Connection class: 
             * The first one's argument is boolean, the second one's argument is byte.
             * Here, the second method is required.
            */
            method = clazz.getMethod(setExplainPlan, byte.class);
            
            if (method == null) {
                throw new NoSuchMethodException(String.format(
                        "Unable to find the target method: [class] %s, [method] %s, [argument type] %s", 
                        clazz.getName(), setExplainPlan,  AltibaseDataTypeDomain.BYTE.getTypeName().toLowerCase()));
            }

            expPlan = AltibaseConstants.ExplainPlan.getByIndex(
                    dataSource.getContainer().getPreferenceStore().getInt(
                            AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE));

            method.invoke(conn, expPlan.getArgValue());
            stmt = conn.prepareStatement(query);
            
            // In case of EXPLAIN_PLAN_ON, need to execute query
            if (expPlan == AltibaseConstants.ExplainPlan.ON) {
                if (query.trim().toUpperCase().startsWith("SELECT")) {
                    stmt.getClass().getMethod("executeQuery").invoke(stmt);
                } else {
                    stmt.getClass().getMethod("execute").invoke(stmt);
                }
            }
            
            planQuery = (String) stmt.getClass().getMethod("getExplainPlan").invoke(stmt);

        } catch (Exception e) {
            log.error("Failed to execute explain plan: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return planQuery;
    }
}
