/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * DataFormatterProfile
 */
class DataFormatterProfile implements DBDDataFormatterProfile, IPropertyChangeListener {

    private IPreferenceStore store;
    private String name;
    private Locale locale;
    private Map<String, Map<Object, Object>> properties = new HashMap<String, Map<Object, Object>>();
    private static final String PROP_LANGUAGE = "dataformat.profile.language";
    private static final String PROP_COUNTRY = "dataformat.profile.country";
    private static final String PROP_VARIANT = "dataformat.profile.variant";

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
        for (DataFormatterDescriptor formatter : DBeaverCore.getInstance().getDataFormatterRegistry().getDataFormatters()) {
            Map<Object, Object> formatterProps = new HashMap<Object, Object>();
            for (IPropertyDescriptor prop : formatter.getProperties()) {
                String propValue = store.getString("dataformat.type." + formatter.getId() + "." + prop.getId());
                if (!CommonUtils.isEmpty(propValue)) {
                    formatterProps.put(prop.getId(), propValue);
                }
            }
            properties.put(formatter.getId(), formatterProps);
        }
    }

    public void saveProfile() throws IOException
    {
        store.setValue(PROP_LANGUAGE, locale.getLanguage());
        store.setValue(PROP_COUNTRY, locale.getCountry());
        store.setValue(PROP_VARIANT, locale.getVariant());

        for (DataFormatterDescriptor formatter : DBeaverCore.getInstance().getDataFormatterRegistry().getDataFormatters()) {
            Map<Object, Object> formatterProps = properties.get(formatter.getId());
            for (IPropertyDescriptor prop : formatter.getProperties()) {
                Object propValue = formatterProps == null ? null : formatterProps.get(prop.getId());
                if (propValue != null) {
                    store.setValue("dataformat.type." + formatter.getId() + "." + prop.getId(), CommonUtils.toString(propValue));
                } else {
                    store.setToDefault("dataformat.type." + formatter.getId() + "." + prop.getId());
                }
            }
        }
        if (store instanceof AbstractPreferenceStore) {
            ((AbstractPreferenceStore)store).save();
        }
    }

    public IPreferenceStore getPreferenceStore()
    {
        return store;
    }

    public String getProfileName()
    {
        return name;
    }

    public void setProfileName(String name)
    {
        this.name = name;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    public Map<Object, Object> getFormatterProperties(String typeId)
    {
        return properties.get(typeId);
    }

    public void setFormatterProperties(String typeId, Map<Object, Object> properties)
    {
        this.properties.put(typeId, new HashMap<Object, Object>(properties));
    }

    public boolean isOverridesParent()
    {
        if (store instanceof AbstractPreferenceStore) {
            AbstractPreferenceStore abstractPreferenceStore = (AbstractPreferenceStore) store;
            return !abstractPreferenceStore.getProperties().isEmpty();
        }
        return true;
    }

    public void reset()
    {
        if (store instanceof AbstractPreferenceStore) {
            ((AbstractPreferenceStore)store).clear();
        }
        loadProfile();
    }

    public DBDDataFormatter createFormatter(String typeId)
        throws IllegalAccessException, InstantiationException, IllegalArgumentException
    {
        DataFormatterDescriptor descriptor = DBeaverCore.getInstance().getDataFormatterRegistry().getDataFormatter(typeId);
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

    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty() != null && event.getProperty().startsWith("dataformat.")) {
            // Reload this profile
            loadProfile();
        }
    }

}
