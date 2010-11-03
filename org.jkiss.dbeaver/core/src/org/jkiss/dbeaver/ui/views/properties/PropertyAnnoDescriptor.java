/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.BeanUtils;
import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * PropertyAnnoDescriptor
*/
public class PropertyAnnoDescriptor implements IPropertyDescriptor
{
    private IPropertySource propertySource;
    private IPropertyDescriptor propertyDescriptor;
    private int orderNumber;
    private String id;
    private Property propInfo;
    private Method getter;
    private Method setter;
    private boolean isLazy;

    public PropertyAnnoDescriptor(Property propInfo, Method getter)
    {
        this.propInfo = propInfo;
        this.getter = getter;
        if (getter.getParameterTypes().length == 1 && getter.getParameterTypes()[0] == DBRProgressMonitor.class) {
            this.isLazy = true;
        }
        this.id = BeanUtils.getPropertyNameFromGetter(getter.getName());
        this.setter = BeanUtils.getSetMethod(getter.getDeclaringClass(), id);
    }

    public PropertyAnnoDescriptor(IPropertySource propertySource, IPropertyDescriptor propertyDescriptor, int orderNumber)
    {
        this.propertySource = propertySource;
        this.propertyDescriptor = propertyDescriptor;
        this.orderNumber = orderNumber;
    }

/*
    public Property getPropInfo()
    {
        return propInfo;
    }
*/

    public int getOrder()
    {
        return propInfo == null ? orderNumber : propInfo.order();
    }

    public boolean isViewable()
    {
        return propInfo == null || propInfo.viewable();
    }

    public boolean isLazy()
    {
        return isLazy;
    }

    public CellEditor createPropertyEditor(Composite parent)
    {
        if (propertyDescriptor != null) {
            return propertyDescriptor.createPropertyEditor(parent);
        } else {
            return null;
        }
    }

    public String getCategory()
    {
        if (propertyDescriptor != null) {
            return propertyDescriptor.getCategory();
        } else {
            return CommonUtils.isEmpty(propInfo.category()) ? null : propInfo.category();
        }
    }

    public String getDescription()
    {
        if (propertyDescriptor != null) {
            return propertyDescriptor.getDescription();
        } else {
            return CommonUtils.isEmpty(propInfo.description()) ? null : propInfo.description();
        }
    }

    public String getDisplayName()
    {
        if (propertyDescriptor != null) {
            return propertyDescriptor.getDisplayName();
        } else {
            return propInfo.name();
        }
    }

    public String[] getFilterFlags()
    {
        if (propertyDescriptor != null) {
            return propertyDescriptor.getFilterFlags();
        } else {
            return null;
        }
    }

    public Object getHelpContextIds()
    {
        if (propertyDescriptor != null) {
            return propertyDescriptor.getHelpContextIds();
        } else {
            return null;
        }
    }

    public Object getId()
    {
        if (propertyDescriptor != null) {
            return propertyDescriptor.getId();
        } else {
            return id;
        }
    }

    public ILabelProvider getLabelProvider()
    {
        if (propertyDescriptor != null) {
            return propertyDescriptor.getLabelProvider();
        } else {
            return new LabelProvider() {

                public Image getImage(Object element)
                {
/*
                    if (element instanceof DBSObject) {
                        DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject((DBSObject) element, true);
                        if (node != null) {
                            return node.getNodeIconDefault();
                        }
                    }
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
*/
                    return null;
                }

                public String getText(Object element)
                {
                    return element == null ?
                        "" :
                        element instanceof DBSObject ?
                            ((DBSObject)element).getName() :
                            element.toString();
                }
            };
        }
    }

    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
    {
        return propertyDescriptor != null && propertyDescriptor.isCompatibleWith(anotherProperty);
    }

    public Object readValue(Object object, DBRProgressMonitor progressMonitor)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Object value;
        if (propertyDescriptor != null) {
            value = propertySource.getPropertyValue(propertyDescriptor.getId());
        } else {
            if (isLazy) {
                if (progressMonitor == null) {
                    throw new IllegalAccessException("Can't read lazy poperties with null progress monitor");
                }
                value = getter.invoke(object, progressMonitor);
            } else {
                value = getter.invoke(object);
            }
        }
        return value;
    }

    public void writeValue(Object object, Object value)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if (propertyDescriptor != null) {
            propertySource.setPropertyValue(propertyDescriptor.getId(), value);
        } else if (setter != null) {
            setter.invoke(object, value);
        } else {
            throw new IllegalAccessError("No setter found for property " + id);
        }
    }

    public static List<PropertyAnnoDescriptor> extractProperties(IPropertySource propertySource)
    {
        List<PropertyAnnoDescriptor> annoProps = new ArrayList<PropertyAnnoDescriptor>();
        IPropertyDescriptor[] descs = propertySource.getPropertyDescriptors();
        for (int i = 0; i < descs.length; i++) {
            IPropertyDescriptor descriptor = descs[i];
            annoProps.add(new PropertyAnnoDescriptor(propertySource, descriptor, i));
        }
        return annoProps;
    }

    public static List<PropertyAnnoDescriptor> extractAnnotations(Object object)
    {
        return extractAnnotations(object.getClass());
    }
    
    public static List<PropertyAnnoDescriptor> extractAnnotations(Class<?> theClass)
    {
        Method[] methods = theClass.getMethods();
        List<PropertyAnnoDescriptor> annoProps = new ArrayList<PropertyAnnoDescriptor>();
        for (Method method : methods) {
            final Property propInfo = method.getAnnotation(Property.class);
            if (propInfo == null || !BeanUtils.isGetterName(method.getName()) || method.getReturnType() == null) {
                continue;
            }
            PropertyAnnoDescriptor desc = new PropertyAnnoDescriptor(propInfo, method);
            annoProps.add(desc);
        }
        Collections.sort(annoProps, new Comparator<PropertyAnnoDescriptor>()
        {
            public int compare(PropertyAnnoDescriptor o1, PropertyAnnoDescriptor o2)
            {
                return o1.getOrder() - o2.getOrder();
            }
        });
        return annoProps;
    }

    public boolean isCollectionAnno()
    {
        return Collection.class.isAssignableFrom(getter.getReturnType());
    }
}
