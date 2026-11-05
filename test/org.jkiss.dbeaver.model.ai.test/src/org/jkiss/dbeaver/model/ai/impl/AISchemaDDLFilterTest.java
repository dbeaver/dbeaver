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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.ai.AISchemaGenerationOptions;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

public class AISchemaDDLFilterTest {
    private final DBRProgressMonitor monitor = new VoidProgressMonitor();
    private MockedStatic<DBWorkbench> workbench;

    @BeforeEach
    void setupPlatform() {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        workbench = Mockito.mockStatic(DBWorkbench.class);
        workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
    }

    @AfterEach
    void closePlatform() {
        workbench.close();
    }

    @Test
    public void compactSchemaFiltersIndividualKeysAndColumns() throws DBException {
        DBSTable table = table("orders");
        DBSTableForeignKey allowed = foreignKey(table, table("customers"), "allowed_fk");
        DBSTableForeignKey excluded = foreignKey(table, table("payroll"), "excluded_fk");
        Mockito.doReturn(List.of(allowed, excluded)).when(table).getAssociations(monitor);
        DBSEntityAttribute excludedColumn = Mockito.mock(DBSEntityAttribute.class);
        Mockito.when(excludedColumn.getName()).thenReturn("secret_column");
        Mockito.doReturn(table.getDataSource()).when(excludedColumn).getDataSource();
        Mockito.doReturn(List.of(table.getAttributes(monitor).getFirst(), excludedColumn)).when(table).getAttributes(monitor);
        AISchemaGenerationOptions options = options(false)
            .withObjectFilter((m, object) -> object != excluded && object != excludedColumn)
            .build();

        String ddl = new AISchemaGeneratorImpl().generateSchema(monitor, null, options, table);
        Assertions.assertTrue(ddl.contains("CREATE TABLE \"orders\""), ddl);
        Assertions.assertTrue(ddl.contains("REFERENCES customers"), ddl);
        Assertions.assertFalse(ddl.contains("payroll"), ddl);
        Assertions.assertFalse(ddl.contains("secret_column"), ddl);

        String unfiltered = new AISchemaGeneratorImpl().generateSchema(monitor, null, options(false).build(), table);
        Assertions.assertTrue(unfiltered.contains("REFERENCES payroll"), unfiltered);
        Assertions.assertTrue(unfiltered.contains("secret_column"), unfiltered);
    }

    @Test
    public void rejectedRootDoesNotReadOrRenderMetadata() throws DBException {
        DBSTable table = Mockito.mock(DBSTable.class);
        AISchemaGenerationOptions options = options(true).withObjectFilter((m, object) -> false).build();
        Assertions.assertEquals("", new AISchemaGeneratorImpl().generateSchema(monitor, null, options, table));
        Assertions.assertEquals("", DBStructUtils.generateTableDDL(monitor, table, Map.of(), false, options.objectFilter()));
        Mockito.verifyNoInteractions(table);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void nativeDDLRetainsAllowedCommandsAndSupportsForeignKeyOnlyMode() throws DBException {
        DBSTable table = table("orders");
        DBSTableForeignKey allowed = foreignKey(table, table("customers"), "allowed_fk");
        DBSTableForeignKey excluded = foreignKey(table, table("payroll"), "excluded_fk");
        Mockito.doReturn(List.of(allowed, excluded)).when(table).getAssociations(monitor);
        SQLForeignKeyManager fkManager = Mockito.mock(SQLForeignKeyManager.class, Mockito.RETURNS_DEEP_STUBS);
        for (DBSTableForeignKey key : List.of(allowed, excluded)) {
            SQLObjectEditor.ObjectCreateCommand command = fkManager.makeCreateCommand(key, Map.of());
            Mockito.when(fkManager.makeCreateCommand(Mockito.eq(key), Mockito.anyMap())).thenReturn(command);
            Mockito.doReturn(key).when(command).getObject();
            DBEPersistAction action = new SQLDatabasePersistAction("FK", "ALTER TABLE orders ADD " + key.getName());
            Mockito.when(command.getPersistActions(Mockito.any(), Mockito.any(), Mockito.anyMap()))
                .thenReturn(new DBEPersistAction[] {action});
        }
        TestTableManager manager = Mockito.mock(TestTableManager.class, Mockito.CALLS_REAL_METHODS);
        manager.foreignKeys = fkManager;
        manager.extraKey = excluded;
        DBERegistry registry = Mockito.mock(DBERegistry.class);
        Mockito.when(registry.getObjectManager(table.getClass(), SQLObjectEditor.class)).thenReturn(manager);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getEditorsRegistry()).thenReturn(registry);
        AISchemaGenerationOptions scopedOptions = options(true).withObjectFilter((m, object) -> object != excluded).build();

        {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            String ddl = new AISchemaGeneratorImpl().generateSchema(monitor, null, scopedOptions, table);
            Assertions.assertTrue(ddl.contains("CREATE TABLE orders"), ddl);
            Assertions.assertTrue(ddl.contains("allowed_fk"), ddl);
            Assertions.assertFalse(ddl.contains("excluded_fk"), ddl);

            DBEPersistAction[] onlyKeys = manager.getTableDDL(monitor, table,
                Map.of(DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS, true), scopedOptions.objectFilter());
            Assertions.assertEquals(1, onlyKeys.length);
            Assertions.assertTrue(onlyKeys[0].getScript().contains("allowed_fk"));

            String unfiltered = DBStructUtils.generateTableDDL(monitor, table, Map.of(), false);
            Assertions.assertTrue(unfiltered.contains("allowed_fk"), unfiltered);
            Assertions.assertTrue(unfiltered.contains("excluded_fk"), unfiltered);
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void incomingReferenceDDLIsFilteredBeforeRendering() throws DBException {
        DBSTable table = table("customers");
        DBSTableForeignKey allowed = foreignKey(table("orders"), table, "allowed_fk");
        DBSTableForeignKey excluded = foreignKey(table("payroll"), table, "excluded_fk");
        Mockito.doReturn(List.of(allowed, excluded)).when(table).getReferences(monitor);
        DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        SQLForeignKeyManager manager = Mockito.mock(SQLForeignKeyManager.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(manager.makeCreateCommand(allowed, Map.of()).getPersistActions(monitor, executionContext, Map.of()))
            .thenReturn(new DBEPersistAction[] {new SQLDatabasePersistAction("FK", "ALTER TABLE orders ADD allowed_fk")});
        DBERegistry registry = Mockito.mock(DBERegistry.class);
        Mockito.when(registry.getObjectManager(allowed.getClass(), SQLObjectEditor.class)).thenReturn(manager);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getEditorsRegistry()).thenReturn(registry);
        AISchemaGenerationOptions options = options(false).withSendReferences(true)
            .withObjectFilter((m, object) -> object != excluded).build();
        {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            String ddl = new AISchemaGeneratorImpl().generateSchema(monitor, executionContext, options, table);
            Assertions.assertTrue(ddl.contains("ALTER TABLE orders ADD allowed_fk"), ddl);
            Assertions.assertFalse(ddl.contains("payroll"), ddl);
            Mockito.verify(manager, Mockito.never()).makeCreateCommand(Mockito.eq(excluded), Mockito.anyMap());
        }
    }

    @NotNull
    private AISchemaGenerationOptions.Builder options(boolean fullDDL) {
        return AISchemaGenerationOptions.builder().withSendObjectComment(false).withSendColumnTypes(true)
            .withSendConstraints(false).withSendForeignKeys(true).withSendIndexes(false)
            .withSendFullDDL(fullDDL).withSendReferences(false).withUseFQN(false);
    }

    @NotNull
    private DBSTable table(@NotNull String name) throws DBException {
        DBSTable table = Mockito.mock(DBSTable.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        Mockito.when(table.getDataSource()).thenReturn(dataSource);
        Mockito.when(table.getName()).thenReturn(name);
        Mockito.when(table.getEntityType()).thenReturn(DBSEntityType.TABLE);
        DBSEntityAttribute column = Mockito.mock(DBSEntityAttribute.class);
        Mockito.when(column.getParentObject()).thenReturn(table);
        Mockito.when(column.getDataSource()).thenReturn(dataSource);
        Mockito.when(column.getName()).thenReturn("id");
        Mockito.when(column.getTypeName()).thenReturn("INTEGER");
        Mockito.doReturn(List.of(column)).when(table).getAttributes(monitor);
        return table;
    }

    @NotNull
    private DBSTableForeignKey foreignKey(@NotNull DBSTable owner, @NotNull DBSTable target, @NotNull String name)
        throws DBException {
        DBSTableForeignKey key = (DBSTableForeignKey) Mockito.mock(AbstractTableConstraint.class,
            Mockito.withSettings().extraInterfaces(DBSTableForeignKey.class));
        Mockito.when(key.getName()).thenReturn(name);
        Mockito.when(key.getParentObject()).thenReturn(owner);
        Mockito.when(key.getAssociatedEntity()).thenReturn(target);
        DBSEntityAttributeRef ref = Mockito.mock(DBSEntityAttributeRef.class);
        Mockito.doReturn(owner.getAttributes(monitor).getFirst()).when(ref).getAttribute();
        Mockito.doReturn(List.of(ref)).when(key).getAttributeReferences(monitor);
        return key;
    }

    public abstract static class TestTableManager extends SQLTableManager<DBSTable, DBSObjectContainer> {
        private SQLForeignKeyManager<?, ?> foreignKeys;
        private DBSTableForeignKey extraKey;

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        protected void addExtraDDLCommands(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBSTable table,
            @NotNull Map<String, Object> options,
            @NotNull StructCreateCommand command
        ) {
            if (extraKey != null) {
                command.aggregateCommand(((SQLForeignKeyManager) foreignKeys).makeCreateCommand(extraKey, options));
            }
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        protected <T extends DBSObject> SQLObjectEditor<T, DBSTable> getObjectEditor(
            @NotNull DBERegistry registry, @NotNull Class<? extends T> type
        ) {
            return type == DBSTableForeignKey.class ? (SQLObjectEditor<T, DBSTable>) foreignKeys : null;
        }

        @Override
        protected void addStructObjectCreateActions(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext executionContext,
            @NotNull List<DBEPersistAction> actions,
            @NotNull StructCreateCommand command,
            @NotNull Map<String, Object> options
        ) {
            StringBuilder ddl = new StringBuilder("CREATE TABLE orders (id INTEGER)");
            command.getObjectCommands().keySet().stream().filter(DBSTableForeignKey.class::isInstance)
                .map(DBSTableForeignKey.class::cast).forEach(key -> ddl.append("; ALTER TABLE orders ADD ").append(key.getName()));
            actions.add(new SQLDatabasePersistAction("DDL", ddl.toString()));
        }
    }
}
