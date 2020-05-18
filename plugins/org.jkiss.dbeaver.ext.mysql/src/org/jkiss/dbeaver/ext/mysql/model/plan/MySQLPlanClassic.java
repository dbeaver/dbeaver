/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.model.plan;

import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MySQL execution plan analyser
 */
public class MySQLPlanClassic extends MySQLPlanAbstract {

    private List<MySQLPlanNodePlain> rootNodes;

    public MySQLPlanClassic(JDBCSession session, String query) throws DBCException {
        super((MySQLDataSource) session.getDataSource(), query);

        SQLDialect dialect = SQLUtils.getDialectFromObject(dataSource);
        String plainQuery = SQLUtils.stripComments(dialect, query).toUpperCase();
        if (!"SELECT".equalsIgnoreCase(SQLUtils.getFirstKeyword(dialect, plainQuery))) {
            throw new DBCException("Only SELECT statements could produce execution plan");
        }
        try (JDBCPreparedStatement dbStat = session.prepareStatement(getPlanQueryString())) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                List<MySQLPlanNodePlain> nodes = new ArrayList<>();
                while (dbResult.next()) {
                    MySQLPlanNodePlain node = new MySQLPlanNodePlain(null, dbResult);
                    nodes.add(node);
                }

                rootNodes = nodes;
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    public MySQLPlanClassic(MySQLDataSource dataSource, String query, List<MySQLPlanNodePlain> rootNodes) {
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
        return "EXPLAIN EXTENDED " + query;
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options) {
        return rootNodes;
    }

    private List<MySQLPlanNodePlain> convertToPlanTree(List<MySQLPlanNodePlain> srcNodes) {
        List<MySQLPlanNodePlain> roots = new ArrayList<>();

        if (srcNodes.size() == 1) {
            // Just one node
            roots.add(srcNodes.get(0));
        } else {
            List<MySQLPlanNodePlain> parsed = new ArrayList<>();
            MySQLPlanNodePlain lastCompositeNode = null;
            for (int id = 1; ; id++) {
                List<MySQLPlanNodePlain> nodes = getQueriesById(srcNodes, id);
                if (nodes.isEmpty()) {
                    break;
                }
                if (nodes.size() == 1) {
                    MySQLPlanNodePlain firstNode = nodes.get(0);
                    if (lastCompositeNode != null) {
                        firstNode.setParent(lastCompositeNode);
                    } else {
                        roots.add(firstNode);
                    }
                    if (firstNode.isCompositeNode()) {
                        lastCompositeNode = firstNode;
                    }
                } else {
                    MySQLPlanNodePlain leftNode = lastCompositeNode;
                    if (leftNode == null) {
                        leftNode = nodes.get(0).getParent();
                    }
                    MySQLPlanNodePlain joinNode = joinNodes(srcNodes, leftNode, nodes);
                    if (leftNode == null) {
                        roots.add(joinNode);
                    }
                }
                parsed.addAll(nodes);
            }
            // Add the rest
            for (MySQLPlanNodePlain node : srcNodes) {
                if (node.getId() != null && !parsed.contains(node)) {
                    roots.add(node);
                }
            }
        }

        for (MySQLPlanNodePlain node : roots) {
            node.computeStats();
        }

        return roots;
    }

    private List<MySQLPlanNodePlain> getQueriesById(List<MySQLPlanNodePlain> srcNodes, int id) {
        List<MySQLPlanNodePlain> subList = new ArrayList<>();
        for (MySQLPlanNodePlain node : srcNodes) {
            if (node.getId() != null && node.getId() == id) {
                subList.add(node);
            }
        }
        return subList;
    }

    private MySQLPlanNodePlain joinNodes(List<MySQLPlanNodePlain> srcNodes, MySQLPlanNodePlain parent, List<MySQLPlanNodePlain> nodes) {
        MySQLPlanNodePlain leftNode = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            MySQLPlanNodePlain rightNode = nodes.get(i);
            MySQLPlanNodeJoin nodeJoin = new MySQLPlanNodeJoin(parent, leftNode, rightNode);
            leftNode.setParent(nodeJoin);
            rightNode.setParent(nodeJoin);
            if (parent != null) {
                nodeJoin.setParent(parent);
            }
            leftNode = nodeJoin;
        }
        return leftNode;
    }

}
