/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.BeanUtils;
import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.meta.IPropertyValueEditor;
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
    private ILabelProvider labelProvider;
    private IPropertyValueEditor valueEditor;

    public ObjectPropertyDescriptor(
        IPropertySource source,
        ObjectPropertyGroupDescriptor parent,
        Property propInfo,
        Method getter)
    {
        super(source, parent, getter, propInfo.id(), propInfo.order());
        this.propInfo = propInfo;
        this.setter = BeanUtils.getSetMethod(
            getter.getDeclaringClass(),
            BeanUtils.getPropertyNameFromGetter(getter.getName()));

        // Obtain label provider
        Class<ILabelProvider> labelProviderClass = propInfo.labelProvider();
        if (labelProviderClass != null && labelProviderClass != ILabelProvider.class) {
            try {
                this.labelProvider = labelProviderClass.newInstance();
            } catch (Throwable e) {
                log.warn(e);
            }
        }
        if (this.labelProvider == null) {
            this.labelProvider = new DefaultLabelProvider();
        }

        // Obtain value editor
        Class<IPropertyValueEditor> valueEditorClass = propInfo.valueEditor();
        if (valueEditorClass != null && valueEditorClass != IPropertyValueEditor.class) {
            try {
                valueEditor = valueEditorClass.newInstance();
            } catch (Throwable e) {
                log.warn(e);
            }
        }
        if (valueEditor == null) {
            valueEditor = new DefaultValueEditor();
        }
        //valueEditor.initEditor();
    }

    public boolean isViewable()
    {
        return propInfo == null || propInfo.viewable();
    }

    public boolean isExpensive()
    {
        return propInfo != null && propInfo.expensive();
    }

    public CellEditor createPropertyEditor(Composite parent)
    {
        final Object object = getSource().getEditableValue();
        if (!isEditable(object)) {
            return null;
        }
        return valueEditor.createCellEditor(parent, object, propInfo);
    }

    public boolean isEditable(Object object)
    {
        final IPropertySource propertySource = getSource();
        if (!(propertySource instanceof IPropertySourceEditable) || !((IPropertySourceEditable) propertySource).isEditable(object)) {
            return false;
        }
        // Read-only or non-updatable property for non-new object
        return isNewObject(object) ? propInfo.editable() : propInfo.updatable();
    }

    public boolean isEditPossible()
    {
        return propInfo.editable();
    }

    private boolean isNewObject(Object object)
    {
        if (object instanceof DBPPersistedObject) {
            return !((DBPPersistedObject)object).isPersisted();
        }
        return false;
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
        return propInfo.helpContextId();
    }

    public ILabelProvider getLabelProvider()
    {
        return this.labelProvider;
    }

    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
    {
        return anotherProperty instanceof ObjectPropertyDescriptor &&
            ((ObjectPropertyDescriptor)anotherProperty).propInfo == propInfo;
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

    @Override
    public String toString()
    {
        return getId() + " (" + propInfo.name() + ")";
    }

    private class DefaultLabelProvider extends LabelProvider implements IFontProvider {

        public Image getImage(Object element)
        {
//            if (getSource() instanceof IPropertySourceEditable) {
//                if (isEditable()) {
//                    return DBIcon.BULLET_GREEN.getImage();
//                } else {
//                    return DBIcon.BULLET_BLACK.getImage();
//                }
//            }
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

        public Font getFont(Object element)
        {
            return null;
        }
    }

    private class DefaultValueEditor implements IPropertyValueEditor {

        public CellEditor createCellEditor(Composite parent, Object object, Property property)
        {
            Class<?> propertyType = getGetter().getReturnType();
            if (CharSequence.class.isAssignableFrom(propertyType)) {
                return new TextCellEditor(parent);
            } else if (Number.class.isAssignableFrom(propertyType)) {
                return new TextCellEditor(parent);
            } else if (Boolean.class.isAssignableFrom(propertyType)) {
                return new CheckboxCellEditor(parent);
            } else {
                return null;
            }
        }

    }
}
