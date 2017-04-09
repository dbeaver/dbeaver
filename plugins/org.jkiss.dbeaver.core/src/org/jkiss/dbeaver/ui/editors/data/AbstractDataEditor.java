/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.data;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainer;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetListener;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * AbstractDataEditor
 */
public abstract class AbstractDataEditor<OBJECT_TYPE extends DBSObject> extends AbstractDatabaseObjectEditor<OBJECT_TYPE>
    implements IResultSetContainer,IResultSetListener
{
    private ResultSetViewer resultSetView;
    private boolean loaded = false;
    //private boolean running = false;
    private Composite parent;

    @Override
    public void createPartControl(Composite parent)
    {
        this.parent = parent;
    }

    @Override
    public void activatePart()
    {
        createResultSetView();

        if (!loaded && !isSuspendDataQuery()) {
            if (isReadyToRun()) {
                resultSetView.setStatus(getDataQueryMessage());
                DBDDataFilter dataFilter = getEditorDataFilter();
                if (dataFilter == null) {
                    resultSetView.refresh();
                } else {
                    resultSetView.refreshWithFilter(dataFilter);
                }
                loaded = true;
            }
        }
        //resultSetView.setSelection(resultSetView.getSelection());
    }

    protected abstract DBDDataFilter getEditorDataFilter();

    protected abstract boolean isSuspendDataQuery();

    protected abstract String getDataQueryMessage();

    private void createResultSetView()
    {
        if (resultSetView == null) {
            resultSetView = new ResultSetViewer(parent, getSite(), this);
            resultSetView.addListener(this);
            parent.layout();
            resultSetView.getControl().setFocus();

            // Set selection provider from resultset
            getSite().setSelectionProvider(resultSetView);
        }
    }

    @Override
    public void deactivatePart()
    {
    }

    @Override
    public void dispose() {
        if (resultSetView != null) {
            resultSetView.removeListener(this);
            resultSetView = null;
        }
        super.dispose();
    }

    @Nullable
    @Override
    public ResultSetViewer getResultSetController()
    {
        return resultSetView;
    }

    @Override
    public boolean isReadyToRun()
    {
        return true;
    }

    @Override
    public void setFocus()
    {
        createResultSetView();
        if (resultSetView != null && !resultSetView.getActivePresentation().getControl().isDisposed()) {
            resultSetView.getActivePresentation().getControl().setFocus();
        }

        refreshActions();
    }

    private void refreshActions() {
        IActionBars actionBars = getEditorSite().getActionBars();
        actionBars.updateActionBars();
    }

    @Override
    public boolean isDirty()
    {
        return resultSetView != null && !resultSetView.getControl().isDisposed() && resultSetView.isDirty();
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
        if (resultSetView != null && resultSetView.isDirty()) {
            if (!resultSetView.applyChanges(RuntimeUtils.makeMonitor(monitor))) {
                monitor.setCanceled(true);
            }
        }
    }

    @Override
    public void handleResultSetLoad() {
        refreshActions();
    }

    @Override
    public void handleResultSetChange()
    {
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (resultSetView != null) {
            if (adapter == ResultSetViewer.class) {
                return adapter.cast(resultSetView);
            }
            T res = resultSetView.getAdapter(adapter);
            if (res != null) {
                return res;
            }
        }
        return null;//super.getAdapter(adapter);
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        if (resultSetView != null && resultSetView.hasData() && !resultSetView.isRefreshInProgress()) {
            resultSetView.refresh();
        }
    }

}
