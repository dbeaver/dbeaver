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
package org.jkiss.dbeaver.ext.oracle.model.plan;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCSavedQueryPlanner;

/**
 * Oracle execution plan node
 */
public class OracleQueryPlanner implements DBCQueryPlanner/*, DBCSavedQueryPlanner*/ {

    private final OracleDataSource dataSource;

    public OracleQueryPlanner(OracleDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public OracleDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public DBCPlan planQueryExecution(DBCSession session, String query) throws DBException {
        OraclePlanAnalyser plan = new OraclePlanAnalyser(dataSource, (JDBCSession) session, query);
        plan.explain();
        return plan;
    }

/*
    @Override
    public DBCPlan readSavedQueryExecutionPlan(DBCSession session, Object savedQueryId) throws DBException {
        OraclePlanAnalyser plan = new OraclePlanAnalyser(dataSource, (JDBCSession) session, savedQueryId);
        plan.readHistoric();
        return plan;
    }
*/

    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }

}
