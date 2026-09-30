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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAbstract;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAnalyser;
import org.jkiss.dbeaver.ext.polardbx.mysql.model.PolarDBXMySQLDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerialInfo;
import org.jkiss.dbeaver.model.impl.plan.ExecutionPlanDeserializer;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class PolarDBXPlanAnalyzer extends MySQLPlanAnalyser {
    private static final String SERIALIZATION_VERSION = "polardbx-classic";
    private static final String[] FIRST_KEYWORD_BLOCK_LIST = new String[]{
        "DESC", "SET", "EXPLAIN"
    };
    private final PolarDBXMySQLDataSource dataSource;

    public PolarDBXPlanAnalyzer(@NotNull PolarDBXMySQLDataSource dataSource) {
        super(dataSource);
        this.dataSource = dataSource;
    }

    private static boolean block(@Nullable String firstKeyword) {
        for (String blockWord : FIRST_KEYWORD_BLOCK_LIST) {
            if (blockWord.equalsIgnoreCase(firstKeyword)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public MySQLPlanAbstract explain(@NotNull JDBCSession session, @NotNull String query) throws DBCException {
        final SQLDialect dialect = SQLUtils.getDialectFromObject(this.dataSource);
        final String plainQuery = SQLUtils.stripComments(dialect, query).toUpperCase();
        final String firstKeyword = SQLUtils.getFirstKeyword(dialect, plainQuery);
        if (PolarDBXPlanAnalyzer.block(firstKeyword)) {
            throw new DBCException("This statement could not produce execution plan");
        }
        return new PolarDBXPlainClassic(session, query);
    }

    @Override
    public void serialize(@NotNull Writer writer, @NotNull DBCPlan plan) throws IOException {
        serializeJson(writer, plan, dataSource.getInfo().getDriverName(), new DBCQueryPlannerSerialInfo() {
            @NotNull
            @Override
            public String version() {
                return SERIALIZATION_VERSION;
            }

            @Override
            public void addNodeProperties(@NotNull DBCPlanNode node, @NotNull JsonObject nodeJson) {
                PolarDBXPlanNodePlain plainNode = (PolarDBXPlanNodePlain) node;
                JsonObject attributes = new JsonObject();
                attributes.add("id", new JsonPrimitive(CommonUtils.notEmpty(plainNode.getId())));
                attributes.add("estRows", new JsonPrimitive(CommonUtils.notEmpty(plainNode.getEstRows())));
                attributes.add("task", new JsonPrimitive(CommonUtils.notEmpty(plainNode.getTask())));
                attributes.add("accessObject", new JsonPrimitive(CommonUtils.notEmpty(plainNode.getAccessObject())));
                attributes.add("operatorInfo", new JsonPrimitive(CommonUtils.notEmpty(plainNode.getOperatorInfo())));
                nodeJson.add(PROP_ATTRIBUTES, attributes);
            }
        });
    }

    @NotNull
    @Override
    public DBCPlan deserialize(@NotNull Reader planData) throws IOException, InvocationTargetException {
        JsonObject plan = new JsonParser().parse(planData).getAsJsonObject();
        String version = getVersion(plan);
        if (!SERIALIZATION_VERSION.equals(version)) {
            throw new IOException("Unsupported PolarDB-X execution plan version: " + version);
        }

        ExecutionPlanDeserializer<PolarDBXPlanNodePlain> loader = new ExecutionPlanDeserializer<>();
        List<PolarDBXPlanNodePlain> rootNodes = loader.loadRoot(
            dataSource,
            plan,
            (source, node, parent) -> new PolarDBXPlanNodePlain(parent, getNodeAttributesAsStrings(node))
        );
        return new PolarDBXPlainClassic(dataSource, getQuery(plan), rootNodes);
    }
}
