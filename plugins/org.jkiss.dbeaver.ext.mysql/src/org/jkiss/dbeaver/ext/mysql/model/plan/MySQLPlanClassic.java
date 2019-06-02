/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerialInfo;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.dbeaver.model.impl.plan.ExecutionPlanDeserializer;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MySQL execution plan analyser
 */
public class MySQLPlanClassic extends MySQLPlanAbstract implements DBCQueryPlannerSerialInfo {

    protected final static String FORMAT_VERSION = "classic.1";
    
    private List<MySQLPlanNodePlain> rootNodes;

    public MySQLPlanClassic(JDBCSession session, String query) throws DBCException {
        super((MySQLDataSource) session.getDataSource(), query);

        String plainQuery = SQLUtils.stripComments(SQLUtils.getDialectFromObject(dataSource), query).toUpperCase();
        if (!plainQuery.startsWith("SELECT")) {
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
            throw new DBCException(e, session.getDataSource());
        }
    }

    public MySQLPlanClassic(MySQLDataSource dataSource, String query, List<MySQLPlanNodePlain> rootNodes) {
        super((MySQLDataSource) dataSource, query);
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

    public void deserialize(MySQLDataSource dataSource, JsonObject jo) throws InvocationTargetException {

            this.query = jo.get(AbstractExecutionPlanSerializer.PROP_SQL).getAsString();

            ExecutionPlanDeserializer<MySQLPlanNodePlain> loader = new ExecutionPlanDeserializer<>();

            rootNodes = loader.loadRoot(dataSource, jo, (datasource, node, parent) -> {
                return new MySQLPlanNodePlain(parent, getNodeAttributes(node));
            });

    }

    @Override
    public String version() {
        return FORMAT_VERSION;
    }

    
    private JsonObject createAttr(String key,String value) {
        JsonObject attr = new JsonObject();
        attr.add(key,new JsonPrimitive(CommonUtils.notEmpty(value)));
        return attr; 
    }

    private JsonObject createAttr(String key,long value) {
        JsonObject attr = new JsonObject();
        attr.add(key,new JsonPrimitive(value));
        return attr; 
    }

    @Override
    public void addNodeProperties(DBCPlanNode node, JsonObject nodeJson) {
        JsonArray attributes = new JsonArray();
        if (node instanceof MySQLPlanNodePlain) {
            MySQLPlanNodePlain plainNode = (MySQLPlanNodePlain) node;
            attributes.add(createAttr("id", plainNode.getId()));
            attributes.add(createAttr("select_type", plainNode.getSelectType()));
            attributes.add(createAttr("table", plainNode.getTable()));
            attributes.add(createAttr("type", plainNode.getNodeType()));
            attributes.add(createAttr("possible_keys", plainNode.getPossibleKeys()));
            attributes.add(createAttr("key", plainNode.getKey()));
            attributes.add(createAttr("key_len", plainNode.getKeyLength()));
            attributes.add(createAttr("ref", plainNode.getRef()));
            attributes.add(createAttr("rows", plainNode.getRowCount()));
            attributes.add(createAttr("filtered", plainNode.getFiltered()));
            attributes.add(createAttr("extra", plainNode.getExtra()));
         }
        nodeJson.add(AbstractExecutionPlanSerializer.PROP_ATTRIBUTES, attributes);
    }

}
