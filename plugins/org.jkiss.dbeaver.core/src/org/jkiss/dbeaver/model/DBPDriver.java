/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;

import java.util.List;
import java.util.Map;

/**
 * DBPDriver
 */
public interface DBPDriver extends DBPObject
{
    DBPDataSourceProvider getDataSourceProvider()
        throws DBException;

    String getName();

    String getDescription();

    String getDriverClassName();

    Object getDriverInstance() throws DBException;

    Integer getDefaultPort();

    String getSampleURL();

    String getWebURL();

    boolean supportsDriverProperties();

    List<? extends DBPPropertyGroup> getConnectionPropertyGroups();

    Map<String, String> getDriverParameters();

    String getDriverParameter(String name);

    void loadDriver()
        throws DBException;

}
