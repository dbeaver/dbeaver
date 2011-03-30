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
    private Property propInfo;
    private Method setter;

    public ObjectPropertyDescriptor(ObjectPropertyGroupDescriptor parent, Property propInfo, Method getter)
    {
        super(parent, getter, propInfo.id(), propInfo.order());
        this.propInfo = propInfo;
        this.setter = BeanUtils.getSetMethod(getter.getDeclaringClass(), getId());
    }

    public boolean isViewable()
    {
        return propInfo == null || propInfo.viewable();
    }

    public CellEditor createPropertyEditor(Composite parent)
    {
        return null;
    }

    public String getCategory()
    {
        return CommonUtils.isEmpty(propInfo.category()) ? null : propInfo.category();
    }

    public String getDescription()
    {
        return CommonUtils.isEmpty(propInfo.description()) ? null : propInfo.description();
    }

    public String getDisplayName()
    {
        return propInfo.name();
    }

    public String[] getFilterFlags()
    {
        return null;
    }

    public Object getHelpContextIds()
    {
        return null;
    }

    public ILabelProvider getLabelProvider()
    {
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

    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
    {
        return false;
    }

    public Object readValue(Object object, DBRProgressMonitor progressMonitor)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if (object == null) {
            return null;
        }
        Object value;
        if (getParent() != null) {
            object = getParent().getGroupObject(object, progressMonitor);
        }
        if (isLazy(object, false)) {
            if (progressMonitor == null) {
                throw new IllegalAccessException("Can't read lazy properties with null progress monitor");
            }
            value = getGetter().invoke(object, progressMonitor);
        } else {
            value = getGetter().invoke(object);
        }
        return value;
    }

    public void writeValue(Object object, Object value)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if (setter != null) {
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
