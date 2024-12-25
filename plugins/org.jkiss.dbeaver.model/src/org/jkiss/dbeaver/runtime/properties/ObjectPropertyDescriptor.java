/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.dpi.DPIClientObject;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNodeReference;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * ObjectPropertyDescriptor
*/
public class ObjectPropertyDescriptor extends ObjectAttributeDescriptor implements DBPPropertyDescriptor, IPropertyValueListProvider<Object>
{

    private final Property propInfo;
    private final String propName;
    private final String propDescription;
    private final String propHint;
    private Method setter;
    private IPropertyValueTransformer valueTransformer;
    private IPropertyValueTransformer valueRenderer;
    private IPropertyValueValidator valueValidator;
    private final Class<?> declaringClass;
    private Format displayFormat = null;
    private IPropertyValueTransformer labelProvider;

    public ObjectPropertyDescriptor(
        DBPPropertySource source,
        ObjectPropertyGroupDescriptor parent,
        Property propInfo,
        Method getter,
        String locale)
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
        if (valueTransformerClass != IPropertyValueTransformer.class) {
            try {
                valueTransformer = valueTransformerClass.getConstructor().newInstance();
            } catch (Throwable e) {
                log.warn("Can't create value transformer", e);
            }
        }

        // Obtain value render
        Class<? extends IPropertyValueTransformer> valueRendererClass = propInfo.valueRenderer();
        if (valueRendererClass != IPropertyValueTransformer.class) {
            try {
                valueRenderer = valueRendererClass.getConstructor().newInstance();
            } catch (Throwable e) {
                log.warn("Can't create value renderer", e);
            }
        }

        // Obtain value validator
        Class<? extends IPropertyValueValidator> valueValidatorClass = propInfo.valueValidator();
        if (valueValidatorClass != IPropertyValueValidator.class) {
            try {
                valueValidator = valueValidatorClass.getConstructor().newInstance();
            } catch (Throwable e) {
                log.warn("Can't create value validator", e);
            }
        }

        // Obtain label provider
        Class<? extends IPropertyValueTransformer> labelProviderClass = propInfo.labelProvider();
        labelProvider = null;
        if (labelProviderClass != IPropertyValueTransformer.class) {
            try {
                labelProvider = labelProviderClass.getConstructor().newInstance();
            } catch (Throwable e) {
                log.warn("Can't create label provider", e);
            }
        }

        this.propName = getLocalizedString(propInfo.name(), Property.RESOURCE_TYPE_NAME, getId(), !propInfo.hidden(), locale);
        this.propDescription = CommonUtils.isEmpty(propInfo.description()) ?
                propName :
                getLocalizedString(propInfo.name(), Property.RESOURCE_TYPE_DESCRIPTION, propName, false, locale);
        this.propHint = CommonUtils.isEmpty(propInfo.hint()) ?
            null :
            getLocalizedString(propInfo.name(), Property.RESOURCE_TYPE_HINT, propName, false, locale);
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

    public boolean isNumeric() {
        Class<?> propType = getGetter().getReturnType();
        return propType != null && BeanUtils.isNumericType(propType);
    }

    public boolean isDateTime() {
        Class<?> propType = getGetter().getReturnType();
        return propType != null && Date.class.isAssignableFrom(propType);
    }

    public boolean isBoolean() {
        Class<?> propType = getGetter().getReturnType();
        return propType == Boolean.class || propType == Boolean.TYPE;
    }

    @NotNull
    public PropertyLength getLength() {
        return propInfo.length();
    }

    public boolean isSpecific() {
        return propInfo.specific();
    }

    public boolean isOptional() {
        return propInfo.optional();
    }

    public boolean isLinkPossible() {
        return propInfo.linkPossible();
    }

    public boolean isHref() {
        return propInfo.href();
    }

    public boolean supportsPreview() {
        return propInfo.supportsPreview();
    }

    public boolean isPassword() {
        return propInfo.password();
    }

    public String getKeyName() {
        return propInfo.keyName();
    }

    public boolean isNonSecuredProperty() {
        return propInfo.nonSecuredProperty();
    }

    public IPropertyValueTransformer getValueTransformer() {
        return valueTransformer;
    }

    public IPropertyValueTransformer getValueRenderer() {
        return valueRenderer;
    }

    public IPropertyValueValidator getValueValidator() {
        return valueValidator;
    }

    public boolean isPropertyVisible(Object object, Object value) {
        Class<? extends IPropertyValueValidator> visiblityCheckerClass = propInfo.visibleIf();
        if (visiblityCheckerClass != IPropertyValueValidator.class) {
            try {
                IPropertyValueValidator checker = visiblityCheckerClass.getConstructor().newInstance();
                return checker.isValidValue(object, value);
            } catch (Throwable e) {
                log.debug(e);
            }
        }
        return true;
    }

    @Override
    public boolean isEditable(Object object)
    {
        final DBPPropertySource propertySource = getSource();
        if (!(propertySource instanceof IPropertySourceEditable) || !((IPropertySourceEditable) propertySource).isEditable(object)) {
            return false;
        }
        // Read-only or non-updatable property for non-new object
        return getEditableValue(object);
    }

    @Nullable
    @Override
    public String[] getFeatures() {

        List<String> features = Arrays.stream(propInfo.features())
            .collect(Collectors.toList());

        if (this.isRequired()) features.add(DBConstants.PROP_FEATURE_REQUIRED);
        if (this.isSpecific()) features.add(DBConstants.PROP_FEATURE_SPECIFIC);
        if (this.isOptional()) features.add(DBConstants.PROP_FEATURE_OPTIONAL);
        if (this.isHidden()) features.add(DBConstants.PROP_FEATURE_HIDDEN);
        if (this.isRemote()) features.add(DBConstants.PROP_FEATURE_REMOTE);

        if (this.isDateTime()) features.add(DBConstants.PROP_FEATURE_DATETME);
        if (this.isNumeric()) features.add(DBConstants.PROP_FEATURE_NUMERIC);
        if (this.isNameProperty()) features.add(DBConstants.PROP_FEATURE_NAME);

        if (this.getLength() == PropertyLength.MULTILINE) features.add(DBConstants.PROP_FEATURE_MULTILINE);
        if (this.isExpensive()) features.add(DBConstants.PROP_FEATURE_EXPENSIVE);
        if (this.isEditPossible()) features.add(DBConstants.PROP_FEATURE_EDIT_POSSIBLE);
        if (this.isLinkPossible()) features.add(DBConstants.PROP_FEATURE_LINK_POSSIBLE);
        if (this.isHref()) features.add(DBConstants.PROP_FEATURE_HREF);
        if (this.isViewable()) features.add(DBConstants.PROP_FEATURE_VIEWABLE);
        if (this.isPassword()) features.add(DBConstants.PROP_FEATURE_PASSWORD);
        if (this.isNonSecuredProperty()) features.add(DBConstants.PROP_FEATURE_NON_SECURED);

        return features.toArray(new String[0]);
    }

    @Override
    public boolean hasFeature(@NotNull String feature) {

        switch (feature) {
            case DBConstants.PROP_FEATURE_REQUIRED:
                return this.isRequired();
            case DBConstants.PROP_FEATURE_SPECIFIC:
                return this.isSpecific();
            case DBConstants.PROP_FEATURE_OPTIONAL:
                return this.isOptional();
            case DBConstants.PROP_FEATURE_HIDDEN:
                return this.isHidden();

            case DBConstants.PROP_FEATURE_DATETME:
                return this.isDateTime();
            case DBConstants.PROP_FEATURE_NUMERIC:
                return this.isNumeric();
            case DBConstants.PROP_FEATURE_NAME:
                return this.isNameProperty();

            case DBConstants.PROP_FEATURE_MULTILINE:
                return this.getLength() == PropertyLength.MULTILINE;
            case DBConstants.PROP_FEATURE_EXPENSIVE:
                return this.isExpensive();
            case DBConstants.PROP_FEATURE_EDIT_POSSIBLE:
                return this.isEditPossible();
            case DBConstants.PROP_FEATURE_LINK_POSSIBLE:
                return this.isLinkPossible();
            case DBConstants.PROP_FEATURE_HREF:
                return this.isHref();
            case DBConstants.PROP_FEATURE_VIEWABLE:
                return this.isViewable();
            case DBConstants.PROP_FEATURE_PASSWORD:
                return this.isPassword();
            case DBConstants.PROP_FEATURE_NON_SECURED:
                return this.isNonSecuredProperty();
        }

        return ArrayUtils.contains(propInfo.features(), feature);
    }

    private boolean getEditableValue(Object object)
    {
        boolean isNew = isNewObject(object);
        String expr = isNew ? propInfo.editableExpr() : propInfo.updatableExpr();
        if (!expr.isEmpty()) {
            return Boolean.TRUE.equals(evaluateExpression(object, expr));
        } else {
            return isNew ? propInfo.editable() : propInfo.updatable();
        }
    }

    private Object evaluateExpression(Object object, String exprString) {
        return AbstractDescriptor.evalExpression(exprString, object, this);
    }

    public boolean isEditPossible()
    {
        return propInfo.editable();
    }

    private boolean isNewObject(Object object)
    {
        return object instanceof DBPPersistedObject && !((DBPPersistedObject) object).isPersisted();
    }

    public boolean isEditPossible(Object context)
    {
        String expr = propInfo.editableExpr();
        if (!CommonUtils.isEmpty(expr)) {
            return Boolean.TRUE.equals(evaluateExpression(context, expr));
        }
        return propInfo.editable();
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
    public String getHint()
    {
        return propHint;
    }

    @NotNull
    @Override
    public String getDisplayName()
    {
        if (labelProvider != null) {
            Object editableValue = getSource().getEditableValue();
            if (editableValue == null) {
                if (getSource() instanceof DBNNodeReference nodeReference &&
                    nodeReference.getReferencedNode() instanceof DBNDatabaseNode dbNode) {
                    editableValue = dbNode.getObject();
                }
            }
            if (editableValue != null) {
                String propLabel = CommonUtils.toString(labelProvider.transform(editableValue, null), null);
                if (propLabel != null) {
                    return propLabel;
                }
            }
        }
        return propName;
    }

    public Format getDisplayFormat() {
        if (displayFormat == null) {
            Class<? extends Format> formatClass = propInfo.formatter();
            if (formatClass != Format.class) {
                try {
                    displayFormat = formatClass.getConstructor().newInstance();
                } catch (Throwable e) {
                    log.error(e);
                }
            }
        }
        if (displayFormat == null) {
            final String format = propInfo.format();
            if (format.isEmpty()) {
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

    public Object readValue(Object object, @Nullable DBRProgressMonitor progressMonitor, boolean formatValue)
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
        if (object instanceof DPIClientObject) {
            log.debug("Read DPI property " + getId());
        }

        Method getter = getGetter();
        Object[] params = getter.getParameterCount() > 0 ?
            new Object[getter.getParameterCount()] : null;
        Object finalObject = object;


        InvocationSupplier<Object> readPropertyMethod;
        if (object instanceof DPIClientObject dpiObject) {
            readPropertyMethod = () -> {
                try {
                    return dpiObject.dpiPropertyValue(progressMonitor, getId());
                } catch (DBException e) {
                    throw new InvocationTargetException(e, e.getMessage());
                }
            };
        } else {
            readPropertyMethod = () -> {
                try {
                    return getGetter().invoke(finalObject, params);
                } catch (Exception e) {
                    throw new InvocationTargetException(e, e.getMessage());
                }
            };
        }

        if (isLazy() && params != null) {
            // Lazy (probably cached)
            if (isLazy(object, true) && progressMonitor == null && !supportsPreview()) {
                throw new IllegalAccessException("Lazy property can't be read with null progress monitor");
            }
            params[0] = progressMonitor;
        }
        if (progressMonitor != null && isLazy() && object instanceof DBSObject) {
            Object[] finalResult = new Object[1];
            try {
                DBExecUtils.tryExecuteRecover(progressMonitor, ((DBSObject) object).getDataSource(), param -> {
                    try {
                        finalResult[0] = readPropertyMethod.get();
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
            value = finalResult[0];
        } else {
            value = readPropertyMethod.get();
        }

        if (formatValue) {
            value = formatValue(object, value);
        }
        return value;
    }

    public Object formatValue(Object object, Object value) {
        if (valueRenderer != null) {
            value = valueRenderer.transform(object, value);
        }
        if (value instanceof Number) {
            final Format displayFormat = getDisplayFormat();
            if (displayFormat != null) {
                value = displayFormat.format(value);
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
            final Class<?> argType = setter.getParameterTypes()[0];
            if (value == null) {
                // Check for primitive argument
                if (argType == Integer.TYPE) {
                    value = 0;
                } else if (argType == Short.TYPE) {
                    value = (short)0;
                } else if (argType == Long.TYPE) {
                    value = 0L;
                } else if (argType == Float.TYPE) {
                    value = (float)0.0;
                } else if (argType == Double.TYPE) {
                    value = 0.0;
                } else if (argType == Boolean.TYPE) {
                    value = false;
                } else if (argType == Character.TYPE) {
                    value = ' ';
                }
            } else {
                if (argType == Boolean.TYPE || argType == Boolean.class && !(value instanceof Boolean)) {
                    value = CommonUtils.toBoolean(value);
                } else if (argType == Long.TYPE) {
                    value = CommonUtils.toLong(value);
                } else if (argType == Integer.TYPE) {
                    value = CommonUtils.toInt(value);
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
        return propInfo.required();
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
                return propInfo.listProvider().getConstructor().newInstance().allowCustomValue();
            } catch (Exception e) {
                log.error(e);
            }
        }
        return false;
    }

    public boolean hasListValueProvider() {
        return (propInfo.listProvider() != IPropertyValueListProvider.class);
    }

    @Override
    public Object[] getPossibleValues(Object object)
    {
        if (propInfo.listProvider() != IPropertyValueListProvider.class) {
            // List
            try {
                return propInfo.listProvider().getConstructor().newInstance().getPossibleValues(object);
            } catch (Exception e) {
                log.error(e);
            }
        } else {
            Class<?> dataType = getDataType();
            if (dataType != null && dataType.isEnum()) {
                return dataType.getEnumConstants();
            }
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

    private String getLocalizedString(String string, String type, String defaultValue, boolean warnMissing, String locale) {
        if (Property.DEFAULT_LOCAL_STRING.equals(string)) {
            Method getter = getGetter();
            String propertyName = BeanUtils.getPropertyNameFromGetter(getter.getName());
            Class<?> propOwner = getter.getDeclaringClass();
            Bundle bundle = FrameworkUtil.getBundle(propOwner);
            ResourceBundle resourceBundle = getPluginResourceBundle(bundle, propOwner, locale);
            String messageID = "meta." + propOwner.getName() + "." + propertyName + "." + type;
            String result = null;
            try {
                result = resourceBundle.getString(messageID);
            } catch (Exception e) {
                // Try to find the same property in parent classes
                for (Class<?> parent = getter.getDeclaringClass().getSuperclass(); parent != null && parent != Object.class; parent = parent.getSuperclass()) {
                    try {
                        Method parentGetter = parent.getMethod(getter.getName(), getter.getParameterTypes());
                        Class<?> parentOwner = parentGetter.getDeclaringClass();
                        Bundle parentBundle = FrameworkUtil.getBundle(parentOwner);
                        if (parentBundle == null || parentBundle == bundle) {
                            continue;
                        }
                        ResourceBundle parentResourceBundle = getPluginResourceBundle(parentBundle, parentOwner, locale);
                        messageID = "meta." + parentOwner.getName() + "." + propertyName + "." + type;
                        try {
                            result = parentResourceBundle.getString(messageID);
                            break;
                        } catch (Exception e1) {
                            // Just skip it
                        }
                    } catch (Exception e1) {
                        // Just skip it
                    }
                }
                if (result == null) {
                    if (type.equals(Property.RESOURCE_TYPE_NAME) && warnMissing) {
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

    private ResourceBundle getPluginResourceBundle(Bundle bundle, Class<?> ownerClass, String language) {
        return RuntimeUtils.getBundleLocalization(bundle, language);
        // Copied from ResourceTranslator.getResourceBundle
//        Locale locale = (language == null) ? Locale.getDefault() : new Locale(language);
//        return ResourceBundle.getBundle("plugin", locale, ownerClass.getClassLoader()); //$NON-NLS-1$
    }

}
