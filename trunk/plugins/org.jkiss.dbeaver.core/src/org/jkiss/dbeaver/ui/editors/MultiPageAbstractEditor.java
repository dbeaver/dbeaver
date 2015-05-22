/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.*;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * MultiPageAbstractEditor
 */
public abstract class MultiPageAbstractEditor extends MultiPageEditorPart
{
    private Image editorImage;
    private int activePageIndex = -1;

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
        setPartName(input.getName());
        setTitleImage(input.getImageDescriptor());
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
        super.dispose();
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

    protected void setContainerStyles()
    {
        Composite pageContainer = getContainer();
        if (pageContainer instanceof CTabFolder) {
            CTabFolder tabFolder = (CTabFolder)pageContainer;
            tabFolder.setSimple(true);
            tabFolder.setMRUVisible(true);
            Control topRight = createTopRightControl(tabFolder);
            if (topRight != null) {
                tabFolder.setTopRight(topRight, SWT.RIGHT | SWT.WRAP);
            }
            tabFolder.setTabPosition(SWT.TOP);
            //tabFolder.setBorderVisible(true);
            Layout parentLayout = tabFolder.getParent().getLayout();
            if (parentLayout instanceof FillLayout) {
                ((FillLayout)parentLayout).marginHeight = 5;
                //((FillLayout)parentLayout).marginWidth = 5;
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
    public IEditorPart getActiveEditor()
    {
        if (getContainer().isDisposed()) {
            return null;
        }
        return super.getActiveEditor();
    }

    protected IEditorPart getEditor(int pageIndex) {
        Item item = ((CTabFolder)getContainer()).getItem(pageIndex);
        if (item != null && !item.isDisposed()) {
            Object data = item.getData();
            if (data instanceof IEditorPart) {
                return (IEditorPart) data;
            }
        }
        return null;
    }

    protected Control createTopRightControl(Composite composite) {
        return null;
    }

}