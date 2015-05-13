/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.model.plan;

import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.utils.IntKeyMap;
import org.jkiss.utils.SecurityUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Oracle execution plan analyser
 */
public class OraclePlanAnalyser implements DBCPlan {

    private OracleDataSource dataSource;
    private String query;
    private List<OraclePlanNode> rootNodes;

    public OraclePlanAnalyser(OracleDataSource dataSource, String query)
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
    public Collection<OraclePlanNode> getPlanNodes()
    {
        return rootNodes;
    }

    public void explain(JDBCSession session)
        throws DBCException
    {
        String planStmtId = SecurityUtils.generateUniqueId();
        try {
            // Detect plan table
            String planTableName = dataSource.getPlanTableName(session);
            if (planTableName == null) {
                throw new DBCException("Plan table not found - query can't be explained");
            }

            // Delete previous statement rows
            // (actually there should be no statement with this id -
            // but let's do it, just in case)
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "DELETE FROM " + planTableName +
                " WHERE STATEMENT_ID=? ");
            try {
                dbStat.setString(1, planStmtId);
                dbStat.execute();
            } finally {
                dbStat.close();
            }

            // Explain plan
            StringBuilder explainSQL = new StringBuilder();
            explainSQL
                .append("EXPLAIN PLAN ").append("\n")
                .append("SET STATEMENT_ID = '").append(planStmtId).append("'\n")
                .append("INTO ").append(planTableName).append("\n")
                .append("FOR ").append(query);
            dbStat = session.prepareStatement(explainSQL.toString());
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }

            // Read explained plan
            dbStat = session.prepareStatement(
                "SELECT * FROM " + planTableName +
                " WHERE STATEMENT_ID=? ORDER BY ID");
            try {
                dbStat.setString(1, planStmtId);
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    rootNodes = new ArrayList<OraclePlanNode>();
                    IntKeyMap<OraclePlanNode> allNodes = new IntKeyMap<OraclePlanNode>();
                    while (dbResult.next()) {
                        OraclePlanNode node = new OraclePlanNode(dataSource, allNodes, dbResult);
                        allNodes.put(node.getId(), node);
                        if (node.getParent() == null) {
                            rootNodes.add(node);
                        }
                    }
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }

        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

}
