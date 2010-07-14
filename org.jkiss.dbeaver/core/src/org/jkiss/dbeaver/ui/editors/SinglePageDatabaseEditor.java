/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.IDBMListener;
import org.jkiss.dbeaver.model.meta.DBMEvent;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * SinglePageDatabaseEditor
 */
public abstract class SinglePageDatabaseEditor<INPUT_TYPE extends IDatabaseEditorInput> extends EditorPart implements IDataSourceUser, IDBMListener
{
    static final Log log = LogFactory.getLog(SinglePageDatabaseEditor.class);

    private INPUT_TYPE editorInput;

    @SuppressWarnings("unchecked")
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        this.setSite(site);
        this.editorInput = (INPUT_TYPE)input;
        this.setPartName(input.getName());
        this.setTitleImage(input.getImageDescriptor().createImage());

        DBeaverCore.getInstance().getMetaModel().addListener(this);
    }

    public void dispose()
    {
        DBeaverCore.getInstance().getMetaModel().removeListener(this);
        super.dispose();
    }

    @Override
    public INPUT_TYPE getEditorInput() {
        return editorInput;
    }

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public DBSDataSourceContainer getDataSourceContainer() {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? null : dataSource.getContainer();
    }

    public DBPDataSource getDataSource() {
        return getEditorInput() == null || getEditorInput().getDatabaseObject() == null ? null : getEditorInput().getDatabaseObject().getDataSource();
    }

    public void setFocus() {

    }

    public void nodeChanged(final DBMEvent event)
    {
        if (event.getNode() == getEditorInput().getTreeNode()) {
            if (event.getAction() == DBMEvent.Action.REMOVE) {
                getSite().getShell().getDisplay().asyncExec(new Runnable() { public void run() {
                    IWorkbenchPage workbenchPage = getSite().getWorkbenchWindow().getActivePage();
                    if (workbenchPage != null) {
                        workbenchPage.closeEditor(SinglePageDatabaseEditor.this, false);
                    }
                }});
            } else if (event.getAction() == DBMEvent.Action.REFRESH) {
                getSite().getShell().getDisplay().asyncExec(new Runnable() { public void run() {
                    refreshContent(event);
                }});
            }
        }
    }

    protected void refreshContent(DBMEvent event)
    {

    }

}