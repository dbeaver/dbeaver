/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.jface.preference.IPreferenceStore;

import java.util.Locale;
import java.util.Map;

/**
 * Data formatter profile
 */
public interface DBDDataFormatterProfile {

    String getProfileName();
    
    void setProfileName(String name);

    Locale getLocale();

    void setLocale(Locale locale);

    Map<String, String> getFormatterProperties(String typeId);

    void setFormatterProperties(String typeId, Map<String, String> properties);

    void saveProfile();

    DBDDataFormatter createFormatter(String typeId) throws IllegalAccessException, InstantiationException, IllegalArgumentException;
    
}
