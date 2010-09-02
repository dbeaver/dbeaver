/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.ext.ui.IDataSourceEditor;
import org.jkiss.dbeaver.ext.ui.IObjectEditorPart;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.IDBNListener;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * MultiPageDatabaseEditor
 */
public abstract class MultiPageDatabaseEditor<INPUT_TYPE extends IDatabaseEditorInput> extends MultiPageEditorPart implements IDataSourceEditor, IDBNListener
{
    static final Log log = LogFactory.getLog(MultiPageDatabaseEditor.class);

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
        setPartName(input.getName());
        setTitleImage(input.getImageDescriptor().createImage());

        DBeaverCore.getInstance().getMetaModel().addListener(this);
    }

    public void dispose()
    {
        DBeaverCore.getInstance().getMetaModel().removeListener(this);
        super.dispose();
    }

    @Override
    @SuppressWarnings("unchecked")
    public INPUT_TYPE getEditorInput() {
        return (INPUT_TYPE )super.getEditorInput();
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

    protected int getContainerStyle()
    {
        return SWT.TOP | SWT.FLAT | SWT.BORDER;
    }

    protected int getContainerMargin()
    {
        return 5;
    }

    protected void createPages()
    {
        this.setContainerStyles();
    }

    private void setContainerStyles()
    {
        Composite pageContainer = getContainer();
        if (pageContainer instanceof CTabFolder) {
            CTabFolder tabFolder = (CTabFolder)pageContainer;
            tabFolder.setSimple(false);
            tabFolder.setTabPosition(SWT.TOP);
            tabFolder.setBorderVisible(true);
            Layout parentLayout = tabFolder.getParent().getLayout();
            if (parentLayout instanceof FillLayout) {
                ((FillLayout)parentLayout).marginHeight = 5;
                ((FillLayout)parentLayout).marginWidth = 5;
            }
        }
    }

    protected void setPageToolTip(int index, String toolTip)
    {
        Composite pageContainer = getContainer();
        if (pageContainer instanceof CTabFolder) {
            CTabFolder tabFolder = (CTabFolder)pageContainer;
            if (index > 0 && index < tabFolder.getItemCount()) {
                tabFolder.getItem(index).setToolTipText(toolTip);
            }
        }
    }

    protected void pageChange(int newPageIndex)
    {
        deactivateEditor();
        super.pageChange(newPageIndex);
        activateEditor();
    }

    protected final void deactivateEditor()
    {
        // Deactivate the nested services from the last active service locator.
        final int pageIndex = getActivePage();
        final IWorkbenchPart part = getEditor(pageIndex);
        if (part instanceof IObjectEditorPart) {
            ((IObjectEditorPart) part).deactivatePart();
        }
    }

    protected final void activateEditor()
    {
        final int pageIndex = getActivePage();
        final IWorkbenchPart part = getEditor(pageIndex);

        if (part instanceof IObjectEditorPart) {
            ((IObjectEditorPart) part).activatePart();
        }
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    public DBPDataSource getDataSource() {
        return getEditorInput() == null ? null : getEditorInput().getDataSource();
    }

    public void nodeChanged(final DBNEvent event)
    {
        if (event.getNode() == getEditorInput().getTreeNode()) {
            if (event.getAction() == DBNEvent.Action.REMOVE) {
                getSite().getShell().getDisplay().asyncExec(new Runnable() { public void run() {
                    IWorkbenchPage workbenchPage = getSite().getWorkbenchWindow().getActivePage();
                    if (workbenchPage != null) {
                        workbenchPage.closeEditor(MultiPageDatabaseEditor.this, false);
                    }
                }});
            } else if (event.getAction() == DBNEvent.Action.REFRESH) {
                getSite().getShell().getDisplay().asyncExec(new Runnable() { public void run() {
                    refreshContent(event);
                }});
            }
        }
    }

    protected void refreshContent(DBNEvent event)
    {

    }

}