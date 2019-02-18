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
package org.jkiss.dbeaver.ext.postgresql.model.plan;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
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
public class PostgrePlanAnalyser extends AbstractExecutionPlan {

    private static final Log log = Log.getLog(PostgrePlanAnalyser.class);

    private boolean oldQuery;
    private String query;
    private List<DBCPlanNode> rootNodes;

    public PostgrePlanAnalyser(boolean oldQuery, String query)
    {
        this.oldQuery = oldQuery;
        this.query = query;
    }

    @Override
    public Object getPlanFeature(String feature) {
        if (DBCPlanCostNode.FEATURE_PLAN_COST.equalsIgnoreCase(feature) |
            DBCPlanCostNode.FEATURE_PLAN_DURATION.equalsIgnoreCase(feature))
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
            return "EXPLAIN VERBOSE " + query;
        } else {
            return "EXPLAIN (FORMAT XML, ANALYSE) " + query;
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
            try (JDBCPreparedStatement dbStat = connection.prepareStatement(getPlanQueryString())) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        SQLXML planXML = dbResult.getSQLXML(1);
                        parsePlan(session, planXML);
                    }
                } catch (XMLException e) {
                    throw new DBCException("Can't parse plan XML", e);
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
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

    private void parsePlan(DBCSession session, SQLXML planXML) throws SQLException, XMLException {
        rootNodes = new ArrayList<>();
        Document planDocument = XMLUtils.parseDocument(planXML.getBinaryStream());
        Element queryElement = XMLUtils.getChildElement(planDocument.getDocumentElement(), "Query");
        for (Element planElement : XMLUtils.getChildElementList(queryElement, "Plan")) {
            rootNodes.add(new PostgrePlanNode((PostgreDataSource) session.getDataSource(), null, planElement));
        }
    }

}
