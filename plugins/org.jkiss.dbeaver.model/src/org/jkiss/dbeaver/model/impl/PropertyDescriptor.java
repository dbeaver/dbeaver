/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * PropertyDescriptor
 */
public class PropertyDescriptor implements DBPPropertyDescriptor, IPropertyValueListProvider<Object>, DBPNamedObject2 {

    public static final String CURRENT_DATE_STRING_VAR_PREFIX = "${now as ";

    public enum PropertyType {
        t_string(String.class),
        t_boolean(Boolean.class),
        t_short(Short.class),
        t_integer(Integer.class),
        t_long(Long.class),
        t_float(Float.class),
        t_double(Double.class),
        t_numeric(Double.class),
        t_file(String.class);
        // Removed because it is initialized before workbench start and breaks init queue
        //t_resource(IResource.class);

        private final Class<?> valueType;

        PropertyType(Class<?> valueType) {
            this.valueType = valueType;
        }

        public Class<?> getValueType() {
            return valueType;
        }
    }

    private static final Log log = Log.getLog(PropertyDescriptor.class);

    public static final String TAG_PROPERTY_GROUP = "propertyGroup"; //NON-NLS-1
    public static final String NAME_UNDEFINED = "<undefined>"; //NON-NLS-1
    public static final String TAG_PROPERTY = "property"; //NON-NLS-1

    public static final String ATTR_ID = "id"; //NON-NLS-1
    public static final String ATTR_LABEL = "label"; //NON-NLS-1
    public static final String ATTR_DESCRIPTION = "description"; //NON-NLS-1
    public static final String ATTR_TYPE = "type"; //NON-NLS-1
    private static final String ATTR_REQUIRED = "required"; //NON-NLS-1
    private static final String ATTR_DEFAULT_VALUE = "defaultValue"; //NON-NLS-1
    private static final String ATTR_VALID_VALUES = "validValues"; //NON-NLS-1
    private static final String ATTR_ALLOW_CUSTOM_VALUES = "allowCustomValues";
    private static final String ATTR_FEATURES = "features";
    private static final String ATTR_LENGTH = "length";

    private static final String VALUE_SPLITTER = ","; //NON-NLS-1

    @NotNull
    private final String id;
    private String name;
    private final String description;
    private final String category;
    private Class<?> type;
    private final boolean required;
    private Object defaultValue;
    private Object[] validValues;
    private boolean allowCustomValues = true;
    private final boolean editable;
    @NotNull
    private final PropertyLength length;
    private String[] features;

    public static DBPPropertyDescriptor[] extractPropertyGroups(IConfigurationElement config) {
        List<DBPPropertyDescriptor> props = new ArrayList<>();
        for (IConfigurationElement prop : ArrayUtils.safeArray(config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
            props.addAll(PropertyDescriptor.extractProperties(prop));
        }
        return props.toArray(new DBPPropertyDescriptor[0]);
    }

    public static List<DBPPropertyDescriptor> extractProperties(IConfigurationElement config) {
        String category = NAME_UNDEFINED;
        if (TAG_PROPERTY_GROUP.equals(config.getName())) {
            category = config.getAttribute(ATTR_LABEL);
            if (CommonUtils.isEmpty(category)) {
                category = NAME_UNDEFINED;
            }
        }
        List<DBPPropertyDescriptor> properties = new ArrayList<>();
        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptor.TAG_PROPERTY);
        for (IConfigurationElement prop : propElements) {
            properties.add(new PropertyDescriptor(category, prop));
        }
        return properties;
    }

    public PropertyDescriptor(String category, @NotNull String id, String name, String description,
                              boolean required, Class<?> type, Object defaultValue, Object[] validValues) {
        this.category = category;
        this.id = id;
        this.name = name;
        this.description = description;
        this.required = required;
        this.type = type;
        this.defaultValue = defaultValue;
        this.validValues = validValues;
        this.editable = true;
        this.length = PropertyLength.LONG;
    }

    public PropertyDescriptor(String category, IConfigurationElement config) {
        this.category = category;
        this.id = config.getAttribute(ATTR_ID);
        this.name = config.getAttribute(ATTR_LABEL);
        if (CommonUtils.isEmpty(this.name)) {
            this.name = CommonUtils.toString(this.id);
        }
        this.description = config.getAttribute(ATTR_DESCRIPTION);
        this.required = CommonUtils.getBoolean(config.getAttribute(ATTR_REQUIRED));
        String typeString = config.getAttribute(ATTR_TYPE);
        if (typeString == null) {
            type = String.class;
        } else {
            try {
                type = PropertyType.valueOf("t_" + typeString).getValueType();
            } catch (IllegalArgumentException ex) {
                log.warn(ex);
                type = String.class;
            }
        }
        this.defaultValue = convertString(config.getAttribute(ATTR_DEFAULT_VALUE), type);
        String valueList = config.getAttribute(ATTR_VALID_VALUES);
        if (valueList != null) {
            final String[] values = valueList.split(VALUE_SPLITTER);
            validValues = new Object[values.length];
            for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
                validValues[i] = convertString(values[i], type);
            }
        }
        this.allowCustomValues = CommonUtils.getBoolean(config.getAttribute(ATTR_ALLOW_CUSTOM_VALUES), true);
        String featuresString = config.getAttribute(ATTR_FEATURES);
        if (!CommonUtils.isEmpty(featuresString)) {
            this.features = featuresString.split(",");
        }

        this.editable = true;

        this.length = CommonUtils.valueOf(PropertyLength.class, config.getAttribute(ATTR_LENGTH), PropertyLength.LONG);
    }

    public static Object convertString(String value, Class<?> valueType) {
        if (!CommonUtils.isEmpty(value) && valueType == String.class && value.startsWith(CURRENT_DATE_STRING_VAR_PREFIX)) {
            String pattern = value.substring(9, value.length() - 1);
            return new SimpleDateFormat(pattern).format(new Date());
        } else {
            return GeneralUtils.convertString(value, valueType);
        }
    }

    public PropertyDescriptor(String category, @NotNull String id, String name, String description, Class<?> type, boolean required, Object defaultValue, String[] validValues, boolean editable) {
        this.category = category;
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.validValues = validValues;
        this.editable = editable;
        this.length = PropertyLength.LONG;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    @Override
    public String getCategory() {
        return category;
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isEditable(Object object) {
        return editable;
    }

    @NotNull
    @Override
    public PropertyLength getLength() {
        return length;
    }

    @Override
    public Class<?> getDataType() {
        return type;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public boolean allowCustomValue() {
        return ArrayUtils.isEmpty(validValues) || allowCustomValues;
    }

    @Override
    public Object[] getPossibleValues(Object object) {
        return validValues;
    }

    public String[] getFeatures() {
        String[] allFeatures = features;
        if (isRequired()) {
            if (allFeatures == null) {
                allFeatures = new String[] { "required" };
            } else {
                allFeatures = ArrayUtils.add(String.class, allFeatures, "required");
            }
        }
        return allFeatures;
    }

    @Override
    public boolean hasFeature(@NotNull String feature) {
        return features != null && ArrayUtils.contains(features, feature);
    }

    @Override
    public String toString() {
        return id + " (" + name + ")";
    }
}
