/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.jface.preference.IPreferenceStore;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Data formatter profile
 */
public interface DBDDataFormatterProfile {

    IPreferenceStore getPreferenceStore();

    String getProfileName();
    
    void setProfileName(String name);

    Locale getLocale();

    void setLocale(Locale locale);

    Map<String, String> getFormatterProperties(String typeId);

    void setFormatterProperties(String typeId, Map<String, String> properties);

    boolean isOverridesParent();

    void reset();

    void saveProfile() throws IOException;

    DBDDataFormatter createFormatter(String typeId) throws IllegalAccessException, InstantiationException, IllegalArgumentException;

}
