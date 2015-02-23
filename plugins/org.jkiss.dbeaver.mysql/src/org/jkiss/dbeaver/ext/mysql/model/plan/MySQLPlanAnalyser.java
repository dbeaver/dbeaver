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
package org.jkiss.dbeaver.ext.mysql.model.plan;

import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * MySQL execution plan analyser
 */
public class MySQLPlanAnalyser implements DBCPlan {

    private MySQLDataSource dataSource;
    private String query;
    private List<DBCPlanNode> rootNodes;

    public MySQLPlanAnalyser(MySQLDataSource dataSource, String query)
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
        String plainQuery = SQLUtils.stripComments((SQLDataSource) session.getDataSource(), query).toUpperCase();
        if (!plainQuery.startsWith("SELECT")) {
            throw new DBCException("Only SELECT statements could produce execution plan");
        }
        JDBCSession connection = (JDBCSession) session;
        try {
            JDBCPreparedStatement dbStat = connection.prepareStatement("EXPLAIN EXTENDED " + query);
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    rootNodes = new ArrayList<DBCPlanNode>();
                    while (dbResult.next()) {
                        MySQLPlanNode node = new MySQLPlanNode(null, dbResult);
                        rootNodes.add(node);
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
