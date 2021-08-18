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
package org.jkiss.dbeaver.registry.formatter;

import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * DataFormatterProfile
 */
public class DataFormatterProfile implements DBDDataFormatterProfile, DBPPreferenceListener {

    private static final String PROP_LANGUAGE = "dataformat.profile.language"; //$NON-NLS-1$
    private static final String PROP_COUNTRY = "dataformat.profile.country"; //$NON-NLS-1$
    private static final String PROP_VARIANT = "dataformat.profile.variant"; //$NON-NLS-1$
    public static final String DATAFORMAT_PREFIX = "dataformat."; //$NON-NLS-1$
    public static final String DATAFORMAT_TYPE_PREFIX = DATAFORMAT_PREFIX + "type."; //$NON-NLS-1$

    private DBPPreferenceStore store;
    private String name;
    private Locale locale;

    public DataFormatterProfile(String profileName, DBPPreferenceStore store)
    {
        this.name = profileName;
        this.store = store;
        loadProfile(store);
    }

    private void loadProfile(DBPPreferenceStore store)
    {
        {
            String language = store.getString(PROP_LANGUAGE);
            String country = store.getString(PROP_COUNTRY);
            String variant = store.getString(PROP_VARIANT);
            if (CommonUtils.isEmpty(language)) {
                this.locale = Locale.getDefault();
            } else if (CommonUtils.isEmpty(country)) {
                this.locale = new Locale(language);
            } else if (CommonUtils.isEmpty(variant)) {
                this.locale = new Locale(language, country);
            } else {
                this.locale = new Locale(language, country, variant);
            }
        }
    }

    @Override
    public void saveProfile(DBPPreferenceStore store) throws IOException
    {
        store.setValue(PROP_LANGUAGE, locale.getLanguage());
        store.setValue(PROP_COUNTRY, locale.getCountry());
        store.setValue(PROP_VARIANT, locale.getVariant());

        PrefUtils.savePreferenceStore(store);
    }

    @Override
    public DBPPreferenceStore getPreferenceStore()
    {
        return store;
    }

    @Override
    public String getProfileName()
    {
        return name;
    }

    @Override
    public void setProfileName(String name)
    {
        this.name = name;
    }

    @Override
    public Locale getLocale()
    {
        return locale;
    }

    @Override
    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    @Override
    public Map<String, Object> getFormatterProperties(DBPPreferenceStore store, String typeId)
    {
        DataFormatterDescriptor formatter = DataFormatterRegistry.getInstance().getDataFormatter(typeId);
        Map<String, Object> defaultProperties = formatter.getSample().getDefaultProperties(locale);
        Map<String, Object> formatterProps = new HashMap<>();
        for (DBPPropertyDescriptor prop : formatter.getProperties()) {
            Object defaultValue = defaultProperties.get(prop.getId());
            Object propValue = PrefUtils.getPreferenceValue(
                store,
                DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId(), prop.getDataType());
            if (propValue != null && !CommonUtils.equalObjects(defaultValue, propValue)) {
                formatterProps.put(prop.getId(), propValue);
            }
        }
        return formatterProps;
    }

    @Override
    public void setFormatterProperties(DBPPreferenceStore store, String typeId, Map<String, Object> formatterProps)
    {
        DataFormatterDescriptor formatter = DataFormatterRegistry.getInstance().getDataFormatter(typeId);
        for (DBPPropertyDescriptor prop : formatter.getProperties()) {
            Object propValue = formatterProps == null ? null : formatterProps.get(prop.getId());
            if (propValue != null) {
                PrefUtils.setPreferenceValue(store, DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId(), propValue);
            } else {
                store.setToDefault(DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId());
            }
        }
    }

    @Override
    public boolean isOverridesParent()
    {
        if (store instanceof SimplePreferenceStore) {
            SimplePreferenceStore prefStore = (SimplePreferenceStore) store;

            if (prefStore.isSet(PROP_LANGUAGE) || prefStore.isSet(PROP_COUNTRY) || prefStore.isSet(PROP_VARIANT)) {
                return true;
            }

            for (DataFormatterDescriptor formatter : DataFormatterRegistry.getInstance().getDataFormatters()) {
                for (DBPPropertyDescriptor prop : formatter.getProperties()) {
                    if (prefStore.isSet(DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId())) {
                        return true;
                    }
                }
            }

            return false;
        }
        return true;
    }

    @Override
    public void reset(DBPPreferenceStore store)
    {
        if (store instanceof SimplePreferenceStore) {
            // Set all formatter properties to default
            store.setToDefault(PROP_LANGUAGE);
            store.setToDefault(PROP_COUNTRY);
            store.setToDefault(PROP_VARIANT);

            for (DataFormatterDescriptor formatter : DataFormatterRegistry.getInstance().getDataFormatters()) {
                for (DBPPropertyDescriptor prop : formatter.getProperties()) {
                    store.setToDefault(DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId());
                }
            }
        }
        loadProfile(store);
    }

    @Override
    public DBDDataFormatter createFormatter(String typeId, DBSTypedObject type)
        throws ReflectiveOperationException
    {
        DataFormatterDescriptor descriptor = DataFormatterRegistry.getInstance().getDataFormatter(typeId);
        if (descriptor == null) {
            throw new IllegalArgumentException("Formatter '" + typeId + "' not found");
        }
        DBDDataFormatter formatter = descriptor.createFormatter();

        Map<String, Object> defProps = descriptor.getSample().getDefaultProperties(locale);
        Map<String, Object> props = getFormatterProperties(store, typeId);
        Map<String, Object> formatterProps = new HashMap<>();
        if (defProps != null && !defProps.isEmpty()) {
            formatterProps.putAll(defProps);
        }
        if (props != null && !props.isEmpty()) {
            formatterProps.putAll(props);
        }
        formatter.init(type, locale, formatterProps);
        return formatter;
    }

    public static void initDefaultPreferences(DBPPreferenceStore store, Locale locale)
    {
        for (DataFormatterDescriptor formatter : DataFormatterRegistry.getInstance().getDataFormatters()) {
            Map<String, Object> defaultProperties = formatter.getSample().getDefaultProperties(locale);
            //Map<String, Object> formatterProps = new HashMap<>();
            for (DBPPropertyDescriptor prop : formatter.getProperties()) {
                Object defaultValue = defaultProperties.get(prop.getId());
                if (defaultValue != null) {
                    PrefUtils.setPreferenceDefaultValue(store, DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId(), defaultValue);
                }
            }
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
        if (event.getProperty() != null && event.getProperty().startsWith(DATAFORMAT_PREFIX)) {
            // Reload this profile
            loadProfile(store);
        }
    }
}
