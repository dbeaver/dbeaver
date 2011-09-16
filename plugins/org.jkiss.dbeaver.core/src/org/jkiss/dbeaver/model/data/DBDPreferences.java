/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

/**
 * Data preferences
 */
public interface DBDPreferences {

    /**
     * Gets current context's data formatter profile
     * @return profile
     */
    DBDDataFormatterProfile getDataFormatterProfile();

    /**
     * Sets current context's data formatter profile
     */
    void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile);

    /**
     * Default value handler
     * @return value handler instance
     */
    DBDValueHandler getDefaultValueHandler();

}
