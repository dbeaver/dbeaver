/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.search.data;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetListener;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

/**
 * DatabaseDataEditor
 */
public class SearchDataEditor extends AbstractDatabaseObjectEditor<DBSDataContainer>
    implements ResultSetProvider,ResultSetListener
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

//        FindReplaceAction action = (FindReplaceAction)getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.FIND.getId());
//        if (action != null) {
//            action.update();
//        }

        if (!loaded) {
            if (getDatabaseObject() != null && getDatabaseObject().isPersisted()) {
                resultSetView.setStatus("Query data from '" + getEditorInput().getDatabaseObject().getName() + "'...");
                resultSetView.refresh();
                loaded = true;
            }
        }
        //resultSetView.setSelection(resultSetView.getSelection());
    }

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

    @Override
    public ResultSetViewer getResultSetViewer()
    {
        return resultSetView;
    }

    @Override
    public DBSDataContainer getDataContainer()
    {
        return (DBSDataContainer)getEditorInput().getDatabaseObject();
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
        if (resultSetView != null) {
            resultSetView.getSpreadsheet().setFocus();
        }

        // Register find/replace action
        // We do it in setFocus because each entity editor registers it's own action - and they
        // share a single action bars instance. To avoid mess just update handler every time editor activated
        FindReplaceAction findReplaceAction = new FindReplaceAction(DBeaverActivator.getResourceBundle(), "Editor.FindReplace.", this); //$NON-NLS-1$
        findReplaceAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);

        IActionBars actionBars = getEditorSite().getActionBars();
        actionBars.setGlobalActionHandler("dde_findReplace", findReplaceAction);
        actionBars.updateActionBars();
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        if (loaded) {
            //resultSetView.refresh();
        }
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
            resultSetView.applyChanges(RuntimeUtils.makeMonitor(monitor));
        }
    }

    @Override
    public void handleResultSetChange()
    {
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    @Override
    public Object getAdapter(Class required)
    {
        if (resultSetView != null) {
            Object adapter = resultSetView.getAdapter(required);
            if (adapter != null) {
                return adapter;
            }
        }
        return super.getAdapter(required);
    }

}
