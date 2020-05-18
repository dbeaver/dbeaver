/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartConstants;

public abstract class DatabaseDebugDetailEditor {

    private ListenerList<IPropertyListener> listeners = new ListenerList<>();
    private boolean dirty = false;
    private boolean mnemonics = true;
    private boolean suppressPropertyChanges = false;

    public abstract Control createControl(Composite parent);

    public abstract void setFocus();

    public abstract Object getInput();

    public abstract void setInput(Object input) throws CoreException;

    public boolean isDirty() {
        return dirty;
    }

    protected void setDirty(int propId) {
        this.dirty = true;
        firePropertyChange(propId);
    }

    protected void setDirty(boolean dirty) {
        if (this.dirty != dirty) {
            this.dirty = dirty;
            firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        }
    }

    public abstract void doSave() throws CoreException;

    public abstract IStatus getStatus();

    protected void dispose() {
        listeners.clear();
    }

    public void addPropertyListener(IPropertyListener listener) {
        listeners.add(listener);
    }

    public void removePropertyListener(IPropertyListener listener) {
        listeners.remove(listener);
    }

    protected void firePropertyChange(int propId) {
        if (!suppressPropertyChanges) {
            for (IPropertyListener listener : listeners) {
                listener.propertyChanged(this, propId);
            }
        }
    }

    protected void suppressPropertyChanges(boolean suppress) {
        suppressPropertyChanges = suppress;
    }

    public void setMnemonics(boolean mnemonics) {
        this.mnemonics = mnemonics;
    }

    protected boolean isMnemonics() {
        return mnemonics;
    }

    protected String processMnemonics(String text) {
        if (isMnemonics()) {
            return text;
        }
        return LegacyActionTools.removeMnemonics(text);
    }

}
