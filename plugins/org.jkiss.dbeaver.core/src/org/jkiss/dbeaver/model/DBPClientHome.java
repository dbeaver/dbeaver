/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;

import java.io.File;

/**
 * Native client home descriptor
 */
public interface DBPClientHome {

    String getHomeId();

    File getHomePath();

    String getDisplayName();

    String getProductName()
        throws DBException;

    String getProductVersion()
        throws DBException;

}
