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
package org.jkiss.dbeaver.ui.dashboard.control;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardGroupContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewContainer;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;

import java.util.ArrayList;
import java.util.List;

public class DashboardList extends Composite implements DashboardGroupContainer {

    private static final int ITEM_SPACING = 5;

    private DashboardViewContainer viewContainer;
    private List<DashboardItem> items = new ArrayList<>();
    private final Font boldFont;
    private DashboardItem selectedItem;

    public DashboardList(Composite parent, DashboardViewContainer viewContainer) {
        super(parent, SWT.NONE);

        this.viewContainer = viewContainer;

        Font normalFont = getFont();
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setHeight(fontData[0].getHeight() + 1);
        fontData[0].setStyle(SWT.BOLD);
        boldFont = new Font(normalFont.getDevice(), fontData[0]);

        addDisposeListener(e -> {
            boldFont.dispose();
        });

        RowLayout layout = new RowLayout();
        layout.spacing = getItemSpacing();
        layout.pack = false;
        layout.wrap = true;
        layout.justify = false;
        this.setLayout(layout);
    }

    DBPDataSourceContainer getDataSourceContainer() {
        return viewContainer.getDataSourceContainer();
    }

    @Override
    public DashboardViewContainer getView() {
        return viewContainer;
    }

    public List<DashboardItem> getItems() {
        return items;
    }

    void createDefaultDashboards() {
        List<DashboardDescriptor> dashboards = DashboardRegistry.getInstance().getDashboards(
            viewContainer.getDataSourceContainer(), true);
        for (DashboardDescriptor dd : dashboards) {
            DashboardItem item = new DashboardItem(this, dd);
        }
    }

    void addItem(DashboardItem item) {
        this.items.add(item);
    }

    void removeItem(DashboardItem item) {
        this.items.remove(item);
    }

    public int getItemSpacing() {
        return ITEM_SPACING;
    }

    public Font getTitleFont() {
        return boldFont;
    }

    public DashboardItem getSelectedItem() {
        return selectedItem;
    }

    public void setSelection(DashboardItem selection) {
        DashboardItem oldSelection = this.selectedItem;
        this.selectedItem = selection;
        if (oldSelection != null) {
            oldSelection.redraw();
        }
    }
}
