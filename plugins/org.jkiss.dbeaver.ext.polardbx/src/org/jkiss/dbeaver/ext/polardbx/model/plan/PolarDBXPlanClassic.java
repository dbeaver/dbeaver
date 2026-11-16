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
package org.jkiss.dbeaver.ext.polardbx.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAbstract;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanSourceFormat;
import org.jkiss.dbeaver.model.impl.local.CachedResultSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PolarDBXPlanClassic extends MySQLPlanAbstract {
    private static final Pattern OPERATOR_LINE = Pattern.compile("[\\w$]+(?:\\(.*)?", Pattern.DOTALL);
    private final List<PolarDBXPlanNodePlain> rootNodes;
    private CachedResultSet rawResultSet;

    public PolarDBXPlanClassic(@NotNull JDBCSession session, @NotNull String query) throws DBCException {
        super((MySQLDataSource) session.getDataSource(), query);
        try (JDBCPreparedStatement dbStat = session.prepareStatement(getPlanQueryString())) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                rawResultSet = new CachedResultSet(getPlanQueryString(), dbResult.getMetaData());
                List<String> lines = new ArrayList<>();
                while (dbResult.next()) {
                    rawResultSet.addRow(dbResult);
                    String line = dbResult.getString(1);
                    if (line != null) {
                        lines.add(line);
                    }
                }
                rootNodes = parsePlanNodes(lines);
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    public PolarDBXPlanClassic(
        @NotNull MySQLDataSource dataSource,
        @NotNull String query,
        @NotNull List<PolarDBXPlanNodePlain> rootNodes
    ) {
        super(dataSource, query);
        this.rootNodes = rootNodes;
    }

    @NotNull
    static List<PolarDBXPlanNodePlain> parsePlanNodes(@NotNull List<String> lines) {
        List<PolarDBXPlanNodePlain> roots = new ArrayList<>();
        List<PolarDBXPlanNodePlain> ancestors = new ArrayList<>();
        List<Integer> indents = new ArrayList<>();
        // CN returns an indented, single-column plan, not TiDB's FORMAT="brief" columns.
        for (String line : lines) {
            String operator = line.strip();
            if (!OPERATOR_LINE.matcher(operator).matches()) {
                // Keep footer metadata (HitCache, Source, TemplateId, etc.) in the raw result only.
                continue;
            }
            int indent = line.length() - line.stripLeading().length();
            while (!indents.isEmpty() && indents.getLast() >= indent) {
                indents.removeLast();
                ancestors.removeLast();
            }
            PolarDBXPlanNodePlain node = new PolarDBXPlanNodePlain(operator);
            if (ancestors.isEmpty()) {
                roots.add(node);
            } else {
                node.setParent(ancestors.getLast());
            }
            ancestors.add(node);
            indents.add(indent);
        }
        return roots;
    }

    @NotNull
    @Override
    public DBCPlanSourceFormat getPlanSourceDataFormat() {
        return DBCPlanSourceFormat.RESULT_SET;
    }

    @Nullable
    @Override
    public Object getPlanSourceData() {
        return rawResultSet;
    }

    @NotNull
    @Override
    public String getQueryString() {
        return query;
    }

    @NotNull
    @Override
    public String getPlanQueryString() {
        return "EXPLAIN " + query;
    }

    @NotNull
    @Override
    public List<? extends DBCPlanNode> getPlanNodes(@NotNull Map<String, Object> options) {
        return rootNodes;
    }
}
