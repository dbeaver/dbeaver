/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.*;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.IObjectEditorPart;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * MultiPageDatabaseEditor
 */
public abstract class MultiPageDatabaseEditor<INPUT_TYPE extends IDatabaseEditorInput> extends MultiPageEditorPart implements IDatabaseEditor, IDataSourceProvider
{
    static final Log log = LogFactory.getLog(MultiPageDatabaseEditor.class);

    private DatabaseEditorListener listener;
    private int activePageIndex = -1;
    private Image editorImage;

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
        setPartName(input.getName());
        editorImage = input.getImageDescriptor().createImage();
        setTitleImage(editorImage);

        listener = new DatabaseEditorListener(this);
    }

    @Override
    protected void setTitleImage(Image titleImage)
    {
        super.setTitleImage(titleImage);
        if (editorImage != null && editorImage != titleImage) {
            editorImage.dispose();
            editorImage = titleImage;
        }
    }

    public void dispose()
    {
        if (editorImage != null) {
            editorImage.dispose();
            editorImage = null;
        }
        listener.dispose();
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
            if (part instanceof IObjectEditorPart) {
                ((IObjectEditorPart) part).deactivatePart();
            }
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

    public DBPDataSource getDataSource() {
        return getEditorInput() == null ? null : getEditorInput().getDataSource();
    }

    public IEditorPart getActiveEditor()
    {
        return super.getActiveEditor();
    }
}