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
package org.jkiss.dbeaver.ext.mimer.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mimer.model.MimerObjectUsedBy;
import org.jkiss.dbeaver.ext.mimer.model.MimerObjectUses;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

/**
 * "Open Target" navigator action for a {@link MimerObjectUsedBy}/{@link MimerObjectUses}
 * "Used By"/"Uses" dependency row - resolves and opens the real object the row names, exactly
 * what Ctrl/Alt+Click on the row's own "Target" property cell is meant to do already.
 * <p>
 * Added as a reliable alternative to that click gesture, which doesn't work at all on macOS:
 * Ctrl+Click there is intercepted by the OS/SWT as a secondary click (it opens the ordinary
 * navigator context menu instead of the left-click-with-modifier core's {@code
 * ObjectViewerRenderer} listens for - the context menu's own "View Object" entry just reopens the
 * row itself, not the resolved target), and Option(Alt)+Click doesn't navigate either. That
 * mouse-handling code lives in {@code org.jkiss.dbeaver.ui} (core, outside
 * this codebase) - rather than try to patch it, this menu action reaches the exact same {@link
 * org.jkiss.dbeaver.ext.mimer.model.MimerUtils#resolveDependencyObject} result through a path
 * that doesn't depend on any mouse modifier at all.
 *
 * @author Mimer Information Technology
 */
public class MimerOpenDependencyTargetHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNodes(selection).stream().findFirst().orElse(null);
        if (!(node instanceof DBNDatabaseNode dbNode)) {
            return null;
        }
        DBSObject row = dbNode.getObject();

        AbstractJob job = new AbstractJob("Resolve dependency target") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                DBSObject target = resolve(row, monitor);
                if (target == null) {
                    UIUtils.asyncExec(() -> DBWorkbench.getPlatformUI().showError(
                        MimerUIMessages.action_open_target_title,
                        MimerUIMessages.action_open_target_message));
                } else {
                    UIUtils.asyncExec(() -> DBWorkbench.getPlatformUI().openEntityEditor(target));
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
        return null;
    }

    @Nullable
    private static DBSObject resolve(@Nullable DBSObject row, DBRProgressMonitor monitor) {
        if (row instanceof MimerObjectUsedBy usedBy) {
            return usedBy.getObject(monitor);
        }
        if (row instanceof MimerObjectUses uses) {
            return uses.getObject(monitor);
        }
        return null;
    }
}
