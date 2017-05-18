/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.runtime.properties;

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * ObjectPropertyDescriptor
*/
public class ObjectPropertyDescriptor extends ObjectAttributeDescriptor implements DBPPropertyDescriptor, IPropertyValueListProvider<Object>
{
    private final Property propInfo;
    private final String propName;
    private final String propDescription;
    private Method setter;
    private IPropertyValueTransformer valueTransformer;
    private IPropertyValueTransformer valueRenderer;
    private final Class<?> declaringClass;
    private Format displayFormat = null;

    public ObjectPropertyDescriptor(
        DBPPropertySource source,
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

        // Obtain value transformer
        Class<? extends IPropertyValueTransformer> valueTransformerClass = propInfo.valueTransformer();
        if (valueTransformerClass != null && valueTransformerClass != IPropertyValueTransformer.class) {
            try {
                valueTransformer = valueTransformerClass.newInstance();
            } catch (Throwable e) {
                log.warn("Can't create value transformer", e);
            }
        }

        // Obtain value transformer
        Class<? extends IPropertyValueTransformer> valueRendererClass = propInfo.valueRenderer();
        if (valueRendererClass != null && valueRendererClass != IPropertyValueTransformer.class) {
            try {
                valueRenderer = valueRendererClass.newInstance();
            } catch (Throwable e) {
                log.warn("Can't create value renderer", e);
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
        return propInfo.viewable() && !propInfo.hidden();
    }

    public boolean isHidden()
    {
        return propInfo.hidden();
    }

    public boolean isExpensive()
    {
        return propInfo.expensive();
    }

    public boolean isNumeric()
    {
        Class<?> propType = getGetter().getReturnType();
        return propType != null && BeanUtils.isNumericType(propType);
    }

    public boolean isDateTime()
    {
        Class<?> propType = getGetter().getReturnType();
        return propType != null && Date.class.isAssignableFrom(propType);
    }

    public boolean supportsPreview()
    {
        return propInfo.supportsPreview();
    }

    public IPropertyValueTransformer getValueTransformer()
    {
        return valueTransformer;
    }

    public IPropertyValueTransformer getValueRenderer() {
        return valueRenderer;
    }

    @Override
    public boolean isEditable(Object object)
    {
        final DBPPropertySource propertySource = getSource();
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

    @NotNull
    @Override
    public String getDisplayName()
    {
        return propName;
    }

    public Format getDisplayFormat() {
        if (displayFormat == null) {
            final String format = propInfo.format();
            if (format == null || format.isEmpty()) {
                return null;
            }
            if (isNumeric()) {
                displayFormat = new DecimalFormat(format);
            } else if (isDateTime()) {
                displayFormat = new SimpleDateFormat(format);
            } else {
                log.debug("Don't know how to apply format to property " + getId());
                displayFormat = null;
            }
        }
        return displayFormat;
    }

    public Object readValue(Object object, @Nullable DBRProgressMonitor progressMonitor)
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
        if (valueRenderer != null) {
            value = valueRenderer.transform(object, value);
        }
        if (value instanceof Number) {
            final Format displayFormat = getDisplayFormat();
            if (displayFormat != null) {
                return displayFormat.format(value);
            }
        }
        return value;
    }

    public void writeValue(Object object, Object value)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        if (setter != null) {
            // Check for null
            if (value == null) {
                Annotation[] valueAnnotations = setter.getParameterAnnotations()[0];
                for (Annotation va : valueAnnotations) {
                    if (va.annotationType() == NotNull.class) {
                        throw new IllegalArgumentException("Property '" + getId()  + "' can't be set into NULL");
                    }
                }
            }
            if (getParent() != null) {
                // Use void monitor because this object already read by readValue
                object = getParent().getGroupObject(object, new VoidProgressMonitor());
            }
            if (value == null) {
                // Check for primitive argument
                final Class<?> argType = setter.getParameterTypes()[0];
                if (argType == Integer.TYPE) {
                    value = 0;
                } else if (argType == Short.TYPE) {
                    value = (short)0;
                } else if (argType == Long.TYPE) {
                    value = 0l;
                } else if (argType == Float.TYPE) {
                    value = (float)0.0;
                } else if (argType == Double.TYPE) {
                    value = 0.0;
                } else if (argType == Boolean.TYPE) {
                    value = false;
                } else if (argType == Character.TYPE) {
                    value = ' ';
                }
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
                        log.debug("Resource '" + messageID + "' not found in bundle " + bundle.getSymbolicName());
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


}
