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
package org.jkiss.dbeaver.ext.firebird.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdDataSource;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdMetaModel;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTable;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FireBirdIndexManagerTest extends DBeaverUnitTest {

    private FireBirdTable table;
    private DBRProgressMonitor monitor;

    @BeforeEach
    public void setUp() {
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(dialect.getQuotedIdentifier(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(dialect.getQuotedString(Mockito.anyString()))
            .thenAnswer(invocation -> "'" + invocation.<String>getArgument(0).replace("'", "''") + "'");
        FireBirdDataSource dataSource = Mockito.mock(FireBirdDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        table = Mockito.mock(FireBirdTable.class);
        Mockito.when(table.getDataSource()).thenReturn(dataSource);
        Mockito.when(table.getFullyQualifiedName(Mockito.any())).thenReturn("T_IX");
        monitor = Mockito.mock(DBRProgressMonitor.class);
    }

    @Test
    public void createsColumnIndex() {
        FireBirdTableIndex index = newIndex("IX_NAME", true, false, null, null, null);
        addColumns(index, true, "NAME", "CITY");

        Assertions.assertEquals(List.of("CREATE INDEX IX_NAME ON T_IX (NAME,CITY)"), getCreateScripts(index));
    }

    @Test
    public void createsUniqueDescendingIndexWithoutColumnSortOrder() {
        FireBirdTableIndex index = newIndex("IX_QTY", false, true, null, null, null);
        addColumns(index, true, "QTY");

        Assertions.assertEquals(List.of("CREATE UNIQUE DESCENDING INDEX IX_QTY ON T_IX (QTY)"), getCreateScripts(index));
    }

    @Test
    public void newIndexIsDescendingIfAColumnIsDescending() {
        // In the UI the sort order is chosen per column; Firebird only supports it for the whole index
        FireBirdTableIndex index = newIndex("IX_QTY", true, false, null, null, null);
        addColumns(index, false, "QTY");

        Assertions.assertEquals(List.of("CREATE DESCENDING INDEX IX_QTY ON T_IX (QTY)"), getCreateScripts(index));
    }

    @Test
    public void createsExpressionPartialInactiveIndexWithComment() {
        FireBirdTableIndex index = newIndex("IX_UPPER", true, false, "UPPER(NAME)", "QTY > 0", "Case-insensitive name");
        index.setInactive(true);

        Assertions.assertEquals(
            List.of(
                "CREATE INDEX IX_UPPER ON T_IX COMPUTED BY (UPPER(NAME)) WHERE QTY > 0",
                "ALTER INDEX IX_UPPER INACTIVE",
                "COMMENT ON INDEX IX_UPPER IS 'Case-insensitive name'"),
            getCreateScripts(index));
    }

    @Test
    public void changesStateAndComment() {
        FireBirdTableIndex index = newIndex("IX_NAME", true, false, null, null, null);
        index.setInactive(false);
        index.setDescription("");

        Assertions.assertEquals(
            List.of("ALTER INDEX IX_NAME ACTIVE", "COMMENT ON INDEX IX_NAME IS NULL"),
            getModifyScripts(index, Map.of("inactive", false, DBConstants.PROP_ID_DESCRIPTION, "")));
    }

    @Test
    public void extractsExpressionAndCondition() {
        Assertions.assertEquals("UPPER(NAME)", FireBirdTableIndex.extractExpression("(UPPER(NAME))"));
        Assertions.assertEquals("(A) || (B)", FireBirdTableIndex.extractExpression("((A) || (B))"));
        Assertions.assertEquals("QTY > 0", FireBirdTableIndex.extractCondition("WHERE QTY > 0"));
        Assertions.assertEquals("QTY > 0", FireBirdTableIndex.extractCondition("where QTY > 0"));
        Assertions.assertNull(FireBirdTableIndex.extractExpression(null));
        Assertions.assertNull(FireBirdTableIndex.extractCondition(null));
    }

    @Test
    public void readsIndexFromResultSet() throws Exception {
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("ASC_OR_DESC")).thenReturn("D");
        Mockito.when(resultSet.getInt("RDB$INDEX_INACTIVE")).thenReturn(1);
        Mockito.when(resultSet.getDouble("RDB$STATISTICS")).thenReturn(0.25);
        Mockito.when(resultSet.getString("RDB$EXPRESSION_SOURCE")).thenReturn("(UPPER(NAME))");
        Mockito.when(resultSet.getString("RDB$CONDITION_SOURCE")).thenReturn("WHERE QTY > 0");
        Mockito.when(resultSet.getString("RDB$DESCRIPTION")).thenReturn("Name lookup");

        FireBirdTableIndex index = Assertions.assertInstanceOf(FireBirdTableIndex.class, new FireBirdMetaModel()
            .createIndexImpl(table, true, null, 0, "IX_UPPER", DBSIndexType.OTHER, true, resultSet));

        Assertions.assertTrue(index.isDescending());
        Assertions.assertTrue(index.isInactive());
        Assertions.assertEquals(0.25, index.getSelectivity());
        Assertions.assertEquals("UPPER(NAME)", index.getExpression());
        Assertions.assertEquals("QTY > 0", index.getCondition());
        Assertions.assertEquals("Name lookup", index.getDescription());
        Assertions.assertFalse(index.isUnique());
    }

    @NotNull
    private FireBirdTableIndex newIndex(
        @NotNull String name,
        boolean nonUnique,
        boolean descending,
        String expression,
        String condition,
        String description
    ) {
        FireBirdTableIndex index = new FireBirdTableIndex(table, nonUnique, name, false);
        index.setDescending(descending);
        index.setExpression(expression);
        index.setCondition(condition);
        index.setDescription(description);
        return index;
    }

    private void addColumns(@NotNull FireBirdTableIndex index, boolean ascending, @NotNull String... names) {
        int position = 1;
        for (String name : names) {
            GenericTableColumn column = Mockito.mock(GenericTableColumn.class);
            Mockito.when(column.getName()).thenReturn(name);
            index.addColumn(new GenericTableIndexColumn(index, column, position++, ascending));
        }
    }

    @NotNull
    private List<String> getCreateScripts(@NotNull FireBirdTableIndex index) {
        return new TestIndexManager().getCreateActions(monitor, index).stream().map(DBEPersistAction::getScript).toList();
    }

    @NotNull
    private List<String> getModifyScripts(@NotNull FireBirdTableIndex index, @NotNull Map<String, Object> properties) {
        return new TestIndexManager().getModifyActions(monitor, index, properties).stream()
            .map(DBEPersistAction::getScript)
            .toList();
    }

    /**
     * Exposes the protected action builders of the manager.
     */
    private static class TestIndexManager extends FireBirdIndexManager {

        @NotNull
        List<DBEPersistAction> getCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull FireBirdTableIndex index) {
            // The constructor is protected, an anonymous subclass may call it
            ObjectCreateCommand command = new ObjectCreateCommand(index, "Create", Map.of()) { };
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectCreateActions(monitor, Mockito.mock(DBCExecutionContext.class), actions, command, Map.of());
            return actions;
        }

        @NotNull
        List<DBEPersistAction> getModifyActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull FireBirdTableIndex index,
            @NotNull Map<String, Object> properties
        ) {
            ObjectChangeCommand command = new ObjectChangeCommand(index);
            command.getProperties().putAll(properties);
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectModifyActions(monitor, Mockito.mock(DBCExecutionContext.class), actions, command, Map.of());
            return actions;
        }
    }
}
