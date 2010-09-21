package org.jkiss.dbeaver.model.data;

import org.eclipse.jface.preference.IPreferenceStore;

import java.util.Locale;
import java.util.Map;

/**
 * Data formatter profile
 */
public interface DBDDataFormatterProfile {

    String getProfileName();

    Locale getLocale();

    void setLocale(Locale locale);

    Map<String, String> getFormatterProperties(String typeId);

    void setFormatterProperties(String typeId, Map<String, String> properties);

    void saveProfile(IPreferenceStore store);

    DBDDataFormatter createFormatter(String typeId) throws IllegalAccessException, InstantiationException, IllegalArgumentException;
    
}
