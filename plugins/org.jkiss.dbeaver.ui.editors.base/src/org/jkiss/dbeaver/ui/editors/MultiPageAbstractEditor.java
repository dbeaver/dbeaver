/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
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
            firePropertyChange(PROP_INPUT);
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

        // Add small margin on top so part's tab doesn't touch editor's tabs
        parent.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 2, 0).create());
        container.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

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
            } else {
                // Sample toolbar's height as it fits quite nicely.
                ToolBar toolBar = new ToolBar(tabFolder, SWT.FLAT | SWT.RIGHT);
                // Add a dummy item as empty toolbars are considered {0, 0} on some platforms
                new ToolItem(toolBar, SWT.PUSH).setImage(DBeaverIcons.getImage(UIIcon.SEPARATOR_V));
                tabFolder.setTabHeight(toolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
                toolBar.dispose();
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