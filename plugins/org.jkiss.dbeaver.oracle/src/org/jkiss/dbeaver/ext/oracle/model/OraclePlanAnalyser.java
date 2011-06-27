/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
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

    public static final String PLAN_TABLE_DEFINITION =
        "create global temporary table PLAN_TABLE (\n" +
            "statement_id varchar2(30),\n" +
            "plan_id number,\n" +
            "timestamp date,\n" +
            "remarks varchar2(4000),\n" +
            "operation varchar2(30),\n" +
            "options varchar2(255),\n" +
            "object_node varchar2(128),\n" +
            "object_owner varchar2(30),\n" +
            "object_name varchar2(30),\n" +
            "object_alias varchar2(65),\n" +
            "object_instance numeric,\n" +
            "object_type varchar2(30),\n" +
            "optimizer varchar2(255),\n" +
            "search_columns number,\n" +
            "id numeric,\n" +
            "parent_id numeric,\n" +
            "depth numeric,\n" +
            "position numeric,\n" +
            "cost numeric,\n" +
            "cardinality numeric,\n" +
            "bytes numeric,\n" +
            "other_tag varchar2(255),\n" +
            "partition_start varchar2(255),\n" +
            "partition_stop varchar2(255),\n" +
            "partition_id numeric,\n" +
            "other long,\n" +
            "distribution varchar2(30),\n" +
            "cpu_cost numeric,\n" +
            "io_cost numeric,\n" +
            "temp_space numeric,\n" +
            "access_predicates varchar2(4000),\n" +
            "filter_predicates varchar2(4000),\n" +
            "projection varchar2(4000),\n" +
            "time numeric,\n" +
            "qblock_name varchar2(30),\n" +
            "other_xml clob\n" +
            ") on commit preserve rows;";

    private OracleDataSource dataSource;
    private String usePlanTable;
    private String query;
    private List<OraclePlanNode> rootNodes;

    public OraclePlanAnalyser(OracleDataSource dataSource, String query)
    {
        this.dataSource = dataSource;
        this.query = query;
    }

    public String getQueryString()
    {
        return query;
    }

    public Collection<OraclePlanNode> getPlanNodes()
    {
        return rootNodes;
    }

    public void explain(JDBCExecutionContext context)
        throws DBCException
    {
        if (!query.trim().toUpperCase().startsWith("SELECT")) {
            throw new DBCException("Only SELECT statements could produce execution plan");
        }

        String planStmtId = SecurityUtils.generateUniqueId();
        try {
            // Detect plan table
            String planTableName = detectPlanTable(context);
            if (planTableName == null) {
                throw new DBCException("Plan table not found - query can't be explained");
            }

            // Delete previous statement rows
            // (actually there should be no statement with this id -
            // but let's do it, just in case)
            JDBCPreparedStatement dbStat = context.prepareStatement(
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
            dbStat = context.prepareStatement(explainSQL.toString());
            try {
                dbStat.execute();
            } finally {
                dbStat.close();
            }

            // Read explained plan
            dbStat = context.prepareStatement(
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
            throw new DBCException(e);
        }
    }

    private String detectPlanTable(JDBCExecutionContext context) throws SQLException
    {
        if (usePlanTable == null) {
            String[] candidateNames = new String[] {"PLAN_TABLE", "TOAD_PLAN_TABLE"};
            for (String candidate : candidateNames) {
                try {
                    JDBCUtils.executeSQL(context, "SELECT 1 FROM " + candidate);
                } catch (SQLException e) {
                    // No such table
                    continue;
                }
                usePlanTable = candidate;
                break;
            }
            if (usePlanTable == null) {
                PlanTableCreator planTableCreator = new PlanTableCreator();
                Display.getDefault().syncExec(planTableCreator);
                if (!planTableCreator.confirmed) {
                    return null;
                }
                usePlanTable = createPlanTable(context);
            }
        }
        return usePlanTable;
    }

    private String createPlanTable(JDBCExecutionContext context) throws SQLException
    {
        JDBCUtils.executeSQL(context, PLAN_TABLE_DEFINITION);
        return "PLAN_TABLE";
    }

    private static class PlanTableCreator implements Runnable {
        boolean confirmed = false;

        public void run()
        {
            // Plan table not found - try to create new one
            MessageDialog dialog = new MessageDialog(
                DBeaverCore.getActiveWorkbenchShell(),
                "Oracle PLAN_TABLE missing",
                null,
                "PLAN_TABLE not found in current user's context. " +
                "Do you want DBeaver to create new PLAN_TABLE?",
                MessageDialog.QUESTION,
                new String[] {
                    IDialogConstants.YES_LABEL,
                    IDialogConstants.NO_LABEL},
                0
            );
            confirmed = (dialog.open() == IDialogConstants.YES_ID);
        }
    }

}
