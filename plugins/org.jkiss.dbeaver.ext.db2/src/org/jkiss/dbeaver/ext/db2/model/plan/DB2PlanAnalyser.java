/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DB2 execution plan analyser
 * 
 * @author Denis Forveille
 */
public class DB2PlanAnalyser implements DBCPlan {

    private static final Log LOG = Log.getLog(DB2PlanAnalyser.class);

    // See init below
    private static String PT_DELETE;
    private static final String PT_EXPLAIN = "EXPLAIN PLAN SET QUERYNO = %d FOR %s";
    private static final String SEL_STMT = "SELECT * FROM %s.EXPLAIN_STATEMENT WHERE QUERYNO = ? AND EXPLAIN_LEVEL = 'P' WITH UR";

    private static AtomicInteger STMT_NO_GEN = new AtomicInteger(Long.valueOf(System.currentTimeMillis() / 10000000L).intValue());

    private String query;
    private String planTableSchema;

    private Collection<DB2PlanNode> listNodes;
    private DB2PlanStatement db2PlanStatement;

    // ------------
    // Constructors
    // ------------

    public DB2PlanAnalyser(String query, String planTableSchema)
    {
        this.query = query;
        this.planTableSchema = planTableSchema;
    }

    // ----------------
    // Standard Getters
    // ----------------

    @Override
    public String getQueryString()
    {
        return query;
    }

    @Override
    public Collection<? extends DBCPlanNode> getPlanNodes()
    {
        return listNodes;
    }

    // ----------------
    // Business Methods
    // ----------------

    public void explain(JDBCSession session) throws DBCException
    {
        Integer stmtNo = STMT_NO_GEN.incrementAndGet();

        String explainStmt = String.format(PT_EXPLAIN, stmtNo, query);
        LOG.debug("Schema=" + planTableSchema + " : " + explainStmt);

        try {

            // Start by cleaning old rows for safety
            cleanExplainTables(session, stmtNo, planTableSchema);

            // Explain
            JDBCPreparedStatement dbStat = session.prepareStatement(String.format(PT_EXPLAIN, stmtNo, query));
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }

            // Build Node Structure
            dbStat = session.prepareStatement(String.format(SEL_STMT, planTableSchema));
            try {
                dbStat.setInt(1, stmtNo);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    dbResult.next();
                    db2PlanStatement = new DB2PlanStatement(session, dbResult, planTableSchema);
                }
            } finally {
                dbStat.close();
            }

            listNodes = db2PlanStatement.buildNodes();

            // Clean afterward
            cleanExplainTables(session, stmtNo, planTableSchema);

        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    // ----------------
    // Helpers
    // ----------------
    private void cleanExplainTables(JDBCSession session, Integer stmtNo, String planTableSchema) throws SQLException
    {
        // Delete previous statement rows
        try (JDBCPreparedStatement dbStat = session.prepareStatement(String.format(PT_DELETE, planTableSchema, planTableSchema))) {
            dbStat.setInt(1, stmtNo);
            dbStat.execute();
        }
    }

    static {
        StringBuilder sb = new StringBuilder(256);
        sb.append("DELETE");
        sb.append("  FROM %s.EXPLAIN_INSTANCE I");
        sb.append(" WHERE EXISTS (SELECT 1");
        sb.append("                 FROM %s.EXPLAIN_STATEMENT S");
        sb.append("                WHERE S.EXPLAIN_TIME = I.EXPLAIN_TIME");
        sb.append("                  AND S.SOURCE_NAME = I.SOURCE_NAME");
        sb.append("                  AND S.SOURCE_SCHEMA = I.SOURCE_SCHEMA");
        sb.append("                  AND S.SOURCE_VERSION = I.SOURCE_VERSION");
        sb.append("                  AND QUERYNO = ?)");
        PT_DELETE = sb.toString();
    }

}
