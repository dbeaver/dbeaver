/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.data;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

/**
 * DatabaseDataEditor
 */
public class DatabaseDataEditor extends AbstractDatabaseObjectEditor<DBSDataContainer> implements ResultSetProvider
{

    private ResultSetViewer resultSetView;
    private boolean loaded = false;
    //private boolean running = false;
    private Composite parent;

    public void createPartControl(Composite parent)
    {
        this.parent = parent;
    }

    public void activatePart()
    {
        if (resultSetView == null) {
            resultSetView = new ResultSetViewer(parent, getSite(), this);
            parent.layout();
        }

        if (!loaded) {
            if (getDatabaseObject() != null && getDatabaseObject().isPersisted()) {
                resultSetView.refresh();
                loaded = true;
            }
        }
    }

    public void deactivatePart()
    {
    }

    public DBSDataContainer getDataContainer()
    {
        return (DBSDataContainer)getEditorInput().getDatabaseObject();
    }

    public boolean isReadyToRun()
    {
        return true;
    }

    @Override
    public void setFocus()
    {
        if (resultSetView != null) {
            resultSetView.getControl().setFocus();
        }
    }

    public void refreshPart(Object source)
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
        if (resultSetView.isDirty()) {
            resultSetView.applyChanges();
        }
    }

}
