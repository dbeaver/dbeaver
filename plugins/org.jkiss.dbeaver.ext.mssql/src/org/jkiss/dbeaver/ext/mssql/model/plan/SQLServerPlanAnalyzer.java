/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.mssql.model.plan;

import java.io.FileWriter;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;

public class SQLServerPlanAnalyzer extends AbstractExecutionPlan {
    
    private static final String TURN_PLAN_ON = "SET STATISTICS XML ON";
    
    private String query;
    private List<DBCPlanNode> nodes;
    
    private static final Log log = Log.getLog(SQLServerPlanAnalyzer.class);

     public SQLServerPlanAnalyzer(String query) {
        this.query = query;
    }

    @Override
    public String getQueryString() {
        return query;
    }

     @Override
    public String getPlanQueryString() throws DBException {
        return query;
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options) {
        return nodes;
    }
    
    public void explain(DBCSession session)      throws DBCException
    {
        JDBCSession connection = (JDBCSession) session;
        boolean oldAutoCommit = false;
        try {
            oldAutoCommit = connection.getAutoCommit();
            if (oldAutoCommit) {
                connection.setAutoCommit(false);
            }
            try (JDBCStatement dbStat = connection.createStatement()) {
                dbStat.execute(TURN_PLAN_ON);
                try (JDBCResultSet dbResult = dbStat.executeQuery(query)) {
                    
                    if (dbStat.getMoreResults()) {
                        
                        try (JDBCResultSet planResult = dbStat.getResultSet()) {
                            if (planResult.next()) {
                               nodes =  SQLServerPlanParser.getInstance().parse(planResult.getString(1),query);
                            } else {
                                throw new DBCException("Query plan not available"); 
                            }
                            
                         }
                         
                    } else {
                        throw new DBCException("Query plan not supported"); 
                    }
                } catch (Exception e) {
                    throw new DBCException("Can't parse plan XML", e);
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        } finally {
            // Rollback changes because EXPLAIN actually executes query and it could be INSERT/UPDATE
            try {
                connection.rollback();
                if (oldAutoCommit) {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error("Error closing plan analyser", e);
            }
        }        
        
    }

}
