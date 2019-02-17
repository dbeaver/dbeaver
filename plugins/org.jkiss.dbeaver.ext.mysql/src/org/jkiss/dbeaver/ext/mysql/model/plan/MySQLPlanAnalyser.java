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
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * MySQL execution plan analyser
 */
public class MySQLPlanAnalyser extends AbstractExecutionPlan {

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
    public String getPlanQueryString() {
        return "EXPLAIN EXTENDED " + query;
    }

    @Override
    public Collection<DBCPlanNode> getPlanNodes()
    {
        return rootNodes;
    }

    public void explain(DBCSession session)
        throws DBCException
    {
        String plainQuery = SQLUtils.stripComments(SQLUtils.getDialectFromObject(session.getDataSource()), query).toUpperCase();
        if (!plainQuery.startsWith("SELECT")) {
            throw new DBCException("Only SELECT statements could produce execution plan");
        }
        JDBCSession connection = (JDBCSession) session;
        try {
            try (JDBCPreparedStatement dbStat = connection.prepareStatement(getPlanQueryString())) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<MySQLPlanNode> nodes = new ArrayList<>();
                    while (dbResult.next()) {
                        MySQLPlanNode node = new MySQLPlanNode(null, dbResult);
                        nodes.add(node);
                    }
                    MySQLPlanNode rootNode = new MySQLPlanNode(nodes);
                    rootNodes = new ArrayList<>();
                    rootNodes.add(rootNode);
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }
}
