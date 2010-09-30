/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

import java.sql.SQLException;
import java.util.Collection;

/**
 * MySQL execution plan analyser
 */
public class MySQLPlanAnalyser implements DBCPlan {

    private MySQLDataSource dataSource;
    private String query;

    public MySQLPlanAnalyser(MySQLDataSource dataSource, String query)
    {
        this.dataSource = dataSource;
        this.query = query;
    }

    public String getQueryString()
    {
        return query;
    }

    public Collection<DBCPlanNode> explain(DBCExecutionContext context)
        throws DBCException
    {
        String plainQuery = SQLUtils.stripComments(query).toUpperCase();
        if (!plainQuery.startsWith("SELECT")) {
            throw new DBCException("Only SELECT statements could produce execution plan");
        }
        JDBCExecutionContext connection = (JDBCExecutionContext)context;
        try {
            JDBCPreparedStatement dbStat = connection.prepareStatement("EXPLAIN EXTENDED " + query);
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        long id = dbResult.getLong("id");
                        String selectType = dbResult.getString("select_type");
                        String table = dbResult.getString("table");
                        String type = dbResult.getString("type");
                        String possibleKeys = dbResult.getString("possible_keys");
                        String key = dbResult.getString("key");
                        String keyLength = dbResult.getString("key_len");
                        String ref = dbResult.getString("ref");
                        long rowCount = dbResult.getLong("rows");
                        double filtered = dbResult.getDouble("filtered");
                        String extra = dbResult.getString("extra");
                    }
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBCException("Could not explain execution plan", e);
        }
        return null;
    }
}
