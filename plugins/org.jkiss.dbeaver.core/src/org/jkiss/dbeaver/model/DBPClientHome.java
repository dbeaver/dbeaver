/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * Native client home descriptor
 */
public interface DBPClientHome {

    String getHomeId();

    String getHomePath();

    String getDisplayName();

    String getProductName();

    String getProductVersion();

}
