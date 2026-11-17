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

import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanSourceFormat;
import org.jkiss.dbeaver.model.impl.local.CachedResultSet;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Map;

public class PolarDBXPlanClassicTest extends DBeaverUnitTest {
    @Test
    public void parsesIndentedTreeAndSkipsFooter() {
        var roots = PolarDBXPlanClassic.parsePlanNodes(List.of(
            "HashJoin(condition=\"a.id = b.id\")",
            "  Gather(concurrent=true)",
            "    LogicalView(tables=\"a\")",
            "  LogicalView(tables=\"b\")",
            "HitCache:false", "Source:PLAN_CACHE", "TemplateId: 1234", ""
        ));
        Assertions.assertEquals(1, roots.size());
        var root = roots.getFirst();
        Assertions.assertEquals("HashJoin", root.getNodeType());
        Assertions.assertEquals("HashJoin", root.getNodeName());
        var children = List.copyOf(root.getNested());
        Assertions.assertEquals(2, children.size());
        Assertions.assertSame(root, children.getFirst().getParent());
        Assertions.assertEquals(1, children.getFirst().getNested().size());
        Assertions.assertEquals("LogicalView", children.getLast().getNodeType());
        Assertions.assertNull(root.getNodeRowCount());
    }

    @Test
    public void preservesBareOperatorsAndMultipleRoots() {
        var roots = PolarDBXPlanClassic.parsePlanNodes(List.of(
            "LogicalView", "  MysqlTableScan(name=[test, t])", "Values(tuples=[{ 1 }])"
        ));
        Assertions.assertEquals(2, roots.size());
        Assertions.assertEquals(1, roots.getFirst().getNested().size());
        Assertions.assertNull(roots.getLast().getParent());
    }

    @Test
    public void preservesMultilineOperatorDetails() {
        String operator = "LogicalView(sql=\"SELECT 'first\nsecond'\")";
        var roots = PolarDBXPlanClassic.parsePlanNodes(List.of(operator));
        Assertions.assertEquals(1, roots.size());
        Assertions.assertEquals(operator, roots.getFirst().getOperatorInfo());
    }

    @Test
    public void cachesCompleteExplainResult() throws Exception {
        var session = Mockito.mock(JDBCSession.class);
        var statement = Mockito.mock(JDBCPreparedStatement.class);
        var result = Mockito.mock(JDBCResultSet.class);
        var metadata = Mockito.mock(ResultSetMetaData.class);
        Mockito.when(session.getDataSource()).thenReturn(Mockito.mock(MySQLDataSource.class));
        Mockito.when(session.prepareStatement("EXPLAIN SELECT 1")).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(result);
        Mockito.when(result.getMetaData()).thenReturn(metadata);
        Mockito.when(metadata.getColumnCount()).thenReturn(1);
        Mockito.when(metadata.getColumnName(1)).thenReturn("LOGICAL EXECUTIONPLAN");
        Mockito.when(result.next()).thenReturn(true, true, false);
        Mockito.when(result.getString(1)).thenReturn("Values(tuples=[{ 1 }])", "HitCache:false");
        Mockito.when(result.getObject(1)).thenReturn("Values(tuples=[{ 1 }])", "HitCache:false");

        var plan = new PolarDBXPlanClassic(session, "SELECT 1");
        Assertions.assertEquals(DBCPlanSourceFormat.RESULT_SET, plan.getPlanSourceDataFormat());
        var source = (CachedResultSet) plan.getPlanSourceData();
        Assertions.assertNotNull(source);
        Assertions.assertEquals(2, source.getRows().size());
        Assertions.assertEquals("HitCache:false", source.getRows().getLast()[0]);
        Assertions.assertEquals(1, plan.getPlanNodes(Map.of()).size());
        Mockito.verify(result).close();
        Mockito.verify(statement).close();
    }
}
