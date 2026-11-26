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
package org.jkiss.dbeaver.ext.doris.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.doris.model.DorisDataSource;
import org.jkiss.dbeaver.ext.doris.model.DorisDatabase;
import org.jkiss.dbeaver.ext.doris.model.DorisDialect;
import org.jkiss.dbeaver.ext.doris.model.DorisMetaModel;
import org.jkiss.dbeaver.ext.doris.model.DorisTable;
import org.jkiss.dbeaver.ext.doris.model.DorisTableColumn;
import org.jkiss.dbeaver.ext.doris.model.DorisView;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.properties.ObjectAttributeDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DorisTableManagerTest extends DBeaverUnitTest {

    @Test
    public void enablesTableAndColumnCommentEditing() throws DBException {
        DorisTable table = new DorisTable(newDatabase(), "comments", "TABLE", null);
        table.setPersisted(true);
        Assertions.assertTrue(isCommentEditable(table, DorisTable.class));
        DorisTableColumn column = new DorisTableColumn(table);
        column.setPersisted(true);
        Assertions.assertTrue(isCommentEditable(column, DorisTableColumn.class));
    }

    @Test
    public void keepsViewCommentsReadOnly() throws DBException {
        DorisView view = new DorisView(newDatabase(), "comments_view", "VIEW", null);
        view.setPersisted(true);
        Assertions.assertFalse(new DorisMetaModel().isTableCommentEditable());
        DorisTableColumn column = new DorisTableColumn(view);
        column.setPersisted(true);
        Assertions.assertFalse(isCommentEditable(column, DorisTableColumn.class));
    }

    @NotNull
    private DorisDatabase newDatabase() throws DBException {
        DorisDataSource dataSource = Mockito.mock(DorisDataSource.class);
        Mockito.when(dataSource.getMetaModel()).thenReturn(new DorisMetaModel());
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new DorisDialect());
        return new DorisDatabase(dataSource, null, "comments");
    }

    private boolean isCommentEditable(@NotNull Object object, @NotNull Class<?> objectClass) {
        PropertySourceEditable source = new PropertySourceEditable(object, object);
        return ObjectAttributeDescriptor.extractAnnotations(source, objectClass, null, null).stream()
            .filter(property -> DBConstants.PROP_ID_DESCRIPTION.equals(property.getId()))
            .findFirst().orElseThrow().isEditable(object);
    }

    @Test
    public void usesDorisColumnManagerForNestedColumns() {
        Assertions.assertTrue(Arrays.asList(new DorisTableManager().getChildTypes()).contains(DorisTableColumn.class));
    }

    @Test
    public void modifiesTableComment() throws DBException {
        assertCommentSQL("电子总装15线1", "'电子总装15线1'");
    }

    @Test
    public void escapesQuotesInTableComment() throws DBException {
        assertCommentSQL("Line's comment", "'Line''s comment'");
    }

    @Test
    public void clearsTableComment() throws DBException {
        assertCommentSQL("", "''");
        assertCommentSQL(null, "''");
    }

    @Test
    public void skipsUnchangedTableComment() throws DBException {
        Assertions.assertEquals(List.of(), new TestableTableManager().modifyActions("Unchanged", false));
    }

    @Test
    public void addsCommentsAfterCreatingTable() throws DBException {
        List<DBEPersistAction> actions = new TestableTableManager().creationCommentActions();
        Assertions.assertEquals(2, actions.size());
        Assertions.assertEquals("ALTER TABLE fifteen MODIFY COMMENT 'Table comment'", actions.getFirst().getScript());
        Assertions.assertEquals("ALTER TABLE fifteen MODIFY COLUMN line COMMENT 'Column comment'", actions.getLast().getScript());
    }

    private void assertCommentSQL(@Nullable String comment, @NotNull String literal) throws DBException {
        List<DBEPersistAction> actions = new TestableTableManager().modifyActions(comment, true);
        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals("ALTER TABLE `QualityInspection1`.`fifteen` MODIFY COMMENT " + literal, actions.getFirst().getScript());
    }

    private static class TestableTableManager extends DorisTableManager {
        @NotNull
        List<DBEPersistAction> creationCommentActions() throws DBException {
            DorisDataSource dataSource = Mockito.mock(DorisDataSource.class);
            Mockito.when(dataSource.getSQLDialect()).thenReturn(new DorisDialect());
            DorisTable table = Mockito.mock(DorisTable.class);
            Mockito.when(table.getDataSource()).thenReturn(dataSource);
            Mockito.when(table.getDescription()).thenReturn("Table comment");
            Mockito.when(table.getFullyQualifiedName(DBPEvaluationContext.DDL)).thenReturn("fifteen");
            DorisTableColumn column = new DorisTableColumn(table);
            column.setName("line");
            column.setDescription("Column comment");
            Mockito.doReturn(List.of(column)).when(table).getAttributes(ArgumentMatchers.any());
            ObjectChangeCommand command = new ObjectChangeCommand(table);
            command.getProperties().put(DBConstants.PROP_ID_DESCRIPTION, table.getDescription());
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectExtraActions(new VoidProgressMonitor(), Mockito.mock(DBCExecutionContext.class), actions, command, Map.of());
            return actions;
        }

        @NotNull
        List<DBEPersistAction> modifyActions(@Nullable String comment, boolean changed) throws DBException {
            DorisDataSource dataSource = Mockito.mock(DorisDataSource.class);
            Mockito.when(dataSource.getSQLDialect()).thenReturn(new DorisDialect());
            DorisTable table = Mockito.mock(DorisTable.class);
            Mockito.when(table.getDataSource()).thenReturn(dataSource);
            Mockito.when(table.isPersisted()).thenReturn(true);
            Mockito.when(table.getDescription()).thenReturn(comment);
            Mockito.when(table.getFullyQualifiedName(DBPEvaluationContext.DDL)).thenReturn("`QualityInspection1`.`fifteen`");
            ObjectChangeCommand command = new ObjectChangeCommand(table);
            if (changed) {
                command.getProperties().put(DBConstants.PROP_ID_DESCRIPTION, comment);
            }
            return List.of(command.getPersistActions(new VoidProgressMonitor(), Mockito.mock(DBCExecutionContext.class), Map.of()));
        }
    }
}
