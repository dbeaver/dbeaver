/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.data;

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

    public void createPartControl(Composite parent)
    {
        resultSetView = new ResultSetViewer(parent, getSite(), this);
    }

    public void activatePart()
    {
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
        resultSetView.getControl().setFocus();
    }

    public void refreshPart(Object source)
    {
        if (loaded) {
            resultSetView.refresh();
        }
    }

}
