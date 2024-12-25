/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.*;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.ui.UIFonts;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.screenreaders.ScreenReader;
import org.jkiss.dbeaver.ui.screenreaders.ScreenReaderPreferences;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * MultiPageAbstractEditor
 */
public abstract class MultiPageAbstractEditor extends MultiPageEditorPart {
    private ImageDescriptor curTitleImage;
    private final List<Image> oldImages = new ArrayList<>();
    private int activePageIndex = -1;
    private final List<CTabItem> tabsList = new ArrayList<>();

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (getEditorInput() == null) {
            super.init(site, input);
        } else {
            // Pages re-initialization. Do not call init because it recreates selection provider
            setSite(site);
            setInput(input);
        }
        setPartName(input.getName());
        setTitleImage(input.getImageDescriptor());
    }

    @Override
    protected CTabItem createItem(int index, Control control) {
        CTabItem item = super.createItem(index, control);
        item.getControl().getAccessible().addAccessibleListener(new EditorAccessibleAdapter(item.getControl())); 
        tabsList.add(item);
        return item;
    }

    protected void setTitleImage(ImageDescriptor titleImage) {
        if (getContainer() != null && getContainer().isDisposed()) {
            return;
        }
        if (CommonUtils.equalObjects(curTitleImage, titleImage)) {
            return;
        }
        curTitleImage = titleImage;
        Image editorImage = titleImage.createImage();
        oldImages.add(editorImage);
        super.setTitleImage(editorImage);
    }

    @Override
    public void dispose() {
        for (Image img : oldImages) {
            UIUtils.dispose(img);
        }
        oldImages.clear();
        super.dispose();
    }

    /**
     * The method do save
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        // nothing
    }

    /**
     * The method do save as
     */
    @Override
    public void doSaveAs() {
        // nothing
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    protected void createPages() {
        this.setContainerStyles();
    }

    @Override
    protected CTabFolder createContainer(Composite parent) {
        CTabFolder container = super.createContainer(parent);

        BaseThemeSettings.instance.addPropertyListener(
            UIFonts.DBEAVER_FONTS_MAIN_FONT,
            s -> container.setFont(BaseThemeSettings.instance.baseFont),
            container
        );
        return container;
    }

    protected void setContainerStyles() {
        Composite pageContainer = getContainer();
        if (pageContainer instanceof CTabFolder tabFolder && !pageContainer.isDisposed()) {
            tabFolder.setFont(BaseThemeSettings.instance.baseFont);
            tabFolder.setSimple(true);
            tabFolder.setMRUVisible(true);
            tabFolder.setTabPosition(SWT.TOP);
            Control topRight = createTopRightControl(tabFolder);
            if (topRight != null) {
                Point trSize = topRight.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                tabFolder.setTabHeight(trSize.y);
                tabFolder.setTopRight(topRight, SWT.RIGHT | SWT.WRAP);
            }

            /*
             * final Accessible accessible = tabFolder.getAccessible();
             * accessible.addAccessibleListener(new AccessibleAdapter() { public void
             * getName(AccessibleEvent e) { if (e.childID < 0) { CTabItem selection =
             * tabFolder.getSelection(); if (selection != null) { e.result = "Tab " +
             * selection.getText(); } } } });
             */

//            tabFolder.setSimple(false);
            // tabFolder.setBorderVisible(true);
            Layout parentLayout = tabFolder.getParent().getLayout();
            if (parentLayout instanceof FillLayout) {
                ((FillLayout) parentLayout).marginHeight = 0;
//                ((FillLayout)parentLayout).marginWidth = 5;
            }
        }
    }

    protected void setPageToolTip(int index, String toolTip) {
        Composite pageContainer = getContainer();
        if (pageContainer instanceof CTabFolder) {
            CTabFolder tabFolder = (CTabFolder) pageContainer;
            if (index < tabFolder.getItemCount()) {
                tabFolder.getItem(index).setToolTipText(toolTip);
            }
        }
    }

    @Override
    protected void pageChange(int newPageIndex) {
        deactivateEditor();
        this.activePageIndex = newPageIndex;
        super.pageChange(newPageIndex);
        activateEditor();
    }

    protected final void deactivateEditor() {
        // Deactivate the nested services from the last active service locator.
        if (activePageIndex >= 0 && getEditorCount() > activePageIndex) {
            final IWorkbenchPart part = getEditor(activePageIndex);
            if (part instanceof IActiveWorkbenchPart) {
                ((IActiveWorkbenchPart) part).deactivatePart();
            }
        }
    }

    protected final void activateEditor() {
        final int pageIndex = getActivePage();
        final IWorkbenchPart part = getEditor(pageIndex);

        if (part instanceof IActiveWorkbenchPart) {
            ((IActiveWorkbenchPart) part).activatePart();
        }
    }

    @Override
    public IEditorPart getActiveEditor() {
        if (getContainer().isDisposed()) {
            return null;
        }
        return super.getActiveEditor();
    }

    protected IEditorPart getEditor(int pageIndex) {
        Item item = ((CTabFolder) getContainer()).getItem(pageIndex);
        if (item != null && !item.isDisposed()) {
            Object data = item.getData();
            if (data instanceof IEditorPart) {
                return (IEditorPart) data;
            }
        }
        return null;
    }

    protected int getEditorCount() {
        return ((CTabFolder) getContainer()).getItemCount();
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

    @Override
    public void setFocus() {
        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        String storedScreenReader = store.getString(ScreenReaderPreferences.PREF_SCREEN_READER_ACCESSIBILITY);
        ScreenReader screenReader = ScreenReader.getScreenReader(storedScreenReader);
        switch (screenReader) {
            case JAWS -> {
                if (activePageIndex != -1) {
                    CTabItem tabItem = tabsList.get(activePageIndex);
                    if (tabItem != null && !tabItem.isDisposed()) {
                        if (tabItem.getControl() != null && !tabItem.getControl().isDisposed()) {
                            tabItem.getControl().setFocus();
                            tabItem.getParent().forceFocus();
                        }
                    }
                }
            }
            case NARRATOR, NVDA, OTHER -> {
                if (activePageIndex != -1) {
                    CTabItem tabItem = tabsList.get(activePageIndex);
                    if (tabItem != null && !tabItem.isDisposed()) {
                        if (tabItem != null && !tabItem.isDisposed()) {
                            Control control = tabItem.getControl();
                            if (control != null && !control.isDisposed()) {
                                control.forceFocus();
                            }
                        }
                    }
                }
            }
            case DEFAULT -> {
                super.setFocus();
            }
        }
    }
}