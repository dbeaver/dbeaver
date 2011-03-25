/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
import java.util.Collection;

/**
 * ObjectPropertyDescriptor
*/
public class ObjectPropertyDescriptor extends ObjectAttributeDescriptor implements IPropertyDescriptor
{
    private IPropertyDescriptor propertyDescriptor;
    private Property propInfo;
    private Method setter;
    private IPropertySource propertySource;

    public ObjectPropertyDescriptor(ObjectPropertyGroupDescriptor parent, Property propInfo, Method getter)
    {
        super(parent, getter, propInfo.id(), propInfo.order());
        this.propInfo = propInfo;
        this.setter = BeanUtils.getSetMethod(getter.getDeclaringClass(), getId());
    }

    public ObjectPropertyDescriptor(IPropertySource propertySource, IPropertyDescriptor propertyDescriptor, int orderNumber)
    {
        super(null, null, CommonUtils.toString(propertyDescriptor.getId()), orderNumber);
        this.propertySource = propertySource;
        this.propertyDescriptor = propertyDescriptor;
    }

/*
    public Property getPropInfo()
    {
        return propInfo;
    }
*/

    public boolean isViewable()
    {
        return propInfo == null || propInfo.viewable();
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
        if (object == null) {
            return null;
        }
        Object value;
        if (propertyDescriptor != null) {
            value = propertySource.getPropertyValue(propertyDescriptor.getId());
        } else {
            if (getParent() != null) {
                object = getParent().getGroupObject(object, progressMonitor);
            }
            if (isLazy(false)) {
                if (progressMonitor == null) {
                    throw new IllegalAccessException("Can't read lazy properties with null progress monitor");
                }
                value = getGetter().invoke(object, progressMonitor);
            } else {
                value = getGetter().invoke(object);
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
            throw new IllegalAccessError("No setter found for property " + getId());
        }
    }

    public boolean isCollectionAnno()
    {
        return Collection.class.isAssignableFrom(getGetter().getReturnType());
    }

}
