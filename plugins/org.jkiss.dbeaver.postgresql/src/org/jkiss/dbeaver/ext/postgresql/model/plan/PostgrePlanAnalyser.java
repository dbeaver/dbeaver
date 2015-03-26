/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.postgresql.model.plan;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
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

    static final Log log = Log.getLog(PostgrePlanAnalyser.class);

    private GenericDataSource dataSource;
    private String query;
    private List<DBCPlanNode> rootNodes;

    public PostgrePlanAnalyser(GenericDataSource dataSource, String query)
    {
        this.dataSource = dataSource;
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
            JDBCPreparedStatement dbStat = connection.prepareStatement("EXPLAIN (FORMAT XML, ANALYSE) " + query);
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    if (dbResult.next()) {
                        SQLXML planXML = dbResult.getSQLXML(1);
                        parsePlan(planXML);
                    }
                } catch (XMLException e) {
                    throw new DBCException("Can't parse plan XML", e);
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
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
        rootNodes = new ArrayList<DBCPlanNode>();
        Document planDocument = XMLUtils.parseDocument(planXML.getBinaryStream());
        Element queryElement = XMLUtils.getChildElement(planDocument.getDocumentElement(), "Query");
        for (Element planElement : XMLUtils.getChildElementList(queryElement, "Plan")) {
            rootNodes.add(new PostgrePlanNode(null, planElement));
        }
    }

}
