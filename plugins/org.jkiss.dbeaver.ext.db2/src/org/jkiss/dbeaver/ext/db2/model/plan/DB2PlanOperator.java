/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DB2 EXPLAIN_OPERATOR table
 * 
 * @author Denis Forveille
 */
public class DB2PlanOperator extends DB2PlanNode {

    private static final String SEL_BASE_SELECT;

    static {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("SELECT *");
        sb.append(" FROM %s.%s");
        sb.append(" WHERE EXPLAIN_REQUESTER = ?"); // 1
        sb.append("   AND EXPLAIN_TIME = ?"); // 2
        sb.append("   AND SOURCE_NAME = ?");// 3
        sb.append("   AND SOURCE_SCHEMA = ?");// 4
        sb.append("   AND SOURCE_VERSION = ?");// 5
        sb.append("   AND EXPLAIN_LEVEL = ?");// 6
        sb.append("   AND STMTNO = ?");// 7
        sb.append("   AND SECTNO = ?");// 8
        sb.append("   AND OPERATOR_ID = ?");// 9
        sb.append(" ORDER BY %s");// 9
        sb.append(" WITH UR");
        SEL_BASE_SELECT = sb.toString();
    }

    private DB2PlanStatement db2Statement;
    private String planTableSchema;

    private List<DB2PlanOperatorArgument> listArguments;
    private List<DB2PlanOperatorPredicate> listPredicates;

    private String displayName;
    private String nodename;

    private Integer operatorId;
    private DB2PlanOperatorType operatorType;
    private Double totalCost;

    private Double estimatedCardinality = -1d;

    // ------------
    // Constructors
    // ------------

    public DB2PlanOperator(JDBCSession session, JDBCResultSet dbResult, DB2PlanStatement db2Statement, String planTableSchema)
        throws SQLException
    {

        this.db2Statement = db2Statement;
        this.planTableSchema = planTableSchema;

        this.operatorId = JDBCUtils.safeGetInteger(dbResult, "OPERATOR_ID");
        this.operatorType = CommonUtils.valueOf(DB2PlanOperatorType.class, JDBCUtils.safeGetString(dbResult, "OPERATOR_TYPE"));
        this.totalCost = JDBCUtils.safeGetDouble(dbResult, "TOTAL_COST");

        this.nodename = buildName(operatorId);
        this.displayName = nodename + " - " + operatorType;

        loadChildren(session);
    }

    @Override
    public void setEstimatedCardinality(Double estimatedCardinality)
    {
        // DF: not sure if this rule is correct. Seems to be OK
        this.estimatedCardinality = Math.max(this.estimatedCardinality, estimatedCardinality);
    }

    @Override
    public String toString()
    {
        return displayName;
    }

    @Override
    public String getNodeName()
    {
        return nodename;
    }

    @Override
    public String getNodeType() {
        return CommonUtils.toString(operatorType);
    }

    @Override
    public String getNodeDescription() {
        return null;
    }

    // --------
    // Helpers
    // --------
    public static String buildName(Integer operatorId)
    {
        return String.valueOf(operatorId);
    }

    // ----------------
    // Pproperties
    // ----------------

    @Property(viewable = true, order = 1)
    public DB2PlanOperatorType getOperatorType()
    {
        return operatorType;
    }

    @Property(viewable = true, order = 2)
    public Integer getOperatorId()
    {
        return operatorId;
    }

    @Property(viewable = true, order = 3)
    public String getDisplayName()
    {
        return ""; // Looks better without a name...
    }

    @Property(viewable = true, order = 4, format = DB2Constants.PLAN_COST_FORMAT)
    public Double getTotalCost()
    {
        return totalCost;
    }

    @Property(viewable = true, order = 5, format = DB2Constants.PLAN_COST_FORMAT)
    public Double getEstimatedCardinality()
    {
        return estimatedCardinality;
    }

    @Property(viewable = false, order = 6)
    public List<DB2PlanOperatorArgument> getArguments()
    {
        return listArguments;
    }

    @Property(viewable = false, order = 7)
    public List<DB2PlanOperatorPredicate> getPredicates()
    {
        return listPredicates;
    }

    // -------------
    // Load children
    // -------------
    private void loadChildren(JDBCSession session) throws SQLException
    {

        listArguments = new ArrayList<>();
        try (JDBCPreparedStatement sqlStmt = session.prepareStatement(
            String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_ARGUMENT", "ARGUMENT_TYPE")))
        {
            setQueryParameters(sqlStmt);
            try (JDBCResultSet res = sqlStmt.executeQuery()) {
                while (res.next()) {
                    listArguments.add(new DB2PlanOperatorArgument(res));
                }
            }
        }

        listPredicates = new ArrayList<>();
        try (JDBCPreparedStatement sqlStmt = session.prepareStatement(
            String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_PREDICATE", "PREDICATE_ID")))
        {
            setQueryParameters(sqlStmt);
            try (JDBCResultSet res = sqlStmt.executeQuery()) {
                while (res.next()) {
                    listPredicates.add(new DB2PlanOperatorPredicate(res, this));
                }
            }
        }
    }

    private void setQueryParameters(JDBCPreparedStatement sqlStmt) throws SQLException
    {
        sqlStmt.setString(1, db2Statement.getExplainRequester());
        sqlStmt.setTimestamp(2, db2Statement.getExplainTime());
        sqlStmt.setString(3, db2Statement.getSourceName());
        sqlStmt.setString(4, db2Statement.getSourceSchema());
        sqlStmt.setString(5, db2Statement.getSourceVersion());
        sqlStmt.setString(6, db2Statement.getExplainLevel());
        sqlStmt.setInt(7, db2Statement.getStmtNo());
        sqlStmt.setInt(8, db2Statement.getSectNo());
        sqlStmt.setInt(9, operatorId);
    }
}
