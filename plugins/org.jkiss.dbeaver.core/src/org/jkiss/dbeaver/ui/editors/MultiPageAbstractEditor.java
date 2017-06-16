/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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
        if (getEditorInput() == null) {
            super.init(site, input);
        } else {
            // Pages re-initialization. Do not call init bcause it recreates selection provider
            setSite(site);
            setInput(input);
        }
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
            tabFolder.setTabPosition(SWT.TOP);
            Control topRight = createTopRightControl(tabFolder);
            if (topRight != null) {
                Point trSize = topRight.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                tabFolder.setTabHeight(trSize.y);
                tabFolder.setTopRight(topRight, SWT.RIGHT | SWT.WRAP);
            }
//            tabFolder.setSimple(false);
            //tabFolder.setBorderVisible(true);
            Layout parentLayout = tabFolder.getParent().getLayout();
            if (parentLayout instanceof FillLayout) {
                ((FillLayout)parentLayout).marginHeight = 0;
//                ((FillLayout)parentLayout).marginWidth = 5;
            }
        }
    }

    protected void setPageToolTip(int index, String toolTip)
    {
        Composite pageContainer = getContainer();
        if (pageContainer instanceof CTabFolder) {
            CTabFolder tabFolder = (CTabFolder)pageContainer;
            if (index < tabFolder.getItemCount()) {
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

    public void recreatePages() {
        int pageCount = getPageCount();
        for (int i = pageCount; i > 0; i--) {
            removePage(i - 1);
        }
        createPages();
    }
}