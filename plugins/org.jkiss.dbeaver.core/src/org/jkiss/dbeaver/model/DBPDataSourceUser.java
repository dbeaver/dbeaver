/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * DBPDataSourceUser
 */
public interface DBPDataSourceUser
{
    /**
     * Checks this users needs active connection.
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    boolean needsConnection();

}