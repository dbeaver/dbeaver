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
package org.jkiss.dbeaver.registry.formatter;

import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
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
        loadProfile();
    }

    private void loadProfile()
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
    public void saveProfile() throws IOException
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
    public Map<Object, Object> getFormatterProperties(String typeId)
    {
        DataFormatterDescriptor formatter = DataFormatterRegistry.getInstance().getDataFormatter(typeId);
        Map<Object, Object> defaultProperties = formatter.getSample().getDefaultProperties(locale);
        Map<Object, Object> formatterProps = new HashMap<>();
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
    public void setFormatterProperties(String typeId, Map<Object, Object> formatterProps)
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
    public void reset()
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
        loadProfile();
    }

    @Override
    public DBDDataFormatter createFormatter(String typeId)
        throws IllegalAccessException, InstantiationException, IllegalArgumentException
    {
        DataFormatterDescriptor descriptor = DataFormatterRegistry.getInstance().getDataFormatter(typeId);
        if (descriptor == null) {
            throw new IllegalArgumentException("Formatter '" + typeId + "' not found");
        }
        DBDDataFormatter formatter = descriptor.createFormatter();

        Map<Object, Object> defProps = descriptor.getSample().getDefaultProperties(locale);
        Map<Object, Object> props = getFormatterProperties(typeId);
        Map<Object, Object> formatterProps = new HashMap<>();
        if (defProps != null && !defProps.isEmpty()) {
            formatterProps.putAll(defProps);
        }
        if (props != null && !props.isEmpty()) {
            formatterProps.putAll(props);
        }
        formatter.init(locale, formatterProps);
        return formatter;
    }

    public static void initDefaultPreferences(DBPPreferenceStore store, Locale locale)
    {
        for (DataFormatterDescriptor formatter : DataFormatterRegistry.getInstance().getDataFormatters()) {
            Map<Object, Object> defaultProperties = formatter.getSample().getDefaultProperties(locale);
            Map<Object, Object> formatterProps = new HashMap<>();
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
            loadProfile();
        }
    }
}
