/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;

/**
 * Native client home descriptor
 */
public interface DBPClientHome {

    String getHomeId();

    String getHomePath();

    String getDisplayName();

    String getProductName()
        throws DBException;

    String getProductVersion()
        throws DBException;

}
