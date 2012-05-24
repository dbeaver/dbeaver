/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model.plan;

import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
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

    public void explain(DBCExecutionContext context)
        throws DBCException
    {
        String plainQuery = SQLUtils.stripComments(context.getDataSource(), query).toUpperCase();
        if (!plainQuery.startsWith("SELECT")) {
            throw new DBCException("Only SELECT statements could produce execution plan");
        }
        JDBCExecutionContext connection = (JDBCExecutionContext)context;
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
            throw new DBCException(e);
        }
    }
}
