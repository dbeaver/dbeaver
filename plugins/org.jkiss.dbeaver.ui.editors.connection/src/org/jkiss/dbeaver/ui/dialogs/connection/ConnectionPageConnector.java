/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverWithLazyIcon;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class ConnectionPageConnector extends ActiveWizardPage<NewConnectionWizard> {
    private static final int VIEWER_MARGIN_WIDTH = 20;
    private static final int VIEWER_MARGIN_HEIGHT = 24;

    private DBPDataSourceType dataSourceType;
    private DBPDriver selectedDriver;
    private ConnectorViewer viewer;

    ConnectionPageConnector(NewConnectionWizard wizard) {
        super("newConnectionDriverChoice");
        setTitle(UIConnectionMessages.dialog_new_connection_wizard_driver_title);
        setDescription(UIConnectionMessages.dialog_new_connection_wizard_driver_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
        GridLayout layout = (GridLayout) composite.getLayout();
        layout.marginWidth = VIEWER_MARGIN_WIDTH;
        layout.marginHeight = VIEWER_MARGIN_HEIGHT;

        viewer = new ConnectorViewer(composite);
        viewer.addSelectionChangedListener(event -> {
            Object element = ((IStructuredSelection) event.getSelection()).getFirstElement();
            selectedDriver = element instanceof DBPDriver driver ? driver : null;
            getContainer().updateButtons();
        });
        viewer.addDoubleClickListener(event -> {
            if (selectedDriver != null) {
                getContainer().showPage(getWizard().getNextPage(this));
            }
        });
        setControl(composite);
        refreshDrivers();
    }

    void setDataSourceType(DBPDataSourceType dataSourceType) {
        if (this.dataSourceType != dataSourceType) {
            this.dataSourceType = dataSourceType;
            this.selectedDriver = null;
            setDescription(NLS.bind(UIConnectionMessages.dialog_new_connection_wizard_driver_description, dataSourceType.getName()));
            refreshDrivers();
        }
    }

    @Nullable
    DBPDriver getSelectedDriver() {
        return selectedDriver;
    }

    @Nullable
    DBPDataSourceType getDataSourceType() {
        return dataSourceType;
    }

    private void refreshDrivers() {
        if (dataSourceType == null) {
            return;
        }
        List<DBPDriver> drivers = new ArrayList<>(dataSourceType.getEnabledDrivers());
        if (DBWorkbench.isDistributed()) {
            drivers.removeIf(driver -> !driver.getDefaultDriverLoader().isDriverInstalled());
        }
        drivers.sort(new DriverUtils.DriverScoreComparator(DataSourceRegistry.getAllDataSources()));
        selectedDriver = drivers.isEmpty() ? null : drivers.get(0);
        if (viewer == null) {
            return;
        }
        viewer.setDrivers(drivers);
        if (selectedDriver != null) {
            viewer.setSelection(new StructuredSelection(selectedDriver), true);
        }
    }

    private static class ConnectorViewer extends TableViewer {
        private static final int ROW_HEIGHT = 90;
        private static final int IMAGE_AREA_WIDTH = 84;
        private static final int IMAGE_MAX_WIDTH = 72;
        private static final int IMAGE_MAX_HEIGHT = 64;
        private static final int HORIZONTAL_PADDING = 14;
        private static final int TEXT_SPACING = 4;
        private static final int VERTICAL_PADDING = 14;
        private static final int MAX_VISIBLE_ITEMS = 3;
        private static final int TABLE_WIDTH_HINT = 100;

        private ConnectorViewer(Composite parent) {
            super(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
            Table table = getTable();
            GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
            layoutData.widthHint = TABLE_WIDTH_HINT;
            table.setLayoutData(layoutData);
            table.setLinesVisible(false);
            TableColumn column = new TableColumn(table, SWT.NONE);
            table.addListener(SWT.Resize, event -> {
                int width = table.getClientArea().width - 1;
//                ScrollBar verticalBar = table.getVerticalBar();
//                if (verticalBar != null && verticalBar.isVisible()) {
//                    width -= verticalBar.getSize().x;
//                }
                if (column.getWidth() != width) {
                    column.setWidth(width);
                }
            });
            setContentProvider(ArrayContentProvider.getInstance());
            setLabelProvider(new LabelProvider() {
                @Override
                public String getText(Object element) {
                    DBPDriver driver = (DBPDriver) element;
                    GC gc = new GC(table);
                    try {
                        gc.setFont(table.getFont());
                        String description = getDescription(driver, gc);
                        return description.isEmpty() ? driver.getName() : driver.getName() + " - " + description;
                    } finally {
                        gc.dispose();
                    }
                }
            });

            table.addListener(SWT.MeasureItem, this::measureItem);
            table.addListener(SWT.EraseItem, this::eraseItem);
            table.addListener(SWT.PaintItem, this::paintItem);
        }

        private void setDrivers(List<DBPDriver> drivers) {
            setInput(drivers);
            Table table = getTable();
            WeakReference<Table> tableReference = new WeakReference<>(table);
            Runnable iconUpdateCallback = () -> UIUtils.asyncExec(() -> {
                Table activeTable = tableReference.get();
                if (activeTable != null && !activeTable.isDisposed()) {
                    activeTable.redraw();
                }
            });
            drivers.stream()
                .filter(DBPDriverWithLazyIcon.class::isInstance)
                .map(DBPDriverWithLazyIcon.class::cast)
                .forEach(driver -> driver.loadIcon(iconUpdateCallback));

            int itemCount = table.getItemCount();
            int rowHeight = itemCount == 0 ? ROW_HEIGHT : Math.max(ROW_HEIGHT, table.getItem(0).getBounds().height);
            GridData layoutData = (GridData) table.getLayoutData();
            layoutData.heightHint = Math.min(itemCount, MAX_VISIBLE_ITEMS) * rowHeight + table.getBorderWidth() * 2;
            table.getParent().layout(true, true);
        }

        private void measureItem(Event event) {
            event.gc.setFont(BaseThemeSettings.instance.baseFontBold);
            int titleHeight = event.gc.getFontMetrics().getHeight();
            event.gc.setFont(getTable().getFont());
            DBPDriver driver = (DBPDriver) event.item.getData();
            String description = getDescription(driver, event.gc);
            int descriptionHeight = description.isEmpty() ? 0 : event.gc.textExtent(description).y;
            event.height = Math.max(ROW_HEIGHT, titleHeight + descriptionHeight +
                (description.isEmpty() ? 0 : TEXT_SPACING) + VERTICAL_PADDING * 2);
        }

        private void eraseItem(Event event) {
            event.detail &= ~(SWT.BACKGROUND | SWT.FOREGROUND | SWT.SELECTED | SWT.FOCUSED | SWT.HOT);
        }

        private void paintItem(Event event) {
            if (!(event.item instanceof TableItem item) || !(item.getData() instanceof DBPDriver driver)) {
                return;
            }

            Table table = getTable();
            Rectangle bounds = item.getBounds();
            int width = table.getClientArea().width;
            boolean selected = table.getSelectionIndex() == table.indexOf(item);
            event.gc.setBackground(table.getBackground());
            event.gc.fillRectangle(0, bounds.y, width, bounds.height);
            if (selected) {
                event.gc.setBackground(table.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
                event.gc.fillRectangle(0, bounds.y, width, bounds.height);
            }

            Image image = DBeaverIcons.getImage(driver.getIconBig());
            Rectangle imageBounds = image.getBounds();
            double scale = Math.min(1, Math.min(
                (double) IMAGE_MAX_WIDTH / imageBounds.width,
                (double) IMAGE_MAX_HEIGHT / imageBounds.height));
            int imageWidth = Math.max(1, (int) (imageBounds.width * scale));
            int imageHeight = Math.max(1, (int) (imageBounds.height * scale));
            int imageX = HORIZONTAL_PADDING + (IMAGE_AREA_WIDTH - imageWidth) / 2;
            int imageY = bounds.y + (bounds.height - imageHeight) / 2;
            event.gc.drawImage(image, 0, 0, imageBounds.width, imageBounds.height, imageX, imageY, imageWidth, imageHeight);

            String description = getDescription(driver, event.gc);
            int textX = HORIZONTAL_PADDING + IMAGE_AREA_WIDTH + HORIZONTAL_PADDING;
            event.gc.setFont(BaseThemeSettings.instance.baseFontBold);
            event.gc.setForeground(selected ?
                table.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT) : table.getForeground());
            int titleHeight = event.gc.getFontMetrics().getHeight();
            int descriptionHeight = 0;
            if (!description.isEmpty()) {
                event.gc.setFont(table.getFont());
                descriptionHeight = event.gc.textExtent(description).y;
            }
            int textY = bounds.y + (bounds.height - titleHeight - descriptionHeight -
                (description.isEmpty() ? 0 : TEXT_SPACING)) / 2;
            event.gc.setFont(BaseThemeSettings.instance.baseFontBold);
            event.gc.drawText(driver.getName(), textX, textY, true);
            if (!description.isEmpty()) {
                event.gc.setFont(table.getFont());
                event.gc.setForeground(selected ?
                    table.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT) :
                    table.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.drawText(description, textX, textY + titleHeight + TEXT_SPACING, true);
            }

            if (!selected) {
                event.gc.setForeground(table.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                event.gc.setAlpha(48);
                event.gc.drawLine(0, bounds.y + bounds.height - 1, width, bounds.y + bounds.height - 1);
                event.gc.setAlpha(255);
            } else if (table.isFocusControl()) {
                event.gc.drawFocus(1, bounds.y + 1, width - 2, bounds.height - 2);
            }
        }

        private String getDescription(DBPDriver driver, GC gc) {
            String description = CommonUtils.getSingleLineString(CommonUtils.notEmpty(driver.getDescription()));
            int textX = HORIZONTAL_PADDING + IMAGE_AREA_WIDTH + HORIZONTAL_PADDING;
            int availableWidth = getTable().getClientArea().width - textX - HORIZONTAL_PADDING;
            int averageCharacterWidth = (int) gc.getFontMetrics().getAverageCharacterWidth();
            if (availableWidth > 0 && averageCharacterWidth > 0) {
                description = StringUtils.wrap(description, Math.max(1, availableWidth / averageCharacterWidth));
            }
            return description;
        }
    }

    @Override
    public boolean isPageComplete() {
        return selectedDriver != null;
    }
}
