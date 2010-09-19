/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.views.properties.PropertyCollector;

import java.util.Hashtable;
import java.util.Map;

/**
 * DBeaverAdapterFactory
 */
public class DBeaverAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = { DBPNamedObject.class, DBPObject.class, DBSObject.class, DBSDataContainer.class, IPropertySource.class, IWorkbenchAdapter.class };

    private Map<Object, IPropertySource> propertySourceCache = new Hashtable<Object, IPropertySource>();

    public void addToCache(Object object, IPropertySource adapter)
    {
        propertySourceCache.put(object, adapter);
    }

    public void removeFromCache(Object object)
    {
        propertySourceCache.remove(object);
    }

    public Object getAdapter(final Object adaptableObject, Class adapterType)
    {
        if (DBPObject.class.isAssignableFrom(adapterType)) {
            if (adaptableObject instanceof DBNNode) {
                DBSObject object = ((DBNNode) adaptableObject).getObject();
                if (object != null && adapterType.isAssignableFrom(object.getClass())) {
                    return object;
                }
            }
        } else if (adapterType == IPropertySource.class) {
            DBPObject dbObject = null;
            if (adaptableObject instanceof DBPObject) {
                dbObject = (DBPObject)adaptableObject;
            } else if (adaptableObject instanceof DBNNode) {
                dbObject = ((DBNNode)adaptableObject).getObject();
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
            if (adaptableObject instanceof DBNNode) {
                final DBNNode node = (DBNNode)adaptableObject;
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
        }
        return null;
    }

    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}
