package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.prop.DBPProperty;
import org.jkiss.dbeaver.model.prop.DBPPropertyGroup;

import java.util.*;

/**
 * DataFormatterProfile
 */
class DataFormatterProfile implements DBDDataFormatterProfile {

    private String name;
    private Locale locale;
    private Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();

    DataFormatterProfile(IPreferenceStore store)
    {
        this.name = store.getString("dataformat.profile.name");
        {
            String language = store.getString("dataformat.profile.language");
            String country = store.getString("dataformat.profile.country");
            String variant = store.getString("dataformat.profile.variant");
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

    public void saveProfile(IPreferenceStore store)
    {
        store.setValue("dataformat.profile.name", name);
        store.setValue("dataformat.profile.language", locale.getLanguage());
        store.setValue("dataformat.profile.country", locale.getCountry());
        store.setValue("dataformat.profile.variant", locale.getVariant());

        for (DataFormatterDescriptor formatter : DataSourceRegistry.getDefault().getDataFormatters()) {
            Map<String, String> formatterProps = properties.get(formatter.getId());
            if (formatterProps == null || formatterProps.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String> entry : formatterProps.entrySet()) {
                store.setValue(
                    "dataformat.type." + formatter.getId() + "." + entry.getKey(),
                    entry.getValue());
            }
        }
    }

    public String getProfileName()
    {
        return name;
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
        Map<String, String> props = properties.get(typeId);
        return props == null ? Collections.<String, String>emptyMap() : props;
    }

    public void setFormatterProperties(String typeId, Map<String, String> properties)
    {
        this.properties.put(typeId, new HashMap<String, String>(properties));
    }

    public DBDDataFormatter createFormatter(String typeId)
        throws IllegalAccessException, InstantiationException, IllegalArgumentException
    {
        DataFormatterDescriptor descriptor = DataSourceRegistry.getDefault().getDataFormatter(typeId);
        if (descriptor == null) {
            throw new IllegalArgumentException("Formatter '" + typeId + "' not found");
        }
        DBDDataFormatter formatter = descriptor.createFormatter();
        formatter.init(locale, getFormatterProperties(typeId));
        return formatter;
    }

}
