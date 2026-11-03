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
package org.jkiss.dbeaver.model.access;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

public class DBAAccessChangeCommandTest extends DBeaverUnitTest {
    private final DBAAccessSubject subject = Mockito.mock(DBAAccessSubject.class);
    private final DBAAccessManager manager = Mockito.mock(DBAAccessManager.class);
    private final DBCExecutionContext context = Mockito.mock(DBCExecutionContext.class);

    @Test
    public void coalescesMembershipChangesAcrossScopesIntoOneAction() throws DBException {
        Mockito.when(subject.getAccessManager()).thenReturn(manager);
        Mockito.when(manager.makeChangeAction(Mockito.any(), Mockito.anyList())).thenReturn(new SQLDatabasePersistAction("test", "test"));
        DBAAccessChangeCommand first = command(state("server", false), state("server", true));
        DBAAccessChangeCommand second = command(state("database", false), state("database", true));
        Assertions.assertSame(first, second.merge(first, Map.of()));
        Assertions.assertEquals(1, first.getPersistActions(new VoidProgressMonitor(), context, Map.of()).length);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DBAAccessManager.Change>> captured = ArgumentCaptor.forClass(List.class);
        Mockito.verify(manager).makeChangeAction(Mockito.any(), captured.capture());
        Assertions.assertEquals(2, captured.getValue().size());
        Assertions.assertEquals("server", captured.getValue().getFirst().before().scope().id());
        Assertions.assertEquals("database", captured.getValue().getLast().before().scope().id());
    }

    @Test
    public void revertingOneScopeKeepsOtherScopeChanges() throws DBException {
        Mockito.when(subject.getAccessManager()).thenReturn(manager);
        Mockito.when(manager.makeChangeAction(Mockito.any(), Mockito.anyList())).thenReturn(new SQLDatabasePersistAction("test", "test"));
        DBAAccessChangeCommand first = command(state("server", false), state("server", true));
        DBAAccessChangeCommand second = command(state("database", false), state("database", true));
        second.merge(first, Map.of());
        DBAAccessChangeCommand reverted = command(state("server", true), state("server", false));
        Assertions.assertSame(first, reverted.merge(first, Map.of()));
        first.getPersistActions(new VoidProgressMonitor(), context, Map.of());
        Mockito.verify(manager).makeChangeAction(Mockito.any(), Mockito.eq(List.of(
            new DBAAccessManager.Change(state("database", false), state("database", true)))));
    }

    @Test
    public void fullRevertProducesNoCommand() {
        DBAAccessChangeCommand first = command(state("server", false), state("server", true));
        DBAAccessChangeCommand reverted = command(state("server", true), state("server", false));
        Assertions.assertNull(reverted.merge(first, Map.of()));
    }

    @Test
    public void repeatedMergePreservesUndoSnapshotsAndInitialState() {
        DBAAccessChangeCommand first = command(state("server", false), state("server", true));
        DBAAccessChangeCommand second = command(state("database", false), state("database", true));
        second.merge(first, Map.of());
        first.merge(null, Map.of());
        second.merge(first, Map.of());
        Assertions.assertEquals(state("server", false), first.getBefore());
        Assertions.assertEquals(state("server", true), first.getAfter());
        Assertions.assertEquals(state("database", false), second.getBefore());
        Assertions.assertEquals(state("database", true), second.getAfter());
    }

    @Test
    public void doesNotMergeDifferentPrincipals() {
        DBAAccessChangeCommand first = command(state("server", false), state("server", true));
        DBAAccessSubject other = Mockito.mock(DBAAccessSubject.class);
        DBAAccessChangeCommand second = new DBAAccessChangeCommand(other, "change", state("server", true), state("server", false));
        Assertions.assertSame(second, second.merge(first, Map.of()));
    }

    @NotNull
    private DBAAccessChangeCommand command(@NotNull DBAAccessState before, @NotNull DBAAccessState after) {
        return new DBAAccessChangeCommand(subject, "change", before, after);
    }

    @NotNull
    private DBAAccessState state(@NotNull String scope, boolean granted) {
        return new DBAAccessState(new DBAAccessState.Scope(scope, scope), null,
            List.of(new DBAAccessState.Role("role", granted, true)), List.of(), List.of());
    }
}
