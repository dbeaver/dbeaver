/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.ui.IDetailPane2;
import org.eclipse.debug.ui.IDetailPane3;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.ui.Widgets;
import org.jkiss.dbeaver.ui.ActionBars;

import java.util.HashSet;
import java.util.Set;

public abstract class DatabaseDebugDetailPane<EDITOR extends DatabaseDebugDetailEditor>
        implements IDetailPane2, IDetailPane3 {

    private static final Log log = Log.getLog(DatabaseDebugDetailPane.class);

    private String name;
    private String description;
    private String id;

    private IWorkbenchPartSite partSite;

    private EDITOR editor;

    private Set<Integer> autoSaveProperties = new HashSet<>();

    private ListenerList<IPropertyListener> propertyListeners = new ListenerList<>();
    private Composite editorParent;

    public DatabaseDebugDetailPane(String name, String description, String id) {
        this.name = name;
        this.description = description;
        this.id = id;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void init(IWorkbenchPartSite partSite) {
        this.partSite = partSite;
    }

    @Override
    public Control createControl(Composite parent) {
        editorParent = Widgets.createComposite(parent, 1, 1, GridData.FILL_BOTH);
        editorParent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        editor = createEditor(editorParent);
        editor.setMnemonics(false);
        editor.addPropertyListener((source, propId) -> {
            if (autoSaveProperties.contains(propId)) {
                try {
                    editor.doSave();
                    return;
                } catch (CoreException e) {
                    log.log(e.getStatus());
                }
            }
            firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        });
        return editor.createControl(editorParent);
    }

    protected abstract EDITOR createEditor(Composite parent);

    protected void registerAutosaveProperties(int[] autosave) {
        for (int anAutosave : autosave) {
            autoSaveProperties.add(anAutosave);
        }
    }

    protected void unregisterAutosaveProperties(int[] autosave) {
        for (int anAutosave : autosave) {
            autoSaveProperties.remove(anAutosave);
        }
    }

    @Override
    public ISelectionProvider getSelectionProvider() {
        return null;
    }

    @Override
    public boolean setFocus() {
        return false;
    }

    @Override
    public void display(IStructuredSelection selection) {
        // clear status line
        IStatusLineManager statusLine = ActionBars.extractStatusLineManager(partSite);
        if (statusLine != null) {
            statusLine.setErrorMessage(null);
        }
        EDITOR editor = getEditor();
        Object input = null;
        if (selection != null && selection.size() == 1) {
            input = selection.getFirstElement();
            // update even if the same in case attributes have changed
        }
        try {
            editor.setInput(input);
        } catch (CoreException e) {
            log.log(e.getStatus());
        }
    }

    protected EDITOR getEditor() {
        return editor;
    }

    @Override
    public boolean isDirty() {
        return editor != null && editor.isDirty();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        IStatusLineManager statusLine = ActionBars.extractStatusLineManager(partSite);
        if (statusLine != null) {
            statusLine.setErrorMessage(null);
        }
        try {
            editor.doSave();
        } catch (CoreException e) {
            if (statusLine != null) {
                statusLine.setErrorMessage(e.getMessage());
            } else {
                log.log(e.getStatus());
            }
        }
    }

    @Override
    public void doSaveAs() {
        // do nothing
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        return isDirty() && editor.getStatus().isOK();
    }

    @Override
    public void addPropertyListener(IPropertyListener listener) {
        propertyListeners.add(listener);
    }

    @Override
    public void removePropertyListener(IPropertyListener listener) {
        propertyListeners.remove(listener);
    }

    protected void firePropertyChange(int property) {
        for (IPropertyListener listener : propertyListeners) {
            listener.propertyChanged(this, property);
        }
    }

    @Override
    public void dispose() {
        editor = null;
        partSite = null;
        propertyListeners.clear();
        autoSaveProperties.clear();
        editorParent.dispose();
    }

}
