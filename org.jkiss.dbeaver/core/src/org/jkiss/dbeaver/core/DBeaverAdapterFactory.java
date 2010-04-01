package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.views.properties.PropertyCollector;

import java.util.Map;
import java.util.Hashtable;

/**
 * DBeaverAdapterFactory
 */
public class DBeaverAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = { IPropertySource.class, IWorkbenchAdapter.class };

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
        if (adaptableObject instanceof DBPObject) {
            if (adapterType == IPropertySource.class) {
                IPropertySource cached = propertySourceCache.get(adaptableObject);
                if (cached != null) {
                    return cached;
                }
                PropertyCollector props = new PropertyCollector((DBPObject)adaptableObject);
                props.collectProperties();
                if (props.isEmpty() && adaptableObject instanceof DBSObject) {
                    // Add default properties
                    DBSObject meta = (DBSObject)adaptableObject;
                    props.addProperty("name", "Name", meta.getName());
                    props.addProperty("desc", "Description", meta.getDescription());
                }
                return props;
            } else if (adapterType == IWorkbenchAdapter.class) {
                if (adaptableObject instanceof DBSObject) {
                    final DBSObject dbObject = (DBSObject)adaptableObject;
                    return new IWorkbenchAdapter() {

                        public Object[] getChildren(Object o)
                        {
                            return null;
                        }

                        public ImageDescriptor getImageDescriptor(Object object)
                        {
                            final DBMNode node = DBeaverCore.getInstance().getMetaModel().getNodeByObject(dbObject);
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
        }/* else if (adaptableObject instanceof ILoadService) {
            if (adapterType == IPropertySource.class) {
                return new IPropertySource() {
                    public Object getEditableValue()
                    {
                        return ((ILoadService)adaptableObject).getServiceName();
                    }
                    public IPropertyDescriptor[] getPropertyDescriptors()
                    {
                        return new IPropertyDescriptor[0];
                    }
                    public Object getPropertyValue(Object id)
                    {
                        return null;
                    }
                    public boolean isPropertySet(Object id)
                    {
                        return false;
                    }
                    public void resetPropertyValue(Object id)
                    {
                    }
                    public void setPropertyValue(Object id, Object value)
                    {
                    }
                };
            }
        }*/
        return null;
    }

    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}
