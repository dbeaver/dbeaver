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
package org.jkiss.dbeaver.ext.firebird.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.firebird.FireBirdUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanSourceFormat;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Build firebird plan tree based on textual plan information returned by getPlan.
 *
 * @author tomashorak@post.cz
 */
public class FireBirdPlanAnalyser extends AbstractExecutionPlan {
    private JDBCSession session;
    private String query;
    private List<FireBirdPlanNode> rootNodes;
    private String planText;

    public FireBirdPlanAnalyser(JDBCSession session, String query) {
        this.session = session;
        this.query = query;
    }

    public void explain() throws DBException {
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement(getQueryString());
            // Read explained plan
            try {
                planText = FireBirdUtils.getPlan(dbStat);
                FireBirdPlanBuilder builder = new FireBirdPlanBuilder(planText);
                rootNodes = builder.Build(session);
            } finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @NotNull
    @Override
    public String getQueryString() {
        return query;
    }

    @NotNull
    @Override
    public String getPlanQueryString() throws DBException {
        return "";
    }

    @NotNull
    @Override
    public DBCPlanSourceFormat getPlanSourceDataFormat() {
        return DBCPlanSourceFormat.TEXT;
    }

    @Nullable
    @Override
    public Object getPlanSourceData() {
        return planText;
    }

    @NotNull
    @Override
    public List<? extends DBCPlanNode> getPlanNodes(@NotNull Map<String, Object> options) {
        return rootNodes;
    }

}
