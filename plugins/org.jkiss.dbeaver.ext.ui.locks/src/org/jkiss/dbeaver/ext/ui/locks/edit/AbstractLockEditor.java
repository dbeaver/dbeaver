/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com) 
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
package org.jkiss.dbeaver.ext.ui.locks.edit;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.ui.locks.LocksUIMessages;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockManagerViewer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;

/**
 * AbstractLockEditor for Lock View
 */

public abstract class AbstractLockEditor extends SinglePageDatabaseEditor<IDatabaseEditorInput> {

    private LockManagerViewer lockViewer;

    public LockManagerViewer getLockViewer() {
        return lockViewer;
    }

    @Override
    public void dispose() {
        if (lockViewer != null) {
            lockViewer.dispose();
        }
        super.dispose();
    }

    @Override
    public void createEditorControl(Composite parent) {
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null) {
            setPartName(LocksUIMessages.create_editor_control_name_lock + executionContext.getDataSource().getContainer().getName());
            lockViewer = createLockViewer(executionContext, parent);
            lockViewer.refreshLocks(null);
        }
    }

    protected abstract LockManagerViewer createLockViewer(DBCExecutionContext executionContext, Composite parent);

    @Override
    public void refreshPart(Object source, boolean force) {
        lockViewer.refreshLocks(null);
    }

    @Override
    public void setFocus() {
        if (lockViewer != null) {
            lockViewer.getControl().setFocus();
        }
    }
}
