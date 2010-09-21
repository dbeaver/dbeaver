package org.jkiss.dbeaver.registry;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * DataFormatterProfile
 */
class DataFormatterProfile implements DBDDataFormatterProfile {

    private String name;
    private IPreferenceStore store;
    private Locale locale;
    private Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();

    DataFormatterProfile(String name, IPreferenceStore store)
    {
        this.name = name;
        this.store = store;
        this.locale = Locale.getDefault();
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
