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
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.ext.ui.IEmbeddedWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * MultiPageDatabaseEditor
 */
public abstract class MultiPageDatabaseEditor<INPUT_TYPE extends IDatabaseEditorInput> extends MultiPageEditorPart implements IDataSourceUser
{
    static final Log log = LogFactory.getLog(MultiPageDatabaseEditor.class);

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
        setPartName(input.getName());
        setTitleImage(input.getImageDescriptor().createImage());
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
        if (part instanceof IEmbeddedWorkbenchPart) {
            ((IEmbeddedWorkbenchPart) part).deactivatePart();
        }
    }

    protected final void activateEditor()
    {
        final int pageIndex = getActivePage();
        final IWorkbenchPart part = getEditor(pageIndex);

        if (part instanceof IEmbeddedWorkbenchPart) {
            ((IEmbeddedWorkbenchPart) part).activatePart();
        }
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return this;
    }

    public DBSDataSourceContainer getDataSourceContainer() {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? null : dataSource.getContainer();
    }

    public DBPDataSource getDataSource() {
        return getEditorInput() == null || getEditorInput().getDatabaseObject() == null ? null : getEditorInput().getDatabaseObject().getDataSource();
    }

}