/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model.plan;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.hana.model.HANADataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HANAPlanAnalyser extends AbstractExecutionPlan {

    private static final Log LOG = Log.getLog(HANAPlanAnalyser.class);

    private HANADataSource dataSource;
    private String query;
    private List<HANAPlanNode> rootNodes;

    public HANAPlanAnalyser(HANADataSource dataSource, String query) {
        this.dataSource = dataSource;
        this.query = query;
    }

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public String getPlanQueryString() {
        return "SELECT * FROM SYS.EXPLAIN_PLAN_TABLE";
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options) {
        return rootNodes;
    }

    public void explain(DBCSession session)  throws DBCException {
        rootNodes = new ArrayList<>();
        List<HANAPlanNode> allNodes = new ArrayList<>();
        JDBCSession connection = (JDBCSession) session;
        boolean oldAutoCommit = false;
        try {
            oldAutoCommit = connection.getAutoCommit();
            if (oldAutoCommit)
                connection.setAutoCommit(false);

            JDBCUtils.executeSQL(connection, "DELETE FROM SYS.EXPLAIN_PLAN_TABLE");
            JDBCUtils.executeSQL(connection, "EXPLAIN PLAN FOR "+query);

            try (JDBCPreparedStatement stmt = connection.prepareStatement(getPlanQueryString())) {
                try (JDBCResultSet dbResult = stmt.executeQuery()) {
                    while (dbResult.next()) {
                        HANAPlanNode node = new HANAPlanNode(dbResult);
                        for (HANAPlanNode parent : allNodes) {
                            if (node.getParentOperatorId() == parent.getOperatorId()) {
                                node.setParent(parent);
                                parent.addNested(node);
                            }
                        }
                        allNodes.add(node);
                    }
                }
            }
            connection.rollback();
            for (HANAPlanNode node : allNodes) {
                if (node.getParent() == null)
                    rootNodes.add(node);
            }

        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        } finally {
            //rollback temporary EXPLAIN_PLAN_TABLE content
            try {
                connection.rollback();
                if (oldAutoCommit)
                    connection.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.error("Error closing plan analyser", e);
            }
        }
    }

    public HANADataSource getDataSource() {
        return this.dataSource;
    }


}
