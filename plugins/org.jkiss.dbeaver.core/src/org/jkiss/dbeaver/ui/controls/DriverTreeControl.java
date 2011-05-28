/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.*;

/**
 * DriverTreeControl
 *
 * @author Serge Rieder
 */
public class DriverTreeControl extends TreeViewer implements ISelectionChangedListener, IDoubleClickListener {

    private Object site;
    private List<DataSourceProviderDescriptor> providers;
    private Font boldFont;

    private static class DriverCategory {
        final String name;
        final List<DriverDescriptor> drivers = new ArrayList<DriverDescriptor>();
        private DriverCategory(String name)
        {
            this.name = name;
        }
    }

    public DriverTreeControl(Composite parent)
    {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        parent.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                UIUtils.dispose(boldFont);
            }
        });
    }

    public void initDrivers(Object site, List<DataSourceProviderDescriptor> providers)
    {
        this.site = site;
        this.providers = providers;
        if (this.providers == null) {
            this.providers = DataSourceProviderRegistry.getDefault().getDataSourceProviders();
        }

        TreeColumn nameColumn = new TreeColumn(getTree(), SWT.LEFT);
        nameColumn.setText("Name");

        TreeColumn usersColumn = new TreeColumn(getTree(), SWT.RIGHT);
        usersColumn.setText("Connections");

        //TreeColumn descColumn = new TreeColumn(getTree(), SWT.RIGHT);
        //descColumn.setText("Description");

        this.setContentProvider(new ViewContentProvider());
        this.setLabelProvider(new ViewLabelProvider());
        this.setInput(DataSourceProviderRegistry.getDefault());
        this.expandAll();
        this.addSelectionChangedListener(this);
        this.addDoubleClickListener(this);
        this.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.packColumns(getTree());
    }

    class ViewContentProvider implements ITreeContentProvider
    {
        public void inputChanged(Viewer v, Object oldInput, Object newInput)
        {
        }

        public void dispose()
        {
        }

        public Object[] getElements(Object parent)
        {
            return getChildren(parent);
        }

        public Object getParent(Object child)
        {
            if (child instanceof DriverDescriptor) {
                return ((DriverDescriptor) child).getProviderDescriptor();
            } else if (child instanceof DataSourceProviderDescriptor) {
                return ((DataSourceProviderDescriptor) child).getRegistry();
            } else {
                return null;
            }
        }

        public Object[] getChildren(Object parent)
        {
            if (parent instanceof DataSourceProviderRegistry) {
                List<Object> children = new ArrayList<Object>();
                for (DataSourceProviderDescriptor provider : providers) {
                    List<DriverDescriptor> drivers = provider.getEnabledDrivers();
                    if (drivers.isEmpty()) {
                        // skip
                    } else if (drivers.size() == 1) {
                        children.add(drivers.get(0));
                    } else {
                        children.add(provider);
                    }
                }
                return children.toArray();
            } else if (parent instanceof DataSourceProviderDescriptor) {
                final List<DriverDescriptor> drivers = ((DataSourceProviderDescriptor) parent).getEnabledDrivers();
                Collections.sort(drivers, new Comparator<DriverDescriptor>() {
                    public int compare(DriverDescriptor o1, DriverDescriptor o2)
                    {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                final Map<String, DriverCategory> categoryMap = new HashMap<String, DriverCategory>();
                final List<Object> children = new ArrayList<Object>();
                for (DriverDescriptor driver : drivers) {
                    final String categoryName = driver.getCategory();
                    if (CommonUtils.isEmpty(categoryName)) {
                        children.add(driver);
                    } else {
                        DriverCategory category = categoryMap.get(categoryName);
                        if (category == null) {
                            category = new DriverCategory(categoryName);
                            categoryMap.put(categoryName, category);
                            children.add(category);
                        }
                        category.drivers.add(driver);
                    }
                }
                return children.toArray();
            } else if (parent instanceof DriverCategory) {
                return ((DriverCategory) parent).drivers.toArray();
            } else {
                return new Object[0];
            }
        }

        public boolean hasChildren(Object parent)
        {
            if (parent instanceof DataSourceProviderRegistry) {
                return !providers.isEmpty();
            } else if (parent instanceof DataSourceProviderDescriptor) {
                return !((DataSourceProviderDescriptor) parent).getEnabledDrivers().isEmpty();
            } else if (parent instanceof DriverCategory) {
                return !((DriverCategory) parent).drivers.isEmpty();
            }
            return false;
        }
    }

    class ViewLabelProvider extends CellLabelProvider
    {

        public void update(ViewerCell cell) {
            switch (cell.getColumnIndex()) {
                case 0:
                    cell.setText(getLabel(cell.getElement()));
                    break;
                case 1:
                    final int count = getConnectionCount(cell.getElement());
                    cell.setText(count <= 0 ? "" : String.valueOf(count));
                    break;
                default:
                    cell.setText("");
                    break;
            }
            cell.setImage(getImage(cell.getElement(), cell.getColumnIndex()));
            if (cell.getElement() instanceof DriverDescriptor && !((DriverDescriptor)cell.getElement()).getUsedBy().isEmpty()) {
                //cell.setFont(boldFont);
            } else {
                cell.setFont(null);
            }
        }

        public String getLabel(Object obj)
        {
            if (obj instanceof DataSourceProviderDescriptor) {
                return ((DataSourceProviderDescriptor) obj).getName();
            } else if (obj instanceof DriverCategory) {
                return ((DriverCategory) obj).name;
            } else if (obj instanceof DriverDescriptor) {
                return ((DriverDescriptor) obj).getName();
            } else {
                return obj.toString();
            }
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

        public Image getImage(Object obj, int index)
        {
            if (index != 0) {
                return null;
            }
            Image defImage = DBIcon.TREE_PAGE.getImage();
			if (obj instanceof DataSourceProviderDescriptor) {
                Image icon = ((DataSourceProviderDescriptor) obj).getIcon();
                if (icon != null) {
                    return icon;
                }
			    defImage = DBIcon.TREE_FOLDER.getImage();
            } else if (obj instanceof DriverCategory) {
                Image icon = ((DriverCategory) obj).drivers.get(0).getIcon();
                if (icon != null) {
                    return icon;
                }
            } else if (obj instanceof DriverDescriptor) {
                Image icon = ((DriverDescriptor) obj).getIcon();
                if (icon != null) {
                    return icon;
                }
            }

            return defImage;
        }
    }


    public void selectionChanged(SelectionChangedEvent event)
    {
        if (site instanceof ISelectionChangedListener) {
            ((ISelectionChangedListener)site).selectionChanged(event);
        }
    }

    public void doubleClick(DoubleClickEvent event)
    {
        if (site instanceof IDoubleClickListener) {
            ((IDoubleClickListener)site).doubleClick(event);
        }
    }

}
