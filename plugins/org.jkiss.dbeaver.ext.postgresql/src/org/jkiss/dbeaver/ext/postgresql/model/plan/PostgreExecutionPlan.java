/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.plan;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Postgre execution plan analyser
 */
public class PostgreExecutionPlan extends AbstractExecutionPlan {

    private static final Log log = Log.getLog(PostgreExecutionPlan.class);

    private static final String NODE_PREFIX = "->  ";
    private static final String PROP_PREFIX = "  ";

    private boolean oldQuery;
    private boolean verbose;
    private String query;
    private DBCQueryPlannerConfiguration configuration;
    private List<DBCPlanNode> rootNodes;

    public PostgreExecutionPlan(boolean oldQuery, boolean verbose, String query, DBCQueryPlannerConfiguration configuration)
    {
        this.oldQuery = oldQuery;
        this.verbose = verbose;
        this.query = query;
        this.configuration = configuration;
    }

    public PostgreExecutionPlan(String query, List<PostgrePlanNodeExternal> nodes) {
        this.query = query;
        this.rootNodes = new ArrayList<>();
        this.rootNodes.addAll(nodes);
        this.configuration = new DBCQueryPlannerConfiguration();
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
        if (oldQuery) {
            return "EXPLAIN " + (verbose ? "VERBOSE " : "") + query;
        } else {
            boolean doAnalyze = CommonUtils.toBoolean(this.configuration.getParameters().get(PostgreQueryPlaner.PARAM_ANALYSE));
            return "EXPLAIN (FORMAT XML" + (doAnalyze ? ", ANALYSE" : "") + ") " + query;
        }
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options)
    {
        return rootNodes;
    }

    public void explain(DBCSession session)
        throws DBCException
    {
        JDBCSession connection = (JDBCSession) session;
        boolean oldAutoCommit = false;
        try {
            oldAutoCommit = connection.getAutoCommit();
            if (oldAutoCommit) {
                connection.setAutoCommit(false);
            }
            try (JDBCStatement dbStat = connection.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery(getPlanQueryString())) {
                    if (oldQuery) {
                        List<String> planLines = new ArrayList<>();
                        while (dbResult.next()) {
                            String planLine = dbResult.getString(1);
                            if (!CommonUtils.isEmpty(planLine)) {
                                planLines.add(planLine);
                            }
                        }
                        parsePlanText(session, planLines);
                    } else {
                        if (dbResult.next()) {
                            SQLXML planXML = dbResult.getSQLXML(1);
                            parsePlanXML(session, planXML);
                        }
                    }
                } catch (XMLException e) {
                    throw new DBCException("Can't parse plan XML", e);
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        } finally {
            // Rollback changes because EXPLAIN actually executes query and it could be INSERT/UPDATE
            try {
                connection.rollback();
                if (oldAutoCommit) {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error("Error closing plan analyser", e);
            }
        }
    }

    private void parsePlanXML(DBCSession session, SQLXML planXML) throws SQLException, XMLException {
        rootNodes = new ArrayList<>();
        Document planDocument = XMLUtils.parseDocument(planXML.getBinaryStream());
        Element queryElement = XMLUtils.getChildElement(planDocument.getDocumentElement(), "Query");
        for (Element planElement : XMLUtils.getChildElementList(queryElement, "Plan")) {
            rootNodes.add(new PostgrePlanNodeXML((PostgreDataSource) session.getDataSource(), null, planElement));
        }
    }

    private void parsePlanText(DBCSession session, List<String> lines) {
        PostgreDataSource dataSource = (PostgreDataSource) session.getDataSource();
        List<PostgrePlanNodeText> nodes = new ArrayList<>(lines.size());
        PostgrePlanNodeText rootNode = null, curNode = null, curParentNode = null;
        int curIndent = 0;
        for (String line : lines) {
            int lineIndent = 0;
            for (int i = lineIndent; i < line.length(); i++) {
                if (line.charAt(i) != ' ') {
                    break;
                }
                lineIndent++;
            }
            if (curIndent == 0 && lineIndent == 0) {
                // Root node
                curParentNode = curNode = rootNode = new PostgrePlanNodeText(dataSource, null, line, lineIndent);
                nodes.add(rootNode);
            } else if (lineIndent >= curIndent) {
                // Child node
                if (line.substring(lineIndent).startsWith(NODE_PREFIX)) {
                    //log.debug("New child " + line);
                    if (lineIndent > curNode.getIndent()) {
                        curParentNode = curNode;  
                    }
                    lineIndent += NODE_PREFIX.length();
                    curNode = new PostgrePlanNodeText(dataSource, curParentNode, line, lineIndent);
                    nodes.add(0,curNode);
                } else if (lineIndent == curIndent || lineIndent == curIndent) {
                    // Property
                    curNode.addProp(line);
                    continue;
                } else {
                    if (curNode != null) {
                        curNode.addProp(line);
                    } else {
                        log.debug("Unexpected node line: " + line);
                    }
                    continue;
                }
                curIndent = lineIndent;
            } else if (lineIndent < curIndent) {
                 if (lineIndent == 0) {
                    // Trailing plan statuses
                } else {
                     if (lineIndent + NODE_PREFIX.length() < curNode.getIndent()) {
                        //need find upper parent
                        curParentNode =  rootNode;
                        for(int i = 0;i<nodes.size();i++) {
                            if(nodes.get(i).getIndent() == lineIndent - 2) {
                                curParentNode = nodes.get(i);
                                break;
                            }
                        }
                    }
                    if (!line.substring(lineIndent).startsWith(NODE_PREFIX)) {
                        curNode.addProp(line);
                    } else {
                        lineIndent += NODE_PREFIX.length();
                        curNode = new PostgrePlanNodeText(dataSource, curParentNode, line, lineIndent);
                        nodes.add(0,curNode);
                    }
                }
                curIndent = lineIndent;
            }
        }
        rootNodes = new ArrayList<>();
        rootNodes.add(rootNode);
    }

}
