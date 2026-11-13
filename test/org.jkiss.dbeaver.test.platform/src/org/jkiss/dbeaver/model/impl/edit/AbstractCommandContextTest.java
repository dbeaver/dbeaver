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
package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AbstractCommandContextTest extends DBeaverUnitTest {

    @Test
    public void removeFirstCommandOfBatchRelinksNextOne() {
        TestCommandContext context = new TestCommandContext(null, false);
        DBECommand<?> first = createCommand();
        DBECommand<?> second = createCommand();
        context.addCommand(first, null, false, null);
        context.addCommand(second, null, false, first);
        Assertions.assertSame(first, context.getUndoCommand());

        context.removeCommand(first);

        // The batch head must not stay the removed command, or Undo/Redo would bring it back
        Assertions.assertSame(second, context.getUndoCommand());
    }

    @Test
    public void removeMiddleCommandOfBatchKeepsChain() {
        TestCommandContext context = new TestCommandContext(null, false);
        DBECommand<?> first = createCommand();
        DBECommand<?> second = createCommand();
        DBECommand<?> third = createCommand();
        context.addCommand(first, null, false, null);
        context.addCommand(second, null, false, first);
        context.addCommand(third, null, false, second);

        context.removeCommand(second);

        Assertions.assertSame(first, context.getUndoCommand());
        context.removeCommand(first);
        Assertions.assertSame(third, context.getUndoCommand());
    }

    @Test
    public void removeUnlinkedCommandKeepsOtherBatches() {
        TestCommandContext context = new TestCommandContext(null, false);
        DBECommand<?> first = createCommand();
        DBECommand<?> second = createCommand();
        context.addCommand(first, null, false, null);
        context.addCommand(second, null, false, null);

        context.removeCommand(second);

        Assertions.assertSame(first, context.getUndoCommand());
    }

    @NotNull
    private static DBECommand<?> createCommand() {
        DBECommand<?> command = Mockito.mock(DBECommand.class);
        Mockito.when(command.isUndoable()).thenReturn(true);
        return command;
    }
}
