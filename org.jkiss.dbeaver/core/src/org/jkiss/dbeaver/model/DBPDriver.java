/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;

import java.sql.Driver;
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

    Driver getDriverInstance() throws DBException;

    Integer getDefaultPort();

    String getSampleURL();

    String getWebURL();

    boolean supportsDriverProperties();

    List<? extends DBPDriverPropertyGroup> getPropertyGroups();

    List<? extends DBPDriverCustomQuery> getCustomQueries();

    String getCustomQuery(String name);

    void loadDriver()
        throws DBException;

}
