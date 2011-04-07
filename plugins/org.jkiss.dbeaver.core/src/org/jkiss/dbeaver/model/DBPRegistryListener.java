/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * DBPRegistryListener
 */
public interface DBPRegistryListener
{

    void handleRegistryLoad(DBPDataSourceRegistry registry);

    void handleRegistryUnload(DBPDataSourceRegistry registry);

}
