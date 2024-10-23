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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CubridPlanAnalyser extends AbstractExecutionPlan {

    private List<CubridPlanNode> rootNodes = new ArrayList<>();
    private String query;
    private String plan;

    public CubridPlanAnalyser(@NotNull JDBCSession session, @NotNull String query)
            throws DBCException {
        this.query = query;
        try {
            plan = CubridStatementProxy.getQueryplan(
                            session.getOriginal().createStatement(), query);
            rootNodes.add(new CubridPlanNode(plan));
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @NotNull
    @Override
    public List<? extends DBCPlanNode> getPlanNodes(@Nullable Map<String, Object> options) {
        return rootNodes;
    }

    @NotNull
    @Override
    public String getQueryString() {
        return query;
    }

    @NotNull
    @Override
    public String getPlanQueryString() {
        return plan;
    }
}
