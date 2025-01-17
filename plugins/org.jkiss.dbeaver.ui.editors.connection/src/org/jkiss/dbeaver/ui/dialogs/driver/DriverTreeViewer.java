/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeColumn;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.DefaultViewerToolTipSupport;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DriverTreeViewer
 *
 * @author Serge Rider
 */
public class DriverTreeViewer extends TreeViewer {

    private List<DBPDataSourceProviderDescriptor> providers;
    private final Map<String,DriverCategory> categories = new HashMap<>();
    private final List<Object> driverList = new ArrayList<>();
    private boolean sortByName = false;

    public static class DriverCategory {
        final String name;
        final List<DriverDescriptor> drivers = new ArrayList<>();

        DriverCategory(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public List<DriverDescriptor> getDrivers()
        {
            return drivers;
        }

        @Override
        public String toString()
        {
            return name;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof DriverCategory &&
                ((DriverCategory) obj).name.equals(name);
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }

        public DBPImage getImage() {
            DBPImage driverImage = null;
            for (DBPDriver driver : getDrivers()) {
                if (driverImage == null) {
                    driverImage = driver.getPlainIcon();
                } else if (!driverImage.equals(driver.getPlainIcon())) {
                    driverImage = null;
                    break;
                }
            }
            if (driverImage != null) {
                return driverImage;
            }
            return DBIcon.TREE_DATABASE_CATEGORY;
        }
    }

    public DriverTreeViewer(Composite parent, int style) {
        super(parent, style);
    }

    public void initDrivers(List<DBPDataSourceProviderDescriptor> providers, boolean expandRecent)
    {
        getTree().setHeaderVisible(true);
        this.providers = providers;
        if (this.providers == null) {
            this.providers = DataSourceProviderRegistry.getInstance().getEnabledDataSourceProviders();
        }

        new DefaultViewerToolTipSupport(this);

        TreeViewerColumn nameColumn = new TreeViewerColumn(this, SWT.LEFT);
        nameColumn.getColumn().setText("Name");
        nameColumn.getColumn().addListener(SWT.Selection, new DriversSortListener(nameColumn.getColumn(), true));
        nameColumn.setLabelProvider(new ViewLabelProvider());

        TreeViewerColumn usersColumn = new TreeViewerColumn(this, SWT.LEFT);
        usersColumn.getColumn().setText("#");
        usersColumn.getColumn().addListener(SWT.Selection, new DriversSortListener(usersColumn.getColumn(), false));
        usersColumn.setLabelProvider(new ViewLabelProvider());

        this.setContentProvider(new ViewContentProvider());

        this.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

        Collection<Object> drivers = collectDrivers();
        this.setInput(drivers);
        UIUtils.expandAll(this);
        UIUtils.packColumns(getTree(), true, new float[] {0.9f, 0.1f});

        if (expandRecent) {
            // Expand used driver categories
            for (Object driver : drivers) {
                if (driver instanceof DriverCategory && getConnectionCount(driver) > 0) {
                    expandToLevel(driver, ALL_LEVELS);
                } else {
                    collapseToLevel(driver, ALL_LEVELS);
                }
            }
        } else {
            this.collapseAll();
        }

        addDoubleClickListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (!selection.isEmpty()) {
                Object element = selection.getFirstElement();
                if (element instanceof DriverCategory || element instanceof DataSourceProviderDescriptor) {
                    if (Boolean.TRUE.equals(getExpandedState(element))) {
                        super.collapseToLevel(element, 1);
                    } else {
                        super.expandToLevel(element, 1);
                    }
                }
            }
        });
    }

    @Override
    public void refresh()
    {
        if (providers != null) {
            collectDrivers();
        }
        super.refresh();
    }

    private Collection<Object> collectDrivers()
    {
        for (DriverCategory category : categories.values()) {
            category.drivers.clear();
        }

        driverList.clear();
        for (DBPDataSourceProviderDescriptor provider : providers) {
            List<? extends DBPDriver> drivers = provider.getEnabledDrivers();
            for (DBPDriver driver : drivers) {
                String category = driver.getCategory();
                if (CommonUtils.isEmpty(category)) {
                    driverList.add(driver);
                } else {
                    DriverCategory driverCategory = categories.get(category);
                    if (driverCategory == null) {
                        driverCategory = new DriverCategory(category);
                        categories.put(category, driverCategory);
                    }
                    if (!driverList.contains(driverCategory)) {
                        driverList.add(driverCategory);
                    }
                    driverCategory.drivers.add((DriverDescriptor) driver);
                }
            }
        }
        driverList.sort((o1, o2) -> {
            int count1 = getConnectionCount(o1);
            int count2 = getConnectionCount(o2);
            if (sortByName || count1 == count2) {
                String name1 = o1 instanceof DriverDescriptor ? ((DriverDescriptor) o1).getName() : ((DriverCategory) o1).getName();
                String name2 = o2 instanceof DriverDescriptor ? ((DriverDescriptor) o2).getName() : ((DriverCategory) o2).getName();
                return name1.compareToIgnoreCase(name2);
            } else {
                return count2 - count1;
            }
        });
        for (DriverCategory category : categories.values()) {
            category.drivers.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        }
        return driverList;
    }

    private int getConnectionCount(Object obj)
    {
        List<DBPDataSourceContainer> allDataSources = DataSourceRegistry.getAllDataSources();

        if (obj instanceof DataSourceProviderDescriptor) {
            int count = 0;
            for (DBPDriver driver : ((DataSourceProviderDescriptor) obj).getEnabledDrivers()) {
                count += DriverUtils.getUsedBy(driver, allDataSources).size();
            }
            return count;
        } else if (obj instanceof DriverCategory) {
            int count = 0;
            for (DBPDriver driver : ((DriverCategory) obj).drivers) {
                count += DriverUtils.getUsedBy(driver, allDataSources).size();
            }
            return count;
        } else if (obj instanceof DBPDriver) {
            return DriverUtils.getUsedBy((DBPDriver) obj, allDataSources).size();
        } else {
            return 0;
        }
    }

    class ViewContentProvider implements ITreeContentProvider
    {
        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput)
        {
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public Object[] getElements(Object parent)
        {
            return getChildren(parent);
        }

        @Override
        public Object getParent(Object child)
        {
            if (child instanceof DriverDescriptor) {
                DriverDescriptor driver = (DriverDescriptor) child;
                if (driver.getCategory() != null) {
                    return categories.get(driver.getCategory());
                }
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parent)
        {
            if (parent instanceof Collection) {
                return ((Collection<?>) parent).toArray();
            } else if (parent instanceof DriverCategory) {
                return ((DriverCategory) parent).getDrivers().toArray();
            } else {
                return new Object[0];
            }
        }

        @Override
        public boolean hasChildren(Object parent)
        {
            if (parent instanceof DriverCategory) {
                return !((DriverCategory) parent).drivers.isEmpty();
            }
            return false;
        }
    }

    class ViewLabelProvider extends CellLabelProvider implements ILabelProvider {

        @Override
        public void update(ViewerCell cell) {
            switch (cell.getColumnIndex()) {
                case 0:
                    cell.setText(getText(cell.getElement()));
                    break;
                case 1:
                    final int count = getConnectionCount(cell.getElement());
                    cell.setText(count <= 0 ? "" : String.valueOf(count));
                    break;
                default:
                    cell.setText("");
                    break;
            }
            DBPImage image = getImage(cell.getElement(), cell.getColumnIndex());
            if (image != null) {
                cell.setImage(DBeaverIcons.getImage(image));
            }
        }

        public String getDescription(Object obj)
        {
            if (obj instanceof DataSourceProviderDescriptor) {
                return ((DataSourceProviderDescriptor) obj).getDescription();
            } else if (obj instanceof DriverDescriptor) {
                return ((DriverDescriptor) obj).getDescription();
            } else {
                return "";
            }
        }

        public DBPImage getImage(Object obj, int index)
        {
            if (index != 0) {
                return null;
            }
            DBPImage defImage = DBIcon.TREE_PAGE;
            DBPImage icon = null;
			if (obj instanceof DataSourceProviderDescriptor) {
                icon = ((DataSourceProviderDescriptor) obj).getIcon();
			    defImage = DBIcon.TREE_FOLDER;
            } else if (obj instanceof DriverCategory) {
                icon = ((DriverCategory)obj).getImage();
            } else if (obj instanceof DriverDescriptor) {
                icon = ((DriverDescriptor) obj).getIcon();
            }
            if (icon != null) {
                return icon;
            }

            return defImage;
        }

        @Override
        public Image getImage(Object element) {
            return DBeaverIcons.getImage(getImage(element, 0));
        }

        @Override
        public String getText(Object element) {
            if (element instanceof DataSourceProviderDescriptor) {
                return ((DataSourceProviderDescriptor) element).getName();
            } else if (element instanceof DriverCategory) {
                return ((DriverCategory) element).name;
            } else if (element instanceof DriverDescriptor) {
                return ((DriverDescriptor) element).getName();
            } else {
                return element.toString();
            }
        }

        @Override
        public String getToolTipText(Object element) {
            if (element instanceof DataSourceProviderDescriptor) {
                return ((DataSourceProviderDescriptor) element).getDescription();
            } else if (element instanceof DriverDescriptor) {
                return ((DriverDescriptor) element).getDescription();
            } else {
                return element.toString();
            }
        }
    }


    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        super.setSelection(selection, reveal);
    }

    private class DriversSortListener implements Listener {
        private final TreeColumn column;
        private final boolean sortByName;
        DriversSortListener(TreeColumn column, boolean sortByName) {
            this.column = column;
            this.sortByName = sortByName;
        }

        @Override
        public void handleEvent(Event event) {
            getTree().setSortColumn(this.column);
            getTree().setSortDirection(SWT.DOWN);
            DriverTreeViewer.this.sortByName = this.sortByName;
            DriverTreeViewer.this.refresh();
        }
    }
}
