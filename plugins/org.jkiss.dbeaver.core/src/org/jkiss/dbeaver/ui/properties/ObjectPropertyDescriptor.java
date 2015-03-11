/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.properties;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

/**
 * ObjectPropertyDescriptor
*/
public class ObjectPropertyDescriptor extends ObjectAttributeDescriptor implements IPropertyDescriptorEx, IPropertyValueListProvider<Object>
{
    private final Property propInfo;
    private final String propName;
    private final String propDescription;
    private Method setter;
    private ILabelProvider labelProvider;
    private IPropertyValueEditorProvider valueEditor;
    private IPropertyValueTransformer valueTransformer;
    private final Class<?> declaringClass;

    public ObjectPropertyDescriptor(
        IPropertySource source,
        ObjectPropertyGroupDescriptor parent,
        Property propInfo,
        Method getter)
    {
        super(source, parent, getter, propInfo.id(), propInfo.order());
        this.propInfo = propInfo;

        final String propertyName = BeanUtils.getPropertyNameFromGetter(getter.getName());
        declaringClass = getter.getDeclaringClass();
        Class<?> c = declaringClass;
        while (setter == null && c != Object.class && c != null) {
            this.setter = BeanUtils.getSetMethod(
                c,
                propertyName);
            if (setter == null) {
                c = c.getSuperclass();
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

        this.propName = propInfo.hidden() ? getId() : getLocalizedString(propInfo.name(), Property.RESOURCE_TYPE_NAME, getId());
        this.propDescription = CommonUtils.isEmpty(propInfo.description()) ?
                propName :
                getLocalizedString(propInfo.name(), Property.RESOURCE_TYPE_DESCRIPTION, propName);
    }

    @Override
    public Class<?> getDeclaringClass()
    {
        return declaringClass;
    }

    public boolean isViewable()
    {
        return propInfo.viewable() || propInfo.hidden();
    }

    public boolean isHidden()
    {
        return propInfo.hidden();
    }

    public boolean isExpensive()
    {
        return propInfo.expensive();
    }

    public boolean supportsPreview()
    {
        return propInfo.supportsPreview();
    }

    public IPropertyValueTransformer getValueTransformer()
    {
        return valueTransformer;
    }

    @Override
    public CellEditor createPropertyEditor(Composite parent)
    {
        IPropertySource source = getSource();
        if (source == null) {
            return null;
        }
        final Object object = source.getEditableValue();
        if (!isEditable(object)) {
            return null;
        }
        if (valueEditor != null) {
            return valueEditor.createCellEditor(parent, object, propInfo);
        } else {
            return createCellEditor(parent, object, this);
        }
    }

    @Override
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

    @Override
    public String getCategory()
    {
        return CommonUtils.isEmpty(propInfo.category()) ? null : propInfo.category();
    }

    @Override
    public String getDescription()
    {
        return propDescription;
    }

    @Override
    public String getDisplayName()
    {
        return propName;
    }

    @Override
    public String[] getFilterFlags()
    {
        return null;
    }

    @Override
    public Object getHelpContextIds()
    {
        return propInfo.helpContextId();
    }

    @Override
    public ILabelProvider getLabelProvider()
    {
        return this.labelProvider;
    }

    @Override
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
            if (object == null) {
                return null;
            }
        }
        if (isLazy()) {
            // Lazy (probably cached)
            if (isLazy(object, true) && progressMonitor == null && !supportsPreview()) {
                throw new IllegalAccessException("Lazy property can't be read with null progress monitor");
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

    @Override
    public Class<?> getDataType()
    {
        return getGetter().getReturnType();
    }

    @Override
    public boolean isRequired()
    {
        return false;
    }

    @Override
    public Object getDefaultValue()
    {
        return null;
    }

    @Override
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

    @Override
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
            if (!ArrayUtils.isEmpty(items)) {
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

    private String getLocalizedString(String string, String type, String defaultValue) {
        if (Property.DEFAULT_LOCAL_STRING.equals(string)) {
            Method getter = getGetter();
            String propertyName = BeanUtils.getPropertyNameFromGetter(getter.getName());
            Class<?> propOwner = getter.getDeclaringClass();
            Bundle bundle = FrameworkUtil.getBundle(propOwner);
            ResourceBundle resourceBundle = Platform.getResourceBundle(bundle);
            String messageID = "meta." + propOwner.getName() + "." + propertyName + "." + type;
            String result = null;
            try {
                result = resourceBundle.getString(messageID);
            } catch (Exception e) {
                // Try to find the same property in parent classes
                for (Class parent = getter.getDeclaringClass().getSuperclass(); parent != null && parent != Object.class; parent = parent.getSuperclass()) {
                    try {
                        Method parentGetter = parent.getMethod(getter.getName(), getter.getParameterTypes());
                        Class<?> parentOwner = parentGetter.getDeclaringClass();
                        Bundle parentBundle = FrameworkUtil.getBundle(parentOwner);
                        if (parentBundle == null || parentBundle == bundle) {
                            continue;
                        }
                        ResourceBundle parentResourceBundle = Platform.getResourceBundle(parentBundle);
                        messageID = "meta." + parentOwner.getName() + "." + propertyName + "." + type;
                        try {
                            result = parentResourceBundle.getString(messageID);
                            break;
                        } catch (Exception e1) {
                            // Just skip it
                        }
                    } catch (NoSuchMethodException e1) {
                        // Just skip it
                    }
                }
                if (result == null) {
                    if (type.equals(Property.RESOURCE_TYPE_NAME)) {
                        log.warn("Resource '" + messageID + "' not found in bundle " + bundle.getSymbolicName());
                    }
                    return defaultValue;
                }
            }
            if (!result.equals(messageID)) {
                return result;
            }
            return defaultValue;
        }
        return string;
    }

    private class DefaultLabelProvider extends LabelProvider implements IFontProvider {

        @Override
        public Image getImage(Object element)
        {
//            if (getSource() instanceof IPropertySourceEditable) {
//                if (isReadOnly()) {
//                    return DBIcon.BULLET_GREEN.getImage();
//                } else {
//                    return DBIcon.BULLET_BLACK.getImage();
//                }
//            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            return element == null ?
                "" :
                element instanceof DBSObject ?
                    ((DBSObject)element).getName() :
                    element.toString();
        }

        @Override
        public Font getFont(Object element)
        {
            return null;
        }
    }

}
