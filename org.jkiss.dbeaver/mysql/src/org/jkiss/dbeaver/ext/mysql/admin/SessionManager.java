/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditorInput;

/**
 * FolderEditor
 */
public class SessionManager extends EditorPart implements IDataSourceUser
{
    static Log log = LogFactory.getLog(SessionManager.class);

    private IDatabaseEditorInput editorInput;

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        setSite(site);
        setInput(input);
        this.editorInput = (IDatabaseEditorInput)input;
        setPartName(this.editorInput.getName());
        setTitleImage(this.editorInput.getImageDescriptor().createImage());
    }

    public void dispose()
    {
        super.dispose();
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void createPartControl(Composite parent)
    {
/*
        itemControl = new ItemListControl(parent, SWT.NONE, this, editorInput.getFolder());
        itemControl.fillData();
        // Hook context menu
        ViewUtils.addContextMenu(this);
        // Add drag and drop support
        ViewUtils.addDragAndDropSupport(this);
        getSite().setSelectionProvider(itemControl.getSelectionProvider());
*/
    }

    public void setFocus()
    {
    }

    public DBSDataSourceContainer getDataSourceContainer() {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? null : dataSource.getContainer();
    }

    public DBPDataSource getDataSource() {
        return editorInput == null || editorInput.getDatabaseObject() == null ? null : editorInput.getDatabaseObject().getDataSource();
    }

}