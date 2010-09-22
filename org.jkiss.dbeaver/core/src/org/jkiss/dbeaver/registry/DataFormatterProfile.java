/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.prop.DBPProperty;
import org.jkiss.dbeaver.model.prop.DBPPropertyGroup;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

import java.util.*;

/**
 * DataFormatterProfile
 */
class DataFormatterProfile implements DBDDataFormatterProfile, IPropertyChangeListener {

    private IPreferenceStore store;
    private String name;
    private Locale locale;
    private Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    private static final String PROP_NAME = "dataformat.profile.name";
    private static final String PROP_LANGUAGE = "dataformat.profile.language";
    private static final String PROP_COUNTRY = "dataformat.profile.country";
    private static final String PROP_VARIANT = "dataformat.profile.variant";

    DataFormatterProfile(IPreferenceStore store)
    {
        this.store = store;
        if (store instanceof AbstractPreferenceStore) {
            ((AbstractPreferenceStore)store).getParentStore().addPropertyChangeListener(this);
        }
        loadProfile();
    }

    private void loadProfile()
    {
        this.name = store.getString(PROP_NAME);
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
        for (DataFormatterDescriptor formatter : DataSourceRegistry.getDefault().getDataFormatters()) {
            Map<String, String> formatterProps = new HashMap<String, String>();
            for (DBPPropertyGroup group : formatter.getPropertyGroups()) {
                for (DBPProperty prop : group.getProperties()) {
                    String propValue = store.getString("dataformat.type." + formatter.getId() + "." + prop.getId());
                    if (!CommonUtils.isEmpty(propValue)) {
                        formatterProps.put(prop.getId(), propValue);
                    }
                }
            }
            properties.put(formatter.getId(), formatterProps);
        }
    }

    public void saveProfile()
    {
        store.setValue(PROP_NAME, name);
        store.setValue(PROP_LANGUAGE, locale.getLanguage());
        store.setValue(PROP_COUNTRY, locale.getCountry());
        store.setValue(PROP_VARIANT, locale.getVariant());

        for (DataFormatterDescriptor formatter : DataSourceRegistry.getDefault().getDataFormatters()) {
            Map<String, String> formatterProps = properties.get(formatter.getId());
            for (DBPPropertyGroup group : formatter.getPropertyGroups()) {
                for (DBPProperty prop : group.getProperties()) {
                    String propValue = formatterProps == null ? null : formatterProps.get(prop.getId());
                    if (!CommonUtils.isEmpty(propValue)) {
                        store.setValue("dataformat.type." + formatter.getId() + "." + prop.getId(), propValue);
                    } else {
                        store.setToDefault("dataformat.type." + formatter.getId() + "." + prop.getId());
                    }
                }
            }
        }
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

    public Map<String, String> getFormatterProperties(String typeId)
    {
        return properties.get(typeId);
    }

    public void setFormatterProperties(String typeId, Map<String, String> properties)
    {
        this.properties.put(typeId, new HashMap<String, String>(properties));
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
        DataFormatterDescriptor descriptor = DataSourceRegistry.getDefault().getDataFormatter(typeId);
        if (descriptor == null) {
            throw new IllegalArgumentException("Formatter '" + typeId + "' not found");
        }
        DBDDataFormatter formatter = descriptor.createFormatter();

        Map<String, String> defProps = descriptor.getSample().getDefaultProperties(locale);
        Map<String, String> props = getFormatterProperties(typeId);
        Map<String, String> formatterProps = new HashMap<String, String>();
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
