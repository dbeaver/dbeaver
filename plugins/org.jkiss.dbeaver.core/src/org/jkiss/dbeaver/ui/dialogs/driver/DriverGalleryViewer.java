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
import org.eclipse.nebula.jface.galleryviewer.FlatTreeContentProvider;
import org.eclipse.nebula.jface.galleryviewer.GalleryTreeViewer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DriverGalleryViewer
 *
 * @author Serge Rider
 */
public class DriverGalleryViewer extends GalleryTreeViewer {

    public static final String GROUP_ALL = "all";
    public static final String GROUP_RECENT = "recent";
    //private final Gallery gallery;
    private final List<DBPDriver> allDrivers = new ArrayList<>();;

    public DriverGalleryViewer(Composite parent, Object site, List<DataSourceProviderDescriptor> providers, boolean expandRecent) {
        super(new Gallery(parent, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER));
        setContentProvider(new FlatTreeContentProvider(new ListContentProvider()));
        gallery.setBackground(TextEditorUtils.getDefaultTextBackground());
        gallery.setForeground(TextEditorUtils.getDefaultTextForeground());
        gallery.setLayoutData(new GridData(GridData.FILL_BOTH));

        gallery.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (site instanceof ISelectionChangedListener) {
                    ((ISelectionChangedListener) site).selectionChanged(new SelectionChangedEvent(DriverGalleryViewer.this, getSelection()));
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (site instanceof IDoubleClickListener) {
                    ((IDoubleClickListener) site).doubleClick(new DoubleClickEvent(DriverGalleryViewer.this, getSelection()));
                }
            }
        });
        gallery.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                GalleryItem[] items = gallery.getSelection();
                if (items.length == 0) {
                    items = gallery.getItems();
                }
                if (items.length > 0) {
                    if (items[0].getItemCount() > 0) {
                        items[0] = items[0].getItem(0);
                    }
                    gallery.setSelection(new GalleryItem[]{ items[0] });
                }
            }
        });

        // Renderers
        DefaultGalleryGroupRenderer groupRenderer = new DefaultGalleryGroupRenderer();
        groupRenderer.setMaxImageHeight(16);
        groupRenderer.setMaxImageWidth(16);
        groupRenderer.setItemHeight(60);
        groupRenderer.setItemWidth(200);
        groupRenderer.setTitleBackground(gallery.getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND));
        gallery.setGroupRenderer(groupRenderer);
        //gallery.setGroupRenderer(new NoGroupRenderer());

        DriverGalleryItemRenderer ir = new DriverGalleryItemRenderer(parent);
        gallery.setItemRenderer(ir);

        for (DataSourceProviderDescriptor dpd : providers) {
            allDrivers.addAll(dpd.getEnabledDrivers());
        }
        allDrivers.sort(Comparator.comparing(DBPNamedObject::getName));

        createDriverGallery();
    }

    private void createDriverGallery() {
        gallery.removeAll();

        GalleryItem groupRecent = new GalleryItem(gallery, SWT.NONE);
        groupRecent.setText("Recent drivers"); //$NON-NLS-1$
        groupRecent.setImage(DBeaverIcons.getImage(DBIcon.TREE_SCHEMA));
        groupRecent.setData(GROUP_RECENT);
        groupRecent.setExpanded(true);

        GalleryItem groupAll = new GalleryItem(gallery, SWT.NONE);
        groupAll.setText("All drivers"); //$NON-NLS-1$
        groupAll.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
        groupAll.setData(GROUP_ALL);
        groupAll.setExpanded(true);

        fillDriverGroup(groupRecent);
        if (groupRecent.getItemCount() == 0) {
            groupRecent.dispose();
        }
        fillDriverGroup(groupAll);
    }

    private void fillDriverGroup(GalleryItem group) {
        List<DBPDataSourceContainer> allDataSources = DataSourceRegistry.getAllDataSources();

        ViewerFilter[] filters = getFilters();

        List<DBPDriver> drivers;
        if (GROUP_ALL.equals(group.getData())) {
            drivers = this.allDrivers;
        } else if (GROUP_RECENT.equals(group.getData())) {
            drivers = getRecentDrivers(allDataSources, 6);
        } else {
            drivers = Collections.emptyList();
        }
        for (DBPDriver driver : drivers) {

            boolean isVisible = true;
            for (ViewerFilter filter : filters) {
                if (!filter.select(this, null, driver)) {
                    isVisible = false;
                    break;
                }
            }
            if (!isVisible) {
                continue;
            }

            GalleryItem item = new GalleryItem(group, SWT.NONE);
            item.setImage(DBeaverIcons.getImage(driver.getIcon()));
            item.setText(driver.getName()); //$NON-NLS-1$
            item.setText(0, driver.getName()); //$NON-NLS-1$
            List<DBPDataSourceContainer> usedBy = DriverUtils.getUsedBy(driver, allDataSources);
            if (!usedBy.isEmpty()) {
                item.setText(1, "Connections: " + usedBy.size());
            }
            if (!CommonUtils.isEmpty(driver.getCategory())) {
                item.setText(2, driver.getCategory());
            }
            item.setData(driver);
        }
    }

    private List<DBPDriver> getRecentDrivers(List<DBPDataSourceContainer> allDataSources, int total) {
        Map<DBPDriver, Integer> connCountMap = new HashMap<>();
        for (DBPDriver driver : allDrivers) {
            connCountMap.put(driver, DriverUtils.getUsedBy(driver, allDataSources).size());
        }
        List<DBPDriver> recentDrivers = new ArrayList<>(allDrivers);
        recentDrivers.sort((o1, o2) -> {
            int ub1 = DriverUtils.getUsedBy(o1, allDataSources).size();
            int ub2 = DriverUtils.getUsedBy(o2, allDataSources).size();
            if (ub1 == ub2) {
                if (o1.isPromoted()) return 1;
                else if (o2.isPromoted()) return -1;
                else return o1.getName().compareTo(o2.getName());
            } else {
                return ub2 - ub1;
            }
        });
        if (recentDrivers.size() > total) {
            return recentDrivers.subList(0, total);
        }
        return recentDrivers;
    }

    public Control getControl() {
        return getGallery();
    }

    @Override
    public Object getInput() {
        return allDrivers;
    }

    public Gallery getGallery() {
        return gallery;
    }

    public void addTraverseListener(TraverseListener traverseListener) {
        if (traverseListener != null) {
            gallery.addTraverseListener(traverseListener);
        }
    }

/*
    @Override
    public ISelection getSelection() {
        GalleryItem[] itemSelection = gallery.getSelection();
        Object[] selectedDrivers = new Object[itemSelection.length];
        for (int i = 0; i < itemSelection.length; i++) {
            selectedDrivers[i] = itemSelection[i].getData();
        }
        return new StructuredSelection(selectedDrivers);
    }
*/

    @Override
    public void refresh(boolean updateLabels) {
        createDriverGallery();
    }
}
