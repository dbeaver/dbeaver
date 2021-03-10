/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DriverCategoryDescriptor;
import org.jkiss.dbeaver.registry.DriverManagerRegistry;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.finder.viewer.AdvancedListViewer;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolder;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderComposite;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderInfo;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DriverTabbedViewer
 *
 // Tabs:
 // - Recent
 // - Cloud
 // - Embedded
 // - All

 */
public class DriverTabbedViewer extends StructuredViewer {
    private static final Log log = Log.getLog(DriverTabbedViewer.class);

    private static final String DIALOG_ID = "DBeaver.DriverTabbedViewer";//$NON-NLS-1$
    private static final String PARAM_LAST_FOLDER = "folder";

    private final TabbedFolderComposite folderComposite;
    private final List<DBPDataSourceContainer> dataSources;
    private ViewerFilter[] curFilters;
    private Comparator<DBPDriver> listComparator;

    public DriverTabbedViewer(Composite parent, int style, List<DBPDataSourceContainer> dataSources, Comparator<DBPDriver> driverComparator) {

        this.dataSources = dataSources;
        this.listComparator = driverComparator;
        //listComparator = new DriverUtils.DriverScoreComparator(dataSources);

        List<DBPDriver> allDrivers = DriverUtils.getAllDrivers();
        //allDrivers.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

        List<DBPDriver> ratedDrivers = new ArrayList<>(allDrivers);
        //DriverUtils.sortDriversByRating(dataSources, ratedDrivers);

        List<DBPDriver> recentDrivers = DriverUtils.getRecentDrivers(allDrivers, 12);

        folderComposite = new TabbedFolderComposite(parent, style) {
            @Override
            public boolean setFocus() {
                ITabbedFolder activeFolder = getActiveFolder();
                if (activeFolder != null) {
                    activeFolder.setFocus();
                    return true;
                } else {
                    return super.setFocus();
                }
            }
        };

        List<TabbedFolderInfo> folders = new ArrayList<>();
        folders.add(
            new TabbedFolderInfo(
                "all", "All", DBIcon.TREE_DATABASE, "All drivers", false,
                new DriverListFolder(null, ratedDrivers)));
        folders.add(
            new TabbedFolderInfo(
                "popular", "Popular", DBIcon.TREE_DATABASE, "Popular and recently used drivers", false,
                new DriverListFolder(null, recentDrivers)));

        List<TabbedFolderInfo> extFolders = new ArrayList<>();
        for (DriverCategoryDescriptor category : DriverManagerRegistry.getInstance().getCategories()) {
            if (category.isPromoted()) {
                extFolders.add(
                    new TabbedFolderInfo(
                        category.getId(), category.getName(), category.getIcon(), category.getDescription(), false,
                        new DriverListFolder(category, getCategoryDrivers(category, allDrivers))));
            }
        }
        extFolders.sort((o1, o2) -> {
            DriverCategoryDescriptor cat1 = ((DriverListFolder) o1.getContents()).category;
            DriverCategoryDescriptor cat2 = ((DriverListFolder) o2.getContents()).category;
            int cmp = cat1.getRank() - cat2.getRank();
            if (cmp == 0) {
                cmp = cat1.getName().compareTo(cat2.getName());
            }
            return cmp;
        });
        folders.addAll(extFolders);

        String folderId = UIUtils.getDialogSettings(DIALOG_ID).get(PARAM_LAST_FOLDER);
        if (CommonUtils.isEmpty(folderId)) {
            folderId = "popular";
        }
        folderComposite.setFolders(getClass().getSimpleName(), folders.toArray(new TabbedFolderInfo[0]));
        folderComposite.switchFolder(folderId, false);
        folderComposite.addFolderListener(folderId1 -> {
            if (curFilters != null) {
                ((DriverListFolder) folderComposite.getActiveFolder()).viewer.setFilters(curFilters);
            }
            UIUtils.getDialogSettings(DIALOG_ID).put(PARAM_LAST_FOLDER, folderId1);
            StructuredViewer currentViewer = getCurrentViewer();
            if (currentViewer != null) {
                ISelection selection = currentViewer.getSelection();
                if (selection.isEmpty()) {
                    DriverCategoryDescriptor category = DriverManagerRegistry.getInstance().getCategory(folderId1);
                    if (category != null) {
                        //fireSelectionChanged(new SelectionChangedEvent(currentViewer, new StructuredSelection(category)));
                    }
                } else {
                    fireSelectionChanged(new SelectionChangedEvent(currentViewer, selection));
                }
            }
        });
    }

    private List<DBPDriver> getCategoryDrivers(DriverCategoryDescriptor category, List<DBPDriver> allDrivers) {
        List<DBPDriver> drivers = new ArrayList<>();
        for (DBPDriver driver : allDrivers) {
            if (driver.getCategories().contains(category.getId())) {
                drivers.add(driver);
            }
        }
        return drivers;
    }

    public TabbedFolderComposite getFolderComposite() {
        return folderComposite;
    }

    private StructuredViewer getCurrentViewer() {
        ITabbedFolder activeFolder = folderComposite.getActiveFolder();
        if (activeFolder instanceof DriverListFolder) {
            return ((DriverListFolder) activeFolder).viewer;
        }
        return null;
    }

    public void setListComparator(Comparator<DBPDriver> listComparator) {
        this.listComparator = listComparator;

        TabbedFolderInfo[] folders = folderComposite.getFolders();
        if (folders != null) {
            for (TabbedFolderInfo folder : folders) {
                if (folder.getContents() instanceof DriverListFolder) {
                    ((DriverListFolder) folder.getContents()).refreshDrivers();
                }
            }
        }
    }

    @Override
    public Control getControl() {
        return folderComposite;
    }

    @Override
    public Object getInput() {
        StructuredViewer viewer = getCurrentViewer();
        return viewer == null ? null : viewer.getInput();
    }

    @Override
    public ISelection getSelection() {
        StructuredViewer viewer = getCurrentViewer();
        return viewer == null ? null : viewer.getSelection();
    }

    @Override
    public void refresh() {
        StructuredViewer viewer = getCurrentViewer();
        if (viewer != null) {
            viewer.refresh();
        }
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        StructuredViewer viewer = getCurrentViewer();
        if (viewer != null) {
            viewer.setSelection(selection, reveal);
        }
    }

    @Override
    public void setFilters(ViewerFilter... filters) {
        StructuredViewer viewer = getCurrentViewer();
        if (viewer != null) {
            viewer.setFilters(filters);
        }
        curFilters = filters;
    }

    @Override
    public void resetFilters() {
        StructuredViewer viewer = getCurrentViewer();
        if (viewer != null) {
            viewer.resetFilters();
        }
    }

    /////////////////////////////////////////
    // Internal stuff

    @Override
    protected Widget doFindInputItem(Object element) {
        return null;
    }

    @Override
    protected Widget doFindItem(Object element) {
        return null;
    }

    @Override
    protected void doUpdateItem(Widget item, Object element, boolean fullMap) {

    }

    @Override
    protected List getSelectionFromWidget() {
        return null;
    }

    @Override
    protected void internalRefresh(Object element) {

    }

    @Override
    public void reveal(Object element) {
        StructuredViewer viewer = getCurrentViewer();
        if (viewer != null) {
            viewer.reveal(element);
        }
    }

    @Override
    protected void setSelectionToWidget(List l, boolean reveal) {

    }

    private void registerViewer(AdvancedListViewer viewer) {
        viewer.addSelectionChangedListener(this::fireSelectionChanged);
        viewer.addDoubleClickListener(this::fireDoubleClick);
    }

    private class DriverListFolder implements ITabbedFolder {

        private final DriverCategoryDescriptor category;
        private AdvancedListViewer viewer;
        private final List<DBPDriver> drivers;
        private boolean activated;

        DriverListFolder(DriverCategoryDescriptor category, List<DBPDriver> drivers) {
            this.category = category;
            this.drivers = new ArrayList<>(drivers);
        }

        @Override
        public void createControl(Composite parent) {
            viewer = new AdvancedListViewer(parent, SWT.NONE);

            viewer.setContentProvider(new ListContentProvider());
            viewer.setLabelProvider(new DriverLabelProvider());
            registerViewer(viewer);
        }

        @Override
        public void aboutToBeShown() {
            if (!activated) {
                this.refreshDrivers();
                activated = true;
            }
        }

        @Override
        public void aboutToBeHidden() {

        }

        @Override
        public void setFocus() {
            viewer.getControl().setFocus();
        }

        @Override
        public void dispose() {
        }

        void refreshDrivers() {
            if (listComparator != null) {
                drivers.sort(listComparator);
            }
            if (viewer != null) {
                viewer.setInput(drivers);
            }
        }

        private class DriverLabelProvider extends LabelProvider implements IToolTipProvider {
            @Override
            public Image getImage(Object element) {
                return DBeaverIcons.getImage(((DBPDriver)element).getIconBig());
            }

            @Override
            public String getText(Object element) {
                return ((DBPDriver)element).getName();
            }

            @Override
            public String getToolTipText(Object element) {
                DBPDriver driver = (DBPDriver) element;
                List<DBPDataSourceContainer> usedBy = DriverUtils.getUsedBy(driver, dataSources);

                StringBuilder toolTip = new StringBuilder();
                toolTip.append(driver.getFullName());
                toolTip.append("\n");
                if (!usedBy.isEmpty()) {
                    toolTip.append("Saved connections: ").append(usedBy.size());
                } else {
                    toolTip.append("No saved connections yet");
                }
                if (!CommonUtils.isEmpty(driver.getDescription())) {
                    if (toolTip.length() > 0) {
                        toolTip.append("\n\n");
                    }
                    toolTip.append(driver.getDescription());
                }
                return toolTip.toString();
            }
        }
    }

}
