/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug.ui.details;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.debug.core.breakpoints.IDatabaseBreakpoint;
import org.jkiss.dbeaver.ui.Widgets;

public class DatabaseBreakpointEditor extends DatabaseDebugDetailEditor {

    /**
     * Property id for hit count enabled state.
     */
    public static final int PROP_HIT_COUNT_ENABLED = 0x1005;

    /**
     * Property id for breakpoint hit count.
     */
    public static final int PROP_HIT_COUNT = 0x1006;

    private IDatabaseBreakpoint fBreakpoint;

    @Override
    public Control createControl(Composite parent) {
        return createStandardControls(parent);
    }

    protected Control createStandardControls(Composite parent) {
        Composite composite = Widgets.createComposite(parent, parent.getFont(), 4, 1, 0, 0, 0);
        return composite;
    }

    @Override
    public void setFocus() {
        // do nothing
    }

    @Override
    public Object getInput() {
        return fBreakpoint;
    }

    @Override
    public void setInput(Object input) throws CoreException {
        try {
            suppressPropertyChanges(true);
            if (input instanceof IDatabaseBreakpoint) {
                setBreakpoint((IDatabaseBreakpoint) input);
            } else {
                setBreakpoint(null);
            }
        } finally {
            suppressPropertyChanges(false);
        }
    }

    protected void setBreakpoint(IDatabaseBreakpoint breakpoint) throws CoreException {
        fBreakpoint = breakpoint;
        setDirty(false);
    }

    @Override
    public void doSave() throws CoreException {
        setDirty(false);
    }

    @Override
    public IStatus getStatus() {
        return Status.OK_STATUS;
    }

}
