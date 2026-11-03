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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DBAAccessChangeCommand extends DBECommandAbstract<DBAAccessSubject> {
    private final DBAAccessState before;
    private final DBAAccessState after;
    private Map<String, DBAAccessManager.Change> mergedChanges;
    private DBAAccessChangeCommand mergedCommand;

    public DBAAccessChangeCommand(
        @NotNull DBAAccessSubject subject,
        @NotNull String title,
        @NotNull DBAAccessState before,
        @NotNull DBAAccessState after
    ) {
        super(subject, title);
        this.before = before;
        this.after = after;
        resetMergedChanges();
    }

    @NotNull
    public DBAAccessState getBefore() {
        return before;
    }

    @NotNull
    public DBAAccessState getAfter() {
        return after;
    }

    @Nullable
    @Override
    public DBECommand<?> merge(@Nullable DBECommand<?> previous, @NotNull Map<Object, Object> userParams) {
        resetMergedChanges();
        if (previous instanceof DBAAccessChangeCommand command && command.getObject() == getObject()) {
            mergedCommand = command.mergedCommand;
            Map<String, DBAAccessManager.Change> changes = new LinkedHashMap<>(mergedCommand.mergedChanges);
            DBAAccessManager.Change old = changes.get(before.scope().id());
            DBAAccessState original = old == null ? before : old.before();
            if (original.equals(after)) {
                changes.remove(before.scope().id());
            } else {
                changes.put(before.scope().id(), new DBAAccessManager.Change(original, after));
            }
            mergedCommand.mergedChanges = changes;
            return changes.isEmpty() ? null : mergedCommand;
        }
        return mergedChanges.isEmpty() ? null : this;
    }

    private void resetMergedChanges() {
        mergedCommand = this;
        mergedChanges = new LinkedHashMap<>();
        if (!before.equals(after)) {
            mergedChanges.put(before.scope().id(), new DBAAccessManager.Change(before, after));
        }
    }

    @NotNull
    @Override
    public DBEPersistAction[] getPersistActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (mergedChanges.isEmpty()) {
            return new DBEPersistAction[0];
        }
        return new DBEPersistAction[] {
            getObject().getAccessManager().makeChangeAction(monitor, List.copyOf(mergedChanges.values()))
        };
    }
}
