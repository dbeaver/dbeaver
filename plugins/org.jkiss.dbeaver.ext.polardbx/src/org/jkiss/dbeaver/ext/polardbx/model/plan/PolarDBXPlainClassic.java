/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0
 */
package org.jkiss.dbeaver.ext.polardbx.model.plan;

import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAbstract;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PolarDBXPlainClassic extends MySQLPlanAbstract {
    private List<PolarDBXPlanNodePlain> rootNodes;

    public PolarDBXPlainClassic(JDBCSession session, String query) throws DBCException {
        super((MySQLDataSource) session.getDataSource(), query);
        try (JDBCPreparedStatement dbStat = session.prepareStatement(getPlanQueryString())) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                List<PolarDBXPlanNodePlain> nodes = new ArrayList<>();
                while (dbResult.next()) {
                    PolarDBXPlanNodePlain node = new PolarDBXPlanNodePlain(null, dbResult);
                    nodes.add(node);
                }
                rootNodes = nodes;
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    public PolarDBXPlainClassic(MySQLDataSource dataSource, String query, List<PolarDBXPlanNodePlain> rootNodes) {
        super(dataSource, query);
        this.rootNodes = rootNodes;
    }

    @Override
    public Object getPlanFeature(String feature) {
        if (DBCPlanCostNode.FEATURE_PLAN_ROWS.equals(feature)) {
            return true;
        }
        return super.getPlanFeature(feature);
    }

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public String getPlanQueryString() {
        return "EXPLAIN FORMAT = \"brief\" " + query;
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options) {
        return rootNodes;
    }
}