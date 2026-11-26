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
import org.jkiss.dbeaver.ext.doris.model.DorisDialect;
import org.jkiss.dbeaver.ext.doris.model.DorisTable;
import org.jkiss.dbeaver.ext.doris.model.DorisTableColumn;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DorisTableColumnManagerTest extends DBeaverUnitTest {

    @Test
    public void modifiesStringColumnCommentWithoutDeclaringType() throws DBException {
        assertCommentSQL("电子总装15线1", "'电子总装15线1'");
    }

    @Test
    public void escapesQuotesInColumnComment() throws DBException {
        assertCommentSQL("Line's comment", "'Line''s comment'");
    }

    @Test
    public void clearsColumnComment() throws DBException {
        assertCommentSQL("", "''");
        assertCommentSQL(null, "''");
    }

    @Test
    public void skipsUnchangedColumnComment() throws DBException {
        Assertions.assertEquals(List.of(), new TestableColumnManager().modifyActions("Unchanged", false));
    }

    @Test
    public void addsCommentAfterCreatingColumn() throws DBException {
        List<DBEPersistAction> actions = new TestableColumnManager().creationCommentActions();
        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals("ALTER TABLE fifteen MODIFY COLUMN line COMMENT 'New column'", actions.getFirst().getScript());
    }

    private void assertCommentSQL(@Nullable String comment, @NotNull String literal) throws DBException {
        List<DBEPersistAction> actions = new TestableColumnManager().modifyActions(comment, true);
        Assertions.assertEquals(1, actions.size());
        Assertions.assertEquals(
            "ALTER TABLE `QualityInspection1`.`fifteen` MODIFY COLUMN `line comment` COMMENT " + literal,
            actions.getFirst().getScript()
        );
    }

    private static class TestDialect extends DorisDialect {
        TestDialect() {
            setIdentifierQuoteString(new String[][]{{"`", "`"}});
        }
    }

    private static class TestableColumnManager extends DorisTableColumnManager {
        @NotNull
        List<DBEPersistAction> creationCommentActions() {
            DorisDataSource dataSource = Mockito.mock(DorisDataSource.class);
            Mockito.when(dataSource.getSQLDialect()).thenReturn(new DorisDialect());
            DorisTable table = Mockito.mock(DorisTable.class);
            Mockito.when(table.getDataSource()).thenReturn(dataSource);
            Mockito.when(table.getFullyQualifiedName(DBPEvaluationContext.DDL)).thenReturn("fifteen");
            DorisTableColumn column = new DorisTableColumn(table);
            column.setName("line");
            column.setDescription("New column");
            List<DBEPersistAction> actions = new ArrayList<>();
            addObjectExtraActions(
                new VoidProgressMonitor(), Mockito.mock(DBCExecutionContext.class), actions, new ObjectChangeCommand(column), Map.of()
            );
            return actions;
        }

        @NotNull
        List<DBEPersistAction> modifyActions(@Nullable String comment, boolean changed) throws DBException {
            DorisDataSource dataSource = Mockito.mock(DorisDataSource.class);
            Mockito.when(dataSource.getSQLDialect()).thenReturn(new TestDialect());
            DorisTable table = Mockito.mock(DorisTable.class);
            Mockito.when(table.getDataSource()).thenReturn(dataSource);
            Mockito.when(table.getFullyQualifiedName(DBPEvaluationContext.DDL)).thenReturn("`QualityInspection1`.`fifteen`");
            DorisTableColumn column = new DorisTableColumn(table);
            column.setName("line comment");
            column.setTypeName("STRING");
            column.setDescription(comment);
            column.setPersisted(true);
            ObjectChangeCommand command = new ObjectChangeCommand(column);
            if (changed) {
                command.getProperties().put(DBConstants.PROP_ID_DESCRIPTION, comment);
            }
            return List.of(command.getPersistActions(new VoidProgressMonitor(), Mockito.mock(DBCExecutionContext.class), Map.of()));
        }
    }
}
