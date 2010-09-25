/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.views.properties.PropertyCollector;

import java.util.Hashtable;
import java.util.Map;

/**
 * Navigator AdapterFactory
 */
public class DBNAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = { DBPNamedObject.class, DBSEntityQualified.class, DBPObject.class, DBSObject.class, DBSDataContainer.class, DBSDataSourceContainer.class, IPropertySource.class, IWorkbenchAdapter.class };

    private Map<Object, IPropertySource> propertySourceCache = new Hashtable<Object, IPropertySource>();

    public void addToCache(Object object, IPropertySource adapter)
    {
        propertySourceCache.put(object, adapter);
    }

    public void removeFromCache(Object object)
    {
        propertySourceCache.remove(object);
    }

    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
        if (!(adaptableObject instanceof DBNNode)) {
            return null;
        }
        final DBNNode node = (DBNNode) adaptableObject;
        if (adapterType == DBSDataSourceContainer.class) {
            if (node instanceof DBNDataSource) {
                return ((DBNDataSource)node).getDataSourceContainer();
            }
            DBSObject object = node.getObject();
            if (object == null) {
                return null;
            }
            if (object instanceof DBSDataSourceContainer) {
                return object;
            }
            DBPDataSource dataSource = object.getDataSource();
            return dataSource == null ? null : dataSource.getContainer();
        } else if (DBPObject.class.isAssignableFrom(adapterType)) {
            DBSObject object = node.getObject();
            if (object != null && adapterType.isAssignableFrom(object.getClass())) {
                return object;
            }
        } else if (adapterType == IPropertySource.class) {
            DBPObject dbObject = node.getObject();
            if (dbObject instanceof IAdaptable) {
                Object adapter = ((IAdaptable) dbObject).getAdapter(adapterType);
                if (adapter != null) {
                    return adapter;
                }
            }
            if (dbObject != null) {
                IPropertySource cached = propertySourceCache.get(dbObject);
                if (cached != null) {
                    return cached;
                }
                PropertyCollector props = new PropertyCollector(dbObject , true);
                props.collectProperties();
                if (props.isEmpty() && adaptableObject instanceof DBSObject) {
                    // Add default properties
                    DBSObject meta = (DBSObject)adaptableObject;
                    props.addProperty("name", "Name", meta.getName());
                    props.addProperty("desc", "Description", meta.getDescription());
                }
                return props;
            }
        } else if (adapterType == IWorkbenchAdapter.class) {
            // Workbench adapter
            return new IWorkbenchAdapter() {

                public Object[] getChildren(Object o)
                {
                    return null;
                }

                public ImageDescriptor getImageDescriptor(Object object)
                {
                    return ImageDescriptor.createFromImage(node.getNodeIconDefault());
                }

                public String getLabel(Object o)
                {
                    return node.getNodeName();
                }

                public Object getParent(Object o)
                {
                    return node.getParentNode();
                }
            };
        }
        return null;
    }

    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}