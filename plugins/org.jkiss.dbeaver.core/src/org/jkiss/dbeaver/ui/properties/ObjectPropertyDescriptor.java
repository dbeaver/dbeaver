/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.meta.IPropertyValueEditorProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.controls.CustomCheckboxCellEditor;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.CustomNumberCellEditor;
import org.jkiss.dbeaver.ui.controls.CustomTextCellEditor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ObjectPropertyDescriptor
*/
public class ObjectPropertyDescriptor extends ObjectAttributeDescriptor implements IPropertyDescriptorEx, IPropertyValueListProvider<Object>
{
    private Property propInfo;
    private Method setter;
    private ILabelProvider labelProvider;
    private IPropertyValueEditorProvider valueEditor;
    private IPropertyValueTransformer valueTransformer;

    public ObjectPropertyDescriptor(
        IPropertySource source,
        ObjectPropertyGroupDescriptor parent,
        Property propInfo,
        Method getter)
    {
        super(source, parent, getter, propInfo.id(), propInfo.order());
        this.propInfo = propInfo;
        final String propertyName = BeanUtils.getPropertyNameFromGetter(getter.getName());
        Class<?> searchClass = getter.getDeclaringClass();
        while (setter == null && searchClass != Object.class) {
            this.setter = BeanUtils.getSetMethod(
                searchClass,
                propertyName);
            if (setter == null) {
                searchClass = searchClass.getSuperclass();
            }
        }

        // Obtain label provider
        Class<? extends ILabelProvider> labelProviderClass = propInfo.labelProvider();
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
        Class<? extends IPropertyValueEditorProvider> valueEditorClass = propInfo.valueEditor();
        if (valueEditorClass != null && valueEditorClass != IPropertyValueEditorProvider.class) {
            try {
                valueEditor = valueEditorClass.newInstance();
            } catch (Throwable e) {
                log.warn("Can't create value editor", e);
            }
        }

        // Obtain value transformer
        Class<? extends IPropertyValueTransformer> valueTransformerClass = propInfo.valueTransformer();
        if (valueTransformerClass != null && valueTransformerClass != IPropertyValueTransformer.class) {
            try {
                valueTransformer = valueTransformerClass.newInstance();
            } catch (Throwable e) {
                log.warn("Can't create value transformer", e);
            }
        }
    }

    public boolean isViewable()
    {
        return propInfo == null || propInfo.viewable() || propInfo.hidden();
    }

    public boolean isHidden()
    {
        return propInfo != null && propInfo.hidden();
    }

    public boolean isExpensive()
    {
        return propInfo != null && propInfo.expensive();
    }

    public IPropertyValueTransformer getValueTransformer()
    {
        return valueTransformer;
    }

    public CellEditor createPropertyEditor(Composite parent)
    {
        final Object object = getSource().getEditableValue();
        if (!isEditable(object)) {
            return null;
        }
        if (valueEditor != null) {
            return valueEditor.createCellEditor(parent, object, propInfo);
        } else {
            return createCellEditor(parent, object, this);
        }
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
        return object instanceof DBPPersistedObject && !((DBPPersistedObject) object).isPersisted();
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
        } else if (isLazy()) {
            // Lazy but cached
            value = getGetter().invoke(object, VoidProgressMonitor.INSTANCE);
        } else {
            value = getGetter().invoke(object);
        }
        return value;
    }

    public void writeValue(Object object, Object value)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if (setter != null) {
            if (getParent() != null) {
                // Use void monitor because this object already read by readValue
                object = getParent().getGroupObject(object, VoidProgressMonitor.INSTANCE);
            }
            setter.invoke(object, value);
        } else {
            throw new IllegalAccessError("No setter found for property " + getId());
        }
    }

    @Override
    public String toString()
    {
        return getId() + " (" + propInfo.name() + ")";
    }

    public Class<?> getDataType()
    {
        return getGetter().getReturnType();
    }

    public boolean isRequired()
    {
        return false;
    }

    public Object getDefaultValue()
    {
        return null;
    }

    public boolean allowCustomValue()
    {
        if (propInfo.listProvider() != IPropertyValueListProvider.class) {
            // List
            try {
                return propInfo.listProvider().newInstance().allowCustomValue();
            } catch (Exception e) {
                log.error(e);
            }
        }
        return false;
    }

    public Object[] getPossibleValues(Object object)
    {
        if (propInfo.listProvider() != IPropertyValueListProvider.class) {
            // List
            try {
                return propInfo.listProvider().newInstance().getPossibleValues(object);
            } catch (Exception e) {
                log.error(e);
            }
        } else if (getDataType().isEnum()) {
            return getDataType().getEnumConstants();
        }
        return null;
    }

    public static CellEditor createCellEditor(Composite parent, Object object, IPropertyDescriptorEx property)
    {
        // List
        if (property instanceof IPropertyValueListProvider) {
            final IPropertyValueListProvider listProvider = (IPropertyValueListProvider) property;
            final Object[] items = listProvider.getPossibleValues(object);
            if (!CommonUtils.isEmpty(items)) {
                final String[] strings = new String[items.length];
                for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                    strings[i] = items[i] instanceof DBPNamedObject ? ((DBPNamedObject)items[i]).getName() : CommonUtils.toString(items[i]);
                }
                final CustomComboBoxCellEditor editor = new CustomComboBoxCellEditor(
                    parent,
                    strings,
                    SWT.DROP_DOWN | (listProvider.allowCustomValue() ? SWT.NONE : SWT.READ_ONLY));
                return editor;
            }
        }
        Class<?> propertyType = property.getDataType();
        if (propertyType == null || CharSequence.class.isAssignableFrom(propertyType)) {
            return new CustomTextCellEditor(parent);
        } else if (BeanUtils.isNumericType(propertyType)) {
            return new CustomNumberCellEditor(parent, propertyType);
        } else if (BeanUtils.isBooleanType(propertyType)) {
            return new CustomCheckboxCellEditor(parent);
            //return new CheckboxCellEditor(parent);
        } else if (propertyType.isEnum()) {
            final Object[] enumConstants = propertyType.getEnumConstants();
            final String[] strings = new String[enumConstants.length];
            for (int i = 0, itemsLength = enumConstants.length; i < itemsLength; i++) {
                strings[i] = ((Enum)enumConstants[i]).name();
            }
            return new CustomComboBoxCellEditor(
                parent,
                strings,
                SWT.DROP_DOWN | SWT.READ_ONLY);
        } else {
            log.warn("Unsupported property type: " + propertyType.getName());
            return null;
        }
    }

    @Override
    public int hashCode()
    {
        return propInfo.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof ObjectPropertyDescriptor &&
            propInfo.equals(((ObjectPropertyDescriptor)obj).propInfo) &&
            CommonUtils.equalObjects(getGetter(), ((ObjectPropertyDescriptor)obj).getGetter());
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

}
