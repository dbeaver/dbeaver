/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.*;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * MultiPageDatabaseEditor
 */
public abstract class MultiPageDatabaseEditor extends MultiPageEditorPart implements IDatabaseEditor, IDataSourceProvider
{
    private DatabaseEditorListener listener;
    private int activePageIndex = -1;
    private Image editorImage;

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
        setPartName(input.getName());
        setTitleImage(input.getImageDescriptor());

        listener = new DatabaseEditorListener(this);
    }

    protected void setTitleImage(ImageDescriptor titleImage)
    {
    	if (getContainer() != null && getContainer().isDisposed()) {
    		return;
    	}
        Image oldImage = editorImage;
        editorImage = titleImage.createImage();
        super.setTitleImage(editorImage);

        UIUtils.dispose(oldImage);
    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(editorImage);
        listener.dispose();
        super.dispose();
    }

    @Override
    public IDatabaseEditorInput getEditorInput() {
        return (IDatabaseEditorInput)super.getEditorInput();
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    protected void createPages()
    {
        this.setContainerStyles();
    }

    @Override
    protected void initializePageSwitching() {
        super.initializePageSwitching();
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

    @Override
    protected void pageChange(int newPageIndex)
    {
        deactivateEditor();
        this.activePageIndex = newPageIndex;
        super.pageChange(newPageIndex);
        activateEditor();
    }

    protected final void deactivateEditor()
    {
        // Deactivate the nested services from the last active service locator.
        if (activePageIndex >= 0) {
            final IWorkbenchPart part = getEditor(activePageIndex);
            if (part instanceof IActiveWorkbenchPart) {
                ((IActiveWorkbenchPart) part).deactivatePart();
            }
        }
    }

    protected final void activateEditor()
    {
        final int pageIndex = getActivePage();
        final IWorkbenchPart part = getEditor(pageIndex);

        if (part instanceof IActiveWorkbenchPart) {
            ((IActiveWorkbenchPart) part).activatePart();
        }
    }

    @Override
    public DBPDataSource getDataSource() {
        return getEditorInput().getDataSource();
    }

    @Override
    public IEditorPart getActiveEditor()
    {
        return super.getActiveEditor();
    }
}