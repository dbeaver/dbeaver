/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cubrid.model.CubridDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.util.List;

public class CubridQueryPlanner implements DBCQueryPlanner
{

    private CubridDataSource dataSource;

    public CubridQueryPlanner(@NotNull CubridDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CubridPlanAnalyser explain(@NotNull JDBCSession session, @NotNull String query)
            throws DBCException {
        final SQLDialect dialect = SQLUtils.getDialectFromObject(dataSource);
        final String plainQuery = SQLUtils.stripComments(dialect, query).toUpperCase();
        final String firstKeyword = SQLUtils.getFirstKeyword(dialect, plainQuery);
        List<String> supportStatement = List.of("SELECT", "WITH", "PREPARE", "VALUES");
        if (!supportStatement.contains(firstKeyword.toUpperCase())) {
            throw new DBCException("Only SELECT, WITH, and PREPARE statements could produce execution plan");
        }
        return new CubridPlanAnalyser(session, query);
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public DBCPlan planQueryExecution(
            @NotNull DBCSession session,
            @NotNull String query,
            @NotNull DBCQueryPlannerConfiguration configuration)
            throws DBCException {
        return explain((JDBCSession) session, query);
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }
}