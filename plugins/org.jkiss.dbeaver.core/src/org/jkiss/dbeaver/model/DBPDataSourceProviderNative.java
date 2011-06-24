/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * Native data source provider
 */
public interface DBPDataSourceProviderNative
{

    String findLibrary(DBPDriverLocalPath path);

}
