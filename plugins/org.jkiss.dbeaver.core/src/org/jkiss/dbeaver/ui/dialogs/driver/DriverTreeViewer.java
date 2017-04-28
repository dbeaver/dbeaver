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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeColumn;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DriverTreeViewer
 *
 * @author Serge Rider
 */
public class DriverTreeViewer extends TreeViewer implements ISelectionChangedListener, IDoubleClickListener {

    private Object site;
    private List<DataSourceProviderDescriptor> providers;
    private Font boldFont;
    private final Map<String,DriverCategory> categories = new HashMap<>();
    private final List<Object> driverList = new ArrayList<>();

    public static class DriverCategory {
        final String name;
        final List<DriverDescriptor> drivers = new ArrayList<>();

        public DriverCategory(String name)
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
    }

    public DriverTreeViewer(Composite parent, int style) {
        super(parent, style);
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                UIUtils.dispose(boldFont);
            }
        });
    }

    public void initDrivers(Object site, List<DataSourceProviderDescriptor> providers, boolean expandRecent)
    {
//        getTree().setHeaderVisible(true);
        this.site = site;
        this.providers = providers;
        if (this.providers == null) {
            this.providers = DataSourceProviderRegistry.getInstance().getDataSourceProviders();
        }

        TreeColumn nameColumn = new TreeColumn(getTree(), SWT.LEFT);
        nameColumn.setText("Name");

        TreeColumn usersColumn = new TreeColumn(getTree(), SWT.LEFT);
        usersColumn.setText("#");

        this.setContentProvider(new ViewContentProvider());
        this.setLabelProvider(new ViewLabelProvider());

        this.addSelectionChangedListener(this);
        this.addDoubleClickListener(this);
        this.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

        Collection<Object> drivers = collectDrivers();
        this.setInput(drivers);
        this.expandAll();
        getTree().addListener(SWT.Resize, new Listener() {
            volatile boolean resizing = false;

            @Override
            public void handleEvent(Event event) {
                if (resizing) {
                    return;
                }
                resizing = true;
                try {
                    UIUtils.packColumns(getTree(), true, new float[] {0.9f, 0.1f});
                } finally {
                    resizing = false;
                }
            }
        });

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
    }

    @Override
    public void refresh()
    {
        collectDrivers();
        super.refresh();
    }

    private Collection<Object> collectDrivers()
    {
        for (DriverCategory category : categories.values()) {
            category.drivers.clear();
        }

        driverList.clear();
        for (DataSourceProviderDescriptor provider : providers) {
            List<DriverDescriptor> drivers = provider.getEnabledDrivers();
            for (DriverDescriptor driver : drivers) {
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
                    driverCategory.drivers.add(driver);
                }
            }
        }
        Collections.sort(driverList, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2)
            {
                int count1 = getConnectionCount(o1);
                int count2 = getConnectionCount(o2);
                if (count1 == count2) {
                    String name1 = o1 instanceof DriverDescriptor ? ((DriverDescriptor) o1).getName() : ((DriverCategory)o1).getName();
                    String name2 = o2 instanceof DriverDescriptor ? ((DriverDescriptor) o2).getName() : ((DriverCategory)o2).getName();
                    return name1.compareToIgnoreCase(name2);
                } else {
                    return count2 - count1;
                }
            }
        });
        for (DriverCategory category : categories.values()) {
            Collections.sort(category.drivers, new Comparator<DriverDescriptor>() {
                @Override
                public int compare(DriverDescriptor o1, DriverDescriptor o2)
                {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
        }
        return driverList;
    }

    public int getConnectionCount(Object obj)
    {
        if (obj instanceof DataSourceProviderDescriptor) {
            int count = 0;
            for (DriverDescriptor driver : ((DataSourceProviderDescriptor) obj).getEnabledDrivers()) {
                count += driver.getUsedBy().size();
            }
            return count;
        } else if (obj instanceof DriverCategory) {
            int count = 0;
            for (DriverDescriptor driver : ((DriverCategory) obj).drivers) {
                count += driver.getUsedBy().size();
            }
            return count;
        } else if (obj instanceof DriverDescriptor) {
            return ((DriverDescriptor) obj).getUsedBy().size();
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
                return ((Collection) parent).toArray();
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

    class ViewLabelProvider extends CellLabelProvider implements ILabelProvider
    {

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
            if (cell.getElement() instanceof DriverDescriptor && !((DriverDescriptor)cell.getElement()).getUsedBy().isEmpty()) {
                //cell.setFont(boldFont);
            } else {
                cell.setFont(null);
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
			if (obj instanceof DataSourceProviderDescriptor) {
                DBPImage icon = ((DataSourceProviderDescriptor) obj).getIcon();
                if (icon != null) {
                    return icon;
                }
			    defImage = DBIcon.TREE_FOLDER;
            } else if (obj instanceof DriverCategory) {
                return DBIcon.TREE_DATABASE_CATEGORY;
            } else if (obj instanceof DriverDescriptor) {
                DBPImage icon = ((DriverDescriptor) obj).getIcon();
                if (icon != null) {
                    return icon;
                }
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
    }


    @Override
    public void selectionChanged(SelectionChangedEvent event)
    {
        if (site instanceof ISelectionChangedListener) {
            ((ISelectionChangedListener)site).selectionChanged(event);
        }
    }

    @Override
    public void doubleClick(DoubleClickEvent event)
    {
        if (site instanceof IDoubleClickListener) {
            ((IDoubleClickListener)site).doubleClick(event);
        }
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
    }

}
