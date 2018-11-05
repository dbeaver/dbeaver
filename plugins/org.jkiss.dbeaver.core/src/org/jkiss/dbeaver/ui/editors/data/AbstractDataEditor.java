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
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Collections;

/**
 * AbstractDataEditor
 */
public abstract class AbstractDataEditor<OBJECT_TYPE extends DBSObject> extends AbstractDatabaseObjectEditor<OBJECT_TYPE>
    implements IResultSetContainer,IResultSetListener
{
    private static final Log log = Log.getLog(AbstractDataEditor.class);

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
    public void openNewContainer(DBRProgressMonitor monitor, DBSDataContainer dataContainer, DBDDataFilter newFilter) {
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            log.error("Can't open new container - not execution context found");
            return;
        }

        final DBNDatabaseNode targetNode = executionContext.getDataSource().getContainer().getPlatform().getNavigatorModel().getNodeByObject(monitor, dataContainer, false);
        if (targetNode == null) {
            UIUtils.showMessageBox(null, "Open link", "Can't navigate to '" + DBUtils.getObjectFullName(dataContainer, DBPEvaluationContext.UI) + "' - navigator node not found", SWT.ICON_ERROR);
            return;
        }

        openNewDataEditor(targetNode, newFilter);
    }

    @Override
    public IResultSetDecorator createResultSetDecorator() {
        return new QueryResultsDecorator();
    }

    public static void openNewDataEditor(DBNDatabaseNode targetNode, DBDDataFilter newFilter) {
        UIUtils.asyncExec(() -> {
            IEditorPart entityEditor = NavigatorHandlerObjectOpen.openEntityEditor(
                targetNode,
                DatabaseDataEditor.class.getName(),
                null,
                Collections.singletonMap(DatabaseDataEditor.ATTR_DATA_FILTER, newFilter),
                UIUtils.getActiveWorkbenchWindow(),
                true);

            if (newFilter != null && entityEditor instanceof MultiPageEditorPart) {
                Object selectedPage = ((MultiPageEditorPart) entityEditor).getSelectedPage();
                if (selectedPage instanceof IResultSetContainer) {
                    ResultSetViewer rsv = (ResultSetViewer) ((IResultSetContainer) selectedPage).getResultSetController();
                    if (rsv != null && !rsv.isRefreshInProgress() && !newFilter.equals(rsv.getModel().getDataFilter())) {
                        // Set filter directly
                        rsv.refreshWithFilter(newFilter);
                    }
                }
            }
        });
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
    public void handleResultSetSelectionChange(SelectionChangedEvent event) {
        // No actions
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
        if (force && resultSetView != null && resultSetView.hasData() && !resultSetView.isRefreshInProgress()) {
            resultSetView.refresh();
        }
    }

}
