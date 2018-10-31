/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.eclipse.nebula.widgets.gallery.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DriverGalleryViewer
 *
 * @author Serge Rider
 */
public class DriverGalleryViewer extends Viewer {

    private final Gallery gallery;
    private final List<DBPDriver> allDrivers = new ArrayList<>();;

    public DriverGalleryViewer(Composite parent, Object site, List<DataSourceProviderDescriptor> providers, boolean expandRecent) {
        {
            gallery = new Gallery(parent, SWT.V_SCROLL | SWT.MULTI);
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

            // Renderers
            DefaultGalleryGroupRenderer groupRenderer = new DefaultGalleryGroupRenderer();
            groupRenderer.setMaxImageHeight(16);
            groupRenderer.setMaxImageWidth(16);
            groupRenderer.setItemHeight(100);
            groupRenderer.setItemWidth(150);
            gallery.setGroupRenderer(new NoGroupRenderer());

            DefaultGalleryItemRenderer ir = new DefaultGalleryItemRenderer();
            ir.setDropShadows(false);
            ir.setShowLabels(true);
            gallery.setItemRenderer(ir);

            for (DataSourceProviderDescriptor dpd : providers) {
                allDrivers.addAll(dpd.getEnabledDrivers());
            }
            allDrivers.sort(Comparator.comparing(DBPNamedObject::getName));

            GalleryItem groupRecent = new GalleryItem(gallery, SWT.NONE);
            groupRecent.setText("Recent drivers"); //$NON-NLS-1$
            groupRecent.setImage(DBeaverIcons.getImage(DBIcon.TREE_SCHEMA));
            groupRecent.setExpanded(true);

            GalleryItem groupAll = new GalleryItem(gallery, SWT.NONE);
            groupAll.setText("All drivers"); //$NON-NLS-1$
            groupAll.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
            groupAll.setExpanded(true);

            for (DBPDriver driver : allDrivers) {

                GalleryItem item = new GalleryItem(groupAll, SWT.NONE);
                item.setImage(DBeaverIcons.getImage(driver.getIcon()));
                item.setText(driver.getName()); //$NON-NLS-1$
                item.setText(0, driver.getFullName()); //$NON-NLS-1$
                item.setText(1, driver.getDescription());
                item.setData(driver);
            }
        }
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

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {

    }

    @Override
    public ISelection getSelection() {
        GalleryItem[] itemSelection = gallery.getSelection();
        Object[] selectedDrivers = new Object[itemSelection.length];
        for (int i = 0; i < itemSelection.length; i++) {
            selectedDrivers[i] = itemSelection[i].getData();
        }
        return new StructuredSelection(selectedDrivers);
    }

    @Override
    public void refresh() {

    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {

    }

    @Override
    public void setInput(Object input) {

    }

    @Override
    public void setSelection(ISelection selection) {

    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {

    }

}
