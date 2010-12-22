/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;

import java.util.List;

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

    List<? extends DBPPropertyGroup> getPropertyGroups();

    List<? extends DBPDriverCustomQuery> getCustomQueries();

    String getCustomQuery(String name);

    String getParameter(String name);

    void loadDriver()
        throws DBException;

}
