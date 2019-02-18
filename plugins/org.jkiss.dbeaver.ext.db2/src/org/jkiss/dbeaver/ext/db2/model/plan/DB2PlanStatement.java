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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
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

    private static final Log LOG = Log.getLog(DB2PlanAnalyser.class);

    private static final String SEL_BASE_SELECT;

    static {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("SELECT *");
        sb.append("  FROM %s.%s");
        sb.append(" WHERE EXPLAIN_REQUESTER = ?"); // 1
        sb.append("   AND EXPLAIN_TIME = ?");// 2
        sb.append("   AND SOURCE_NAME = ?");// 3
        sb.append("   AND SOURCE_SCHEMA = ?");// 4
        sb.append("   AND SOURCE_VERSION = ?");// 5
        sb.append("   AND EXPLAIN_LEVEL = ?");// 6
        sb.append("   AND STMTNO = ?");// 7
        sb.append("   AND SECTNO = ?");// 8
        sb.append(" ORDER BY %s");
        sb.append(" WITH UR");
        SEL_BASE_SELECT = sb.toString();
    }

    private Map<String, DB2PlanOperator> mapOperators;
    private Map<String, DB2PlanObject> mapDataObjects;
    private List<DB2PlanStream> listStreams;

    private DB2PlanNode rootNode;

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
    public DB2PlanStatement(JDBCSession session, JDBCResultSet dbResult, String planTableSchema) throws SQLException
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

        loadChildren(session);
    }

    // ----------------
    // Business Methods
    // ----------------
    public List<DB2PlanNode> buildNodes()
    {
        // Based on streams, establish relationships between nodes
        // DF: Very Important!: The Stream MUST be order by STREAM_ID DESC for the viewer to display things right (from the list
        // order)

        DB2PlanNode sourceNode;
        DB2PlanNode targetNode;
        for (DB2PlanStream planStream : listStreams) {

            // LOG.debug(planStream.getStreamId() + " src=" + planStream.getSourceName() + " tgt=" + planStream.getTargetName());

            // DF: "Data Objects" may be "target" of "Explain" Streams and have multiple parents..
            // DBeaver Explain Plan Viewer shows nodes in parent-child hierarchy so a node can not have multiple "parents"
            // It seems reasonable to reverse the stream after cloning the Object because the same Data Object has multiple
            // parents

            // Get Source Node
            if (planStream.getSourceType().equals(DB2PlanNodeType.D)) {
                sourceNode = mapDataObjects.get(planStream.getSourceName());
                sourceNode = new DB2PlanObject((DB2PlanObject) sourceNode);
            } else {
                sourceNode = mapOperators.get(planStream.getSourceName());
            }

            // Get Target Node
            if (planStream.getTargetType().equals(DB2PlanNodeType.D)) {
                targetNode = mapDataObjects.get(planStream.getTargetName());
                targetNode = new DB2PlanObject((DB2PlanObject) targetNode);

                // Inverse target <-> source
                sourceNode.getNested().add(targetNode);
                targetNode.setParent(sourceNode);
            } else {
                targetNode = mapOperators.get(planStream.getTargetName());

                targetNode.getNested().add(sourceNode);
                targetNode.setEstimatedCardinality(planStream.getStreamCount());
                sourceNode.setParent(targetNode);
            }

        }

        // return rootNode == null ? Collections.<DB2PlanNode> emptyList() : Collections.singletonList(rootNode);
        return Collections.singletonList(rootNode);
    }

    // -------------
    // Load children
    // -------------
    private void loadChildren(JDBCSession session) throws SQLException
    {

        mapDataObjects = new HashMap<>(32);
        try (JDBCPreparedStatement sqlStmt = session.prepareStatement(String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_OBJECT",
            "OBJECT_SCHEMA,OBJECT_NAME")))
        {
            setQueryParameters(sqlStmt);
            try (JDBCResultSet res = sqlStmt.executeQuery()) {
                DB2PlanObject db2PlanObject;
                while (res.next()) {
                    db2PlanObject = new DB2PlanObject(res);
                    mapDataObjects.put(db2PlanObject.getNodeName(), db2PlanObject);
                }
            }
        }

        mapOperators = new HashMap<>(64);
        try (JDBCPreparedStatement sqlStmt = session.prepareStatement(
            String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_OPERATOR", "OPERATOR_ID")))
        {
            setQueryParameters(sqlStmt);
            try (JDBCResultSet res = sqlStmt.executeQuery()) {
                DB2PlanOperator db2PlanOperator;
                while (res.next()) {
                    db2PlanOperator = new DB2PlanOperator(session, res, this, planTableSchema);
                    mapOperators.put(db2PlanOperator.getNodeName(), db2PlanOperator);
                    if (db2PlanOperator.getOperatorType() == DB2PlanOperatorType.RETURN) {
                        rootNode = db2PlanOperator;
                    }
                }
            }
        }

        listStreams = new ArrayList<>();
        try (JDBCPreparedStatement sqlStmt = session.prepareStatement(
            String.format(SEL_BASE_SELECT, planTableSchema, "EXPLAIN_STREAM", "STREAM_ID DESC")))
        {
            setQueryParameters(sqlStmt);
            try (JDBCResultSet res = sqlStmt.executeQuery()) {
                while (res.next()) {
                    listStreams.add(new DB2PlanStream(res, this));
                }
            }
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
