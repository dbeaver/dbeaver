/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * DB2 EXPLAIN_STATEMENT table
 *
 * @author Denis Forveille
 */
public class DB2PlanStatement {

    private static String SEL_BASE_SELECT;

    static {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("SELECT *");
        sb.append(" FROM %s.%s");
        sb.append(" WHERE EXPLAIN_REQUESTER = ?"); // 1
        sb.append("   AND EXPLAIN_TIME = ?");// 2
        sb.append("   AND SOURCE_NAME = ?");// 3
        sb.append("   AND SOURCE_SCHEMA = ?");// 4
        sb.append("   AND SOURCE_VERSION = ?");// 5
        sb.append("   AND EXPLAIN_LEVEL = ?");// 6
        sb.append("   AND STMTNO = ?");// 7
        sb.append("   AND SECTNO = ?");// 8
        sb.append(" WITH UR");// 8
        SEL_BASE_SELECT = sb.toString();
    }

    private List<DB2PlanOperator> listOperators;
    private List<DB2PlanObject> listObjects;
    private List<DB2PlanStream> listStreams;

    private DB2PlanInstance planInstance;
    private String planTableSchema;

    private String explainRequester;
    private Timestamp explainTime;
    private String sourceName;
    private String sourceSchema;
    private String sourceVersion;
    private String explainLevel;
    private Integer stmtNo;
    private Integer sectNo;
    private Double totalCost;
    private String statementText;
    private Integer queryDegree;

    // ------------
    // Constructors
    // ------------
    public DB2PlanStatement(JDBCExecutionContext context, JDBCResultSet dbResult, String planTableSchema) throws SQLException
    {

        this.planTableSchema = planTableSchema;

        this.explainRequester = JDBCUtils.safeGetStringTrimmed(dbResult, "EXPLAIN_REQUESTER");
        this.explainTime = JDBCUtils.safeGetTimestamp(dbResult, "EXPLAIN_TIME");
        this.sourceName = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCE_NAME");
        this.sourceSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCE_SCHEMA");
        this.sourceVersion = JDBCUtils.safeGetStringTrimmed(dbResult, "SOURCE_VERSION");
        this.explainLevel = JDBCUtils.safeGetStringTrimmed(dbResult, "EXPLAIN_LEVEL");
        this.stmtNo = JDBCUtils.safeGetInteger(dbResult, "STMTNO");
        this.sectNo = JDBCUtils.safeGetInteger(dbResult, "SECTNO");
        this.totalCost = JDBCUtils.safeGetDouble(dbResult, "TOTAL_COST");
        this.queryDegree = JDBCUtils.safeGetInteger(dbResult, "QUERY_DEGREE");

        this.statementText = JDBCUtils.safeGetString(dbResult, "STATEMENT_TEXT");

        loadChildren(context);
    }

    // ----------------
    // Business Methods
    // ----------------
    public Collection<DB2PlanNode> buildNodes()
    {
        Map<String, DB2PlanNode> mapNodes = new HashMap<String, DB2PlanNode>(32);

        // First Put all current objects and operators in a map
        for (DB2PlanNode planNode : listOperators) {
            mapNodes.put(planNode.getNodeName(), planNode);
        }
        for (DB2PlanNode planNode : listObjects) {
            mapNodes.put(planNode.getNodeName(), planNode);
        }

        // Then Based on streams, establish relationships between nodes
        DB2PlanNode sourceNode;
        DB2PlanNode targetNode;
        for (DB2PlanStream planStream : listStreams) {
            sourceNode = mapNodes.get(planStream.getSourceName());
            targetNode = mapNodes.get(planStream.getTargetName());

            targetNode.getNested().add(sourceNode);
            targetNode.setEstimatedCardinality(planStream.getStreamCount());
            sourceNode.setParent(targetNode);
        }

        // // Keep only the "root" node
        DB2PlanNode rootNode = mapNodes.get(String.valueOf("1"));
        return Collections.singletonList(rootNode);
    }

    // -------------
    // Load children
    // -------------
    private void loadChildren(JDBCExecutionContext context) throws SQLException
    {

        listObjects = new ArrayList<DB2PlanObject>(32);
        JDBCPreparedStatement sqlStmt = context.prepareStatement(String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_OBJECT"));
        try {
            setQueryParameters(sqlStmt);
            JDBCResultSet res = sqlStmt.executeQuery();
            try {
                while (res.next()) {
                    listObjects.add(new DB2PlanObject(res));
                }
            } finally {
                res.close();
            }
        } finally {
            sqlStmt.close();
        }

        listOperators = new ArrayList<DB2PlanOperator>(64);
        sqlStmt = context.prepareStatement(String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_OPERATOR"));
        try {
            setQueryParameters(sqlStmt);
            JDBCResultSet res = sqlStmt.executeQuery();
            try {
                while (res.next()) {
                    listOperators.add(new DB2PlanOperator(context, res, this, planTableSchema));
                }
            } finally {
                res.close();
            }
        } finally {
            sqlStmt.close();
        }

        listStreams = new ArrayList<DB2PlanStream>();
        sqlStmt = context.prepareStatement(String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_STREAM"));
        try {
            setQueryParameters(sqlStmt);
            JDBCResultSet res = sqlStmt.executeQuery();
            try {
                while (res.next()) {
                    listStreams.add(new DB2PlanStream(res, this));
                }
            } finally {
                res.close();
            }
        } finally {
            sqlStmt.close();
        }
    }

    private void setQueryParameters(JDBCPreparedStatement sqlStmt) throws SQLException
    {
        sqlStmt.setString(1, explainRequester);
        sqlStmt.setTimestamp(2, explainTime);
        sqlStmt.setString(3, sourceName);
        sqlStmt.setString(4, sourceSchema);
        sqlStmt.setString(5, sourceVersion);
        sqlStmt.setString(6, explainLevel);
        sqlStmt.setInt(7, stmtNo);
        sqlStmt.setInt(8, sectNo);
    }

    // ----------------
    // Standard Getters
    // ----------------

    public DB2PlanInstance getPlanInstance()
    {
        return planInstance;
    }

    public String getExplainLevel()
    {
        return explainLevel;
    }

    public Integer getStmtNo()
    {
        return stmtNo;
    }

    public Integer getSectNo()
    {
        return sectNo;
    }

    public Double getTotalCost()
    {
        return totalCost;
    }

    public String getStatementText()
    {
        return statementText;
    }

    public Integer getQueryDegree()
    {
        return queryDegree;
    }

    public List<DB2PlanStream> getListStreams()
    {
        return listStreams;
    }

    public String getPlanTableSchema()
    {
        return planTableSchema;
    }

    public String getExplainRequester()
    {
        return explainRequester;
    }

    public Timestamp getExplainTime()
    {
        return explainTime;
    }

    public String getSourceName()
    {
        return sourceName;
    }

    public String getSourceSchema()
    {
        return sourceSchema;
    }

    public String getSourceVersion()
    {
        return sourceVersion;
    }

}
