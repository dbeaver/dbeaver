/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;

/**
 * Wires {@link MimerExecutionPlan} up to the SQL Editor's "Explain Execution Plan" action,
 * same registration mechanism H2 uses ({@code MimerMetaModel#getQueryPlanner}, adapted off
 * via {@code GenericDataSource.getAdapter(DBCQueryPlanner.class)}). {@code PLAN} style (not
 * {@code QUERY}) - the plan doesn't come from a runnable SQL string ({@link
 * MimerExecutionPlan#getPlanQueryString()} has nothing to give it), it comes from a driver API
 * call, so it has to go through the dedicated plan-tree viewer rather than "run this SQL text
 * as a query and show the results grid" (which is how {@code QUERY} style works, and is the
 * only style that would render {@link MimerExecutionPlan#getPlanSourceData()} without also
 * having a node tree - see {@code SQLEditor#explainPlanFromQuery}).
 *
 * @author Mimer Information Technology
 */
public class MimerQueryPlanner implements DBCQueryPlanner {

    private final MimerDataSource dataSource;

    public MimerQueryPlanner(@NotNull MimerDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public DBCPlan planQueryExecution(
        @NotNull DBCSession session,
        @NotNull String query,
        @NotNull DBCQueryPlannerConfiguration configuration
    ) throws DBException {
        MimerExecutionPlan plan = new MimerExecutionPlan(query);
        plan.explain(session);
        return plan;
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }
}
