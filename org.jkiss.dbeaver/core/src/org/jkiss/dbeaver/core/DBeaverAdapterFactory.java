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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.views.properties.PropertyCollector;

import java.util.Hashtable;
import java.util.Map;

/**
 * DBeaverAdapterFactory
 */
public class DBeaverAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = { DBPNamedObject.class, DBPObject.class, DBSObject.class, IPropertySource.class, IWorkbenchAdapter.class };

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
        if (adapterType == DBPNamedObject.class || adapterType == DBPObject.class || adapterType == DBSObject.class) {
            if (adaptableObject instanceof DBNNode) {
                return ((DBNNode)adaptableObject).getObject();
            }
        } else if (adapterType == IPropertySource.class) {
            if (adaptableObject instanceof DBPObject) {
                IPropertySource cached = propertySourceCache.get(adaptableObject);
                if (cached != null) {
                    return cached;
                }
                DBPObject dbObject = (DBPObject) adaptableObject;
                if (dbObject instanceof DBNNode) {
                    dbObject = ((DBNNode)dbObject).getObject();
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
            if (adaptableObject instanceof DBSObject || adaptableObject instanceof DBNNode) {
                final DBSObject dbObject = adaptableObject instanceof DBNNode ?
                        ((DBNNode)adaptableObject).getObject() :
                        (DBSObject)adaptableObject;
                return new IWorkbenchAdapter() {

                    public Object[] getChildren(Object o)
                    {
                        return null;
                    }

                    public ImageDescriptor getImageDescriptor(Object object)
                    {
                        final DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(dbObject);
                        if (node != null) {
                            return ImageDescriptor.createFromImage(node.getNodeIconDefault());
                        } else {
                            return null;
                        }
                    }

                    public String getLabel(Object o)
                    {
                        return dbObject.getName();
                    }

                    public Object getParent(Object o)
                    {
                        return dbObject.getParentObject();
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
