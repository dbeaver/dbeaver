/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;

/**
 * Navigator AdapterFactory
 */
public class DBNAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = {
        DBPNamedObject.class,
        DBPQualifiedObject.class,
        DBPObject.class,
        DBSObject.class,
        DBSDataContainer.class,
        DBSDataManipulator.class,
        DBSDataSourceContainer.class,
        IPropertySource.class,
        IProject.class,
        IFolder.class,
        IFile.class,
        IResource.class,
        IWorkbenchAdapter.class
    };

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
        if (adapterType == DBSDataSourceContainer.class) {
            if (adaptableObject instanceof DBNDataSource) {
                return ((DBNDataSource)adaptableObject).getDataSourceContainer();
            }
            DBSObject object = null;
            if (adaptableObject instanceof DBSWrapper) {
                object = ((DBSWrapper) adaptableObject).getObject();
            } else if (adaptableObject instanceof DBSObject) {
                object = (DBSObject) adaptableObject;
            }
            if (object == null) {
                return null;
            }
            if (object instanceof DBSDataSourceContainer) {
                return object;
            }
            DBPDataSource dataSource = object.getDataSource();
            return dataSource == null ? null : dataSource.getContainer();
        } else if (DBPObject.class.isAssignableFrom(adapterType)) {
            DBPObject object = null;
            if (adaptableObject instanceof DBSWrapper) {
                object = ((DBSWrapper) adaptableObject).getObject();
            } else if (adaptableObject instanceof DBPObject) {
                object = (DBPObject) adaptableObject;
            }
            if (object != null && adapterType.isAssignableFrom(object.getClass())) {
                return object;
            }
        } else if (IResource.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof DBNResource) {
                IResource resource = ((DBNResource) adaptableObject).getResource();
                if (resource != null && adapterType.isAssignableFrom(resource.getClass())) {
                    return resource;
                } else {
                    return null;
                }
            }
        } else if (adapterType == IPropertySource.class) {
            DBPObject dbObject = null;
            if (adaptableObject instanceof DBSWrapper) {
                dbObject = ((DBSWrapper) adaptableObject).getObject();
            } else if (adaptableObject instanceof DBPObject) {
                dbObject = (DBPObject) adaptableObject;
            }
            if (dbObject instanceof IPropertySource) {
                return dbObject;
            }
            if (dbObject instanceof IAdaptable) {
                Object adapter = ((IAdaptable) dbObject).getAdapter(IPropertySource.class);
                if (adapter != null) {
                    return adapter;
                }
            }
            if (dbObject != null) {
                PropertyCollector props = new PropertyCollector(adaptableObject, dbObject , true);
                props.collectProperties();
                if (props.isEmpty() && adaptableObject instanceof DBSObject) {
                    // Add default properties
                    DBSObject meta = (DBSObject)adaptableObject;
                    props.addProperty(null, "name", CoreMessages.model_navigator_Name, meta.getName()); //$NON-NLS-1$
                    props.addProperty(null, "desc", CoreMessages.model_navigator_Description, meta.getDescription()); //$NON-NLS-1$
                }
                return props;
            }
        } else if (adapterType == IWorkbenchAdapter.class) {
            // Workbench adapter
            if (adaptableObject instanceof DBNNode) {
                final DBNNode node = (DBNNode)adaptableObject;
                return new WorkbenchAdapter() {
                    @Override
                    public ImageDescriptor getImageDescriptor(Object object)
                    {
                        return ImageDescriptor.createFromImage(node.getNodeIconDefault());
                    }

                    @Override
                    public String getLabel(Object o)
                    {
                        return node.getNodeName();
                    }

                    @Override
                    public Object getParent(Object o)
                    {
                        return node.getParentNode();
                    }
                };
            } else {
                return null;
            }
        }
        return null;
    }

    @Override
    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}