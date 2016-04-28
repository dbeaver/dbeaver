/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model.plan;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Postgre execution plan analyser
 */
public class PostgrePlanAnalyser implements DBCPlan {

    private static final Log log = Log.getLog(PostgrePlanAnalyser.class);

    private String query;
    private List<DBCPlanNode> rootNodes;

    public PostgrePlanAnalyser(String query)
    {
        this.query = query;
    }

    @Override
    public String getQueryString()
    {
        return query;
    }

    @Override
    public Collection<DBCPlanNode> getPlanNodes()
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
            try (JDBCPreparedStatement dbStat = connection.prepareStatement("EXPLAIN (FORMAT XML, ANALYSE) " + query)) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        SQLXML planXML = dbResult.getSQLXML(1);
                        parsePlan(planXML);
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

    private void parsePlan(SQLXML planXML) throws SQLException, XMLException {
        rootNodes = new ArrayList<>();
        Document planDocument = XMLUtils.parseDocument(planXML.getBinaryStream());
        Element queryElement = XMLUtils.getChildElement(planDocument.getDocumentElement(), "Query");
        for (Element planElement : XMLUtils.getChildElementList(queryElement, "Plan")) {
            rootNodes.add(new PostgrePlanNode(null, planElement));
        }
    }

}
