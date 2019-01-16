/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.controls.finder.viewer.AdvancedListViewer;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolder;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderComposite;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderInfo;

import java.util.List;

/**
 * DriverTabbedList
 *
 // Tabs:
 // - Recent
 // - Cloud
 // - Embedded
 // - All

 */
public class DriverTabbedList extends StructuredViewer {
    private static final Log log = Log.getLog(DriverTabbedList.class);
    private final TabbedFolderComposite folderComposite;

    public DriverTabbedList(Composite parent, int style) {

        List<DBPDriver> allDrivers = DriverUtils.getAllDrivers();
        allDrivers.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        List<DBPDriver> recentDrivers = DriverUtils.getRecentDrivers(allDrivers, 6);


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

        TabbedFolderInfo[] folders = new TabbedFolderInfo[] {
            new TabbedFolderInfo("popular", "Popular", DBIcon.TREE_DATABASE, "Recent drivers", false, new DriverListFolder(recentDrivers)),
            new TabbedFolderInfo("all", "All", DBIcon.TREE_DATABASE, "All drivers", false, new DriverListFolder(allDrivers))
        };
        folderComposite.setFolders(getClass().getSimpleName(), folders);
        folderComposite.switchFolder("popular", false);
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

        private AdvancedListViewer viewer;
        private List<DBPDriver> drivers;
        private boolean activated;

        DriverListFolder(List<DBPDriver> drivers) {
            this.drivers = drivers;
        }

        @Override
        public void createControl(Composite parent) {
            viewer = new AdvancedListViewer(parent, SWT.NONE);

            viewer.setContentProvider((IStructuredContentProvider) inputElement -> drivers.toArray());
            viewer.setLabelProvider(new LabelProvider() {
                @Override
                public Image getImage(Object element) {
                    return DBeaverIcons.getImage(((DBPDriver)element).getIconBig());
                }

                @Override
                public String getText(Object element) {
                    return ((DBPDriver)element).getName();
                }
            });
            registerViewer(viewer);
        }

        @Override
        public void aboutToBeShown() {
            if (!activated) {
                viewer.setInput(drivers);
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
    }

}
