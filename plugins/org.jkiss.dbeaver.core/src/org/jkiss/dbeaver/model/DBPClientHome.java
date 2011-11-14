/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * DBPClientHome
 */
public interface DBPClientHome {

    String getClientName();

    String getClientPath();

    String getProductName();

    String getProductVersion();

}
