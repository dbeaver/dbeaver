/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * DBPDataSourceHandler
 */
public interface DBPDataSourceHandler
{
    void beforeConnect();

    void beforeDisconnect();
}