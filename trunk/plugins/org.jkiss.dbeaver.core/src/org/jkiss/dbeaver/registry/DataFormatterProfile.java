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
package org.jkiss.dbeaver.registry;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * DataFormatterProfile
 */
public class DataFormatterProfile implements DBDDataFormatterProfile, IPropertyChangeListener {

    private static final String PROP_LANGUAGE = "dataformat.profile.language"; //$NON-NLS-1$
    private static final String PROP_COUNTRY = "dataformat.profile.country"; //$NON-NLS-1$
    private static final String PROP_VARIANT = "dataformat.profile.variant"; //$NON-NLS-1$
    public static final String DATAFORMAT_PREFIX = "dataformat."; //$NON-NLS-1$
    public static final String DATAFORMAT_TYPE_PREFIX = DATAFORMAT_PREFIX + "type."; //$NON-NLS-1$

    private IPreferenceStore store;
    private String name;
    private Locale locale;
    private Map<String, Map<Object, Object>> properties = new HashMap<String, Map<Object, Object>>();

    DataFormatterProfile(String profileName, IPreferenceStore store)
    {
        this.name = profileName;
        this.store = store;
        if (store instanceof AbstractPreferenceStore) {
            ((AbstractPreferenceStore)store).getParentStore().addPropertyChangeListener(this);
        }
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
        properties.clear();
        for (DataFormatterDescriptor formatter : DataFormatterRegistry.getInstance().getDataFormatters()) {
            Map<Object, Object> defaultProperties = formatter.getSample().getDefaultProperties(locale);
            Map<Object, Object> formatterProps = new HashMap<Object, Object>();
            for (PropertyDescriptorEx prop : formatter.getProperties()) {
                Object defaultValue = defaultProperties.get(prop.getId());
                Object propValue = RuntimeUtils.getPreferenceValue(
                    store,
                    DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId(), prop.getDataType());
                if (propValue != null && !CommonUtils.equalObjects(defaultValue, propValue)) {
                    formatterProps.put(prop.getId(), propValue);
                }
            }
            properties.put(formatter.getId(), formatterProps);
        }
    }

    @Override
    public void saveProfile() throws IOException
    {
        store.setValue(PROP_LANGUAGE, locale.getLanguage());
        store.setValue(PROP_COUNTRY, locale.getCountry());
        store.setValue(PROP_VARIANT, locale.getVariant());

        for (DataFormatterDescriptor formatter : DataFormatterRegistry.getInstance().getDataFormatters()) {
            Map<Object, Object> formatterProps = properties.get(formatter.getId());
            for (PropertyDescriptorEx prop : formatter.getProperties()) {
                Object propValue = formatterProps == null ? null : formatterProps.get(prop.getId());
                if (propValue != null) {
                    RuntimeUtils.setPreferenceValue(store, DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId(), propValue);
                } else {
                    store.setToDefault(DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId());
                }
            }
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    public IPreferenceStore getPreferenceStore()
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
        return properties.get(typeId);
    }

    @Override
    public void setFormatterProperties(String typeId, Map<Object, Object> properties)
    {
        this.properties.put(typeId, new HashMap<Object, Object>(properties));
    }

    @Override
    public boolean isOverridesParent()
    {
        if (store instanceof AbstractPreferenceStore) {
            AbstractPreferenceStore prefStore = (AbstractPreferenceStore) store;

            if (prefStore.isSet(PROP_LANGUAGE) || prefStore.isSet(PROP_COUNTRY) || prefStore.isSet(PROP_VARIANT)) {
                return true;
            }

            for (DataFormatterDescriptor formatter : DataFormatterRegistry.getInstance().getDataFormatters()) {
                for (PropertyDescriptorEx prop : formatter.getProperties()) {
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
        if (store instanceof AbstractPreferenceStore) {
            // Set all formatter properties to default
            store.setToDefault(PROP_LANGUAGE);
            store.setToDefault(PROP_COUNTRY);
            store.setToDefault(PROP_VARIANT);

            for (DataFormatterDescriptor formatter : DataFormatterRegistry.getInstance().getDataFormatters()) {
                for (PropertyDescriptorEx prop : formatter.getProperties()) {
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
        Map<Object, Object> formatterProps = new HashMap<Object, Object>();
        if (defProps != null && !defProps.isEmpty()) {
            formatterProps.putAll(defProps);
        }
        if (props != null && !props.isEmpty()) {
            formatterProps.putAll(props);
        }
        formatter.init(locale, formatterProps);
        return formatter;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty() != null && event.getProperty().startsWith(DATAFORMAT_PREFIX)) {
            // Reload this profile
            loadProfile();
        }
    }

    public static void initDefaultPreferences(IPreferenceStore store, Locale locale)
    {
        for (DataFormatterDescriptor formatter : DataFormatterRegistry.getInstance().getDataFormatters()) {
            Map<Object, Object> defaultProperties = formatter.getSample().getDefaultProperties(locale);
            Map<Object, Object> formatterProps = new HashMap<Object, Object>();
            for (PropertyDescriptorEx prop : formatter.getProperties()) {
                Object defaultValue = defaultProperties.get(prop.getId());
                if (defaultValue != null) {
                    RuntimeUtils.setPreferenceDefaultValue(store, DATAFORMAT_TYPE_PREFIX + formatter.getId() + "." + prop.getId(), defaultValue);
                }
            }
        }
    }
}
