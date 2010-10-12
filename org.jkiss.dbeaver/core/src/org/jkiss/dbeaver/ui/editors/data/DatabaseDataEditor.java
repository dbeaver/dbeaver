/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.data;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetProvider;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

/**
 * DatabaseDataEditor
 */
public class DatabaseDataEditor extends AbstractDatabaseObjectEditor<IDatabaseObjectManager<DBSDataContainer>> implements ResultSetProvider
{

    private ResultSetViewer resultSetView;
    private boolean loaded = false;
    private boolean running = false;

    public void createPartControl(Composite parent)
    {
        resultSetView = new ResultSetViewer(parent, getSite(), this);
    }

    public void activatePart()
    {
        if (!loaded) {
            resultSetView.refresh();
            loaded = true;
        }
    }

    public void deactivatePart()
    {
    }

    public DBSDataContainer getDataContainer()
    {
        return getObjectManager().getObject();
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
}
