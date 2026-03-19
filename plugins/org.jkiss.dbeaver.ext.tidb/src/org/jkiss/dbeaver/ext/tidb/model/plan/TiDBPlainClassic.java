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

package org.jkiss.dbeaver.ext.tidb.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAbstract;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanSourceFormat;
import org.jkiss.dbeaver.model.impl.local.CachedResultSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TiDBPlainClassic extends MySQLPlanAbstract {
    private final List<TiDBPlanNodePlain> rootNodes;
    private CachedResultSet planResults;

    public TiDBPlainClassic(JDBCSession session, String query) throws DBCException {
        super((MySQLDataSource) session.getDataSource(), query);
        String planQueryString = getPlanQueryString();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(planQueryString)) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                planResults = new CachedResultSet(planQueryString, dbResult.getMetaData());
                List<TiDBPlanNodePlain> nodes = new ArrayList<>();
                while (dbResult.next()) {
                    TiDBPlanNodePlain node = new TiDBPlanNodePlain(null, dbResult);
                    nodes.add(node);
                    planResults.addRow(dbResult);
                }

                rootNodes = nodes;
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    public TiDBPlainClassic(MySQLDataSource dataSource, String query, List<TiDBPlanNodePlain> rootNodes) {
        super(dataSource, query);
        this.rootNodes = rootNodes;
    }

    @Nullable
    @Override
    public Object getPlanFeature(@NotNull String feature) {
        if (DBCPlanCostNode.FEATURE_PLAN_ROWS.equals(feature)) {
            return true;
        }
        return super.getPlanFeature(feature);
    }

    @NotNull
    @Override
    public String getQueryString() {
        return query;
    }

    @NotNull
    @Override
    public String getPlanQueryString() {
        return "EXPLAIN FORMAT = \"brief\" " + query;
    }

    @NotNull
    @Override
    public DBCPlanSourceFormat getPlanSourceDataFormat() {
        return DBCPlanSourceFormat.RESULT_SET;
    }

    @Nullable
    @Override
    public Object getPlanSourceData() {
        return planResults;
    }

    @NotNull
    @Override
    public List<? extends DBCPlanNode> getPlanNodes(@NotNull Map<String, Object> options) {
        return rootNodes;
    }
}
