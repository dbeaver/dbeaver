/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetListener;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class SQLResultsEditor extends EditorPart implements IResultSetListener {
    private ResultSetViewer viewer;

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        setTitleToolTip(input.getToolTipText());
    }

    @Override
    public void createPartControl(Composite parent) {
        final SQLResultsEditorInput input = (SQLResultsEditorInput) getEditorInput();
        viewer = new ResultSetViewer(parent, getSite(), input.getContainer());
        viewer.addListener(this);
        viewer.refresh();
        getSite().setSelectionProvider(viewer);
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
        getEditorSite().getActionBars().updateActionBars();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        viewer.doSave(monitor);
    }

    @Override
    public void doSaveAs() {
        viewer.doSaveAs();
    }

    @Override
    public boolean isDirty() {
        return viewer.isDirty();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return viewer.isSaveAsAllowed();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isInstance(viewer)) {
            return adapter.cast(viewer);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void dispose() {
        if (viewer != null) {
            viewer.removeListener(this);
            viewer = null;
        }
        super.dispose();
    }

    @Override
    public void handleResultSetChange() {
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    @Override
    public void handleResultSetLoad() {
        getEditorSite().getActionBars().updateActionBars();
    }

    @Override
    public void handleResultSetSelectionChange(SelectionChangedEvent event) {
        // do nothing
    }
}
