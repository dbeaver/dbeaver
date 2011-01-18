/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * DriverTreeControl
 *
 * @author Serge Rieder
 */
public class DriverTreeControl extends TreeViewer implements ISelectionChangedListener, IDoubleClickListener {

    private Object site;
    private List<DataSourceProviderDescriptor> providers;

    public DriverTreeControl(Composite parent)
    {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    }

    public void initDrivers(Object site, List<DataSourceProviderDescriptor> providers)
    {
        this.site = site;
        this.providers = providers;
        if (this.providers == null) {
            this.providers = DataSourceProviderRegistry.getDefault().getDataSourceProviders();
        }

        this.setContentProvider(new ViewContentProvider());
        this.setLabelProvider(new ViewLabelProvider());
        this.setInput(DataSourceProviderRegistry.getDefault());
        this.expandAll();
        this.addSelectionChangedListener(this);
        this.addDoubleClickListener(this);
        this.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    class ViewContentProvider implements IStructuredContentProvider,
        ITreeContentProvider
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
                return ((DataSourceProviderDescriptor) parent).getEnabledDrivers().toArray();
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
            }
            return false;
        }
    }

    class ViewLabelProvider extends LabelProvider
    {

        public String getText(Object obj)
        {
            if (obj instanceof DataSourceProviderDescriptor) {
                return ((DataSourceProviderDescriptor) obj).getName();
            } else if (obj instanceof DriverDescriptor) {
                return ((DriverDescriptor) obj).getName();
            } else {
                return obj.toString();
            }
        }

        public Image getImage(Object obj)
        {
            String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			if (obj instanceof DataSourceProviderDescriptor) {
                Image icon = ((DataSourceProviderDescriptor) obj).getIcon();
                if (icon != null) {
                    return icon;
                }
			    imageKey = ISharedImages.IMG_OBJ_FOLDER;
            } else if (obj instanceof DriverDescriptor) {
                Image icon = ((DriverDescriptor) obj).getIcon();
                if (icon != null) {
                    return icon;
                }
            }

            return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
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
