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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireBirdDependencyTest extends DBeaverUnitTest {

    @Test
    public void dependentsQueryFiltersOnDependedOnSide() {
        String query = FireBirdDependency.getDependenciesQuery(FireBirdDependency.RELATION_TYPES, true);

        Assertions.assertTrue(query.startsWith("SELECT D.RDB$DEPENDENT_NAME AS OBJECT_NAME,D.RDB$DEPENDENT_TYPE AS OBJECT_TYPE"), query);
        Assertions.assertTrue(query.contains("WHERE D.RDB$DEPENDED_ON_NAME=? AND D.RDB$DEPENDED_ON_TYPE IN (0,1)"), query);
    }

    @Test
    public void dependenciesQueryFiltersOnDependentSide() {
        String query = FireBirdDependency.getDependenciesQuery(new int[] {FireBirdDependency.TYPE_TRIGGER}, false);

        Assertions.assertTrue(query.startsWith("SELECT D.RDB$DEPENDED_ON_NAME AS OBJECT_NAME,D.RDB$DEPENDED_ON_TYPE AS OBJECT_TYPE"), query);
        Assertions.assertTrue(query.contains("WHERE D.RDB$DEPENDENT_NAME=? AND D.RDB$DEPENDENT_TYPE IN (2)"), query);
    }

    @Test
    public void internalObjectsAreShownAsTheirOwnersAndColumnsAreCollected() throws Exception {
        // Rows as Firebird 5 returns them for the objects that use table T1
        List<Map<String, Object>> rows = List.of(
            row("CHECK_1", FireBirdDependency.TYPE_TRIGGER, "A", "T1", null, "CK_A"),
            row("CHECK_2", FireBirdDependency.TYPE_TRIGGER, "A", "T1", null, "CK_A"),
            row("RDB$2", FireBirdDependency.TYPE_COMPUTED_FIELD, "A", "T1", "B", null),
            row("IX_EXPR", FireBirdDependency.TYPE_INDEX_EXPRESSION, "A", "T1", null, null),
            row("TR1", FireBirdDependency.TYPE_TRIGGER, "A", "T1", null, null),
            row("TRDB", FireBirdDependency.TYPE_TRIGGER, null, null, null, null),
            row("V1", FireBirdDependency.TYPE_VIEW, "ID", null, null, null),
            row("V1", FireBirdDependency.TYPE_VIEW, "A", null, null, null),
            row("F1", FireBirdDependency.TYPE_FUNCTION, null, null, null, null)
        );
        DBSObject owner = Mockito.mock(DBSObject.class);
        Mockito.when(owner.getName()).thenReturn("T1");

        List<FireBirdDependency> dependencies = FireBirdDependency.readDependencies(
            mockSession(rows), owner, FireBirdDependency.RELATION_TYPES, true);

        Assertions.assertEquals(
            List.of(
                "Check constraint T1.CK_A [A]",
                "Computed column T1.B [A]",
                "Index T1.IX_EXPR [A]",
                "Table trigger T1.TR1 [A]",
                "Database trigger TRDB []",
                "View V1 [ID, A]",
                "Function F1 []"),
            dependencies.stream().map(dependency -> dependency + " [" + dependency.getFieldNames() + "]").toList());
        Assertions.assertSame(owner, dependencies.getFirst().getParentObject());
    }

    @NotNull
    private static Map<String, Object> row(
        @NotNull String objectName,
        int objectType,
        @Nullable String fieldName,
        @Nullable String tableName,
        @Nullable String columnName,
        @Nullable String constraintName
    ) {
        // Firebird pads CHAR names with spaces
        Map<String, Object> row = new HashMap<>();
        row.put("OBJECT_NAME", objectName + "   ");
        row.put("OBJECT_TYPE", objectType);
        row.put("FIELD_NAME", fieldName == null ? null : fieldName + "   ");
        row.put("TABLE_NAME", tableName == null ? null : tableName + "   ");
        row.put("COLUMN_NAME", columnName == null ? null : columnName + "   ");
        row.put("CONSTRAINT_NAME", constraintName == null ? null : constraintName + "   ");
        return row;
    }

    @NotNull
    private static JDBCSession mockSession(@NotNull List<Map<String, Object>> rows) throws Exception {
        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);
        int[] index = {-1};
        Mockito.when(resultSet.next()).thenAnswer(invocation -> ++index[0] < rows.size());
        Mockito.when(resultSet.getString(Mockito.anyString()))
            .thenAnswer(invocation -> (String) rows.get(index[0]).get(invocation.<String>getArgument(0)));
        Mockito.when(resultSet.getInt(Mockito.anyString()))
            .thenAnswer(invocation -> (Integer) rows.get(index[0]).get(invocation.<String>getArgument(0)));
        return session;
    }
}
