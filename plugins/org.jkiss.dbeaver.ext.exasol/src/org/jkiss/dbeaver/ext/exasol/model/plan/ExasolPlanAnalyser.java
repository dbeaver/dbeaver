/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model.plan;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
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

/**
 * @author Karl
 */
public class ExasolPlanAnalyser extends AbstractExecutionPlan {

    private static final Log LOG = Log.getLog(ExasolPlanAnalyser.class);

    private ExasolDataSource dataSource;
    private String query;
    private List<ExasolPlanNode> rootNodes;

    public ExasolPlanAnalyser(ExasolDataSource dataSource, String query) {
        this.dataSource = dataSource;
        this.query = query;
    }

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public String getPlanQueryString() {
        return "/*snapshot execution*/ SELECT * FROM EXA_USER_PROFILE_LAST_DAY WHERE SESSION_ID = CURRENT_SESSION AND STMT_ID = (select max(stmt_id) from EXA_USER_PROFILE_LAST_DAY where sql_text = ?)";
    }

    @Override
    public List<? extends DBCPlanNode> getPlanNodes(Map<String, Object> options) {
        return rootNodes;
    }

    public void explain(DBCSession session)
        throws DBCException {
        rootNodes = new ArrayList<>();
        JDBCSession connection = (JDBCSession) session;
        boolean oldAutoCommit = false;
        try {
            oldAutoCommit = connection.getAutoCommit();
            if (oldAutoCommit)
                connection.setAutoCommit(false);

            //alter session
            JDBCUtils.executeSQL(connection, "ALTER SESSION SET PROFILE = 'ON'");

            //execute query
            JDBCUtils.executeSQL(connection, query);

            //alter session
            JDBCUtils.executeSQL(connection, "ALTER SESSION SET PROFILE = 'OFF'");

            //rollback in case of DML
            connection.rollback();

            //alter session
            JDBCUtils.executeSQL(connection, "FLUSH STATISTICS");
            connection.commit();

            //retrieve execute info
            try (JDBCPreparedStatement stmt = connection.prepareStatement(getPlanQueryString())) {
	            stmt.setString(1, query);
	            try (JDBCResultSet dbResult = stmt.executeQuery()) {
		            while (dbResult.next()) {
		                ExasolPlanNode node = new ExasolPlanNode(null, dbResult);
		                rootNodes.add(node);
		            }
	            }
            }

        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        } finally {

            //rollback changes because profile actually executes query and it could be INSERT/UPDATE
            try {
                connection.rollback();
                if (oldAutoCommit)
                    connection.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.error("Error closing plan analyser", e);
            }
        }
    }

    public ExasolDataSource getDataSource() {
        return this.dataSource;
    }


}
