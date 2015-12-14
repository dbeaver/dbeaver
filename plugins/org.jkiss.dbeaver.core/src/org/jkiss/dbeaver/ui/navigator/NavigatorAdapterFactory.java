/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.navigator;

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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.properties.PropertySourceDelegate;

/**
 * Navigator AdapterFactory
 */
public class NavigatorAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = {
        DBPNamedObject.class,
        DBPQualifiedObject.class,
        DBPObject.class,
        DBSObject.class,
        DBSDataContainer.class,
        DBSDataManipulator.class,
        DBPDataSourceContainer.class,
//        DBPContextProvider.class,
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
        if (adapterType == DBPDataSourceContainer.class) {
            if (adaptableObject instanceof DBNDataSource) {
                return ((DBNDataSource)adaptableObject).getDataSourceContainer();
            }
            DBSObject object = DBUtils.getFromObject(adaptableObject);
            if (object == null) {
                return null;
            }
            if (object instanceof DBPDataSourceContainer) {
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
            if (dbObject instanceof DBPPropertySource) {
                return new PropertySourceDelegate((DBPPropertySource) dbObject);
            }
            if (dbObject instanceof IAdaptable) {
                Object adapter = ((IAdaptable) dbObject).getAdapter(IPropertySource.class);
                if (adapter != null) {
                    return adapter;
                }
                adapter = ((IAdaptable) dbObject).getAdapter(DBPPropertySource.class);
                if (adapter != null) {
                    return new PropertySourceDelegate((DBPPropertySource) adapter);
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
                return new PropertySourceDelegate(props);
            }
        } else if (adapterType == IWorkbenchAdapter.class) {
            // Workbench adapter
            if (adaptableObject instanceof DBNNode) {
                final DBNNode node = (DBNNode)adaptableObject;
                return new WorkbenchAdapter() {
                    @Override
                    public ImageDescriptor getImageDescriptor(Object object)
                    {
                        return DBeaverIcons.getImageDescriptor(node.getNodeIconDefault());
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