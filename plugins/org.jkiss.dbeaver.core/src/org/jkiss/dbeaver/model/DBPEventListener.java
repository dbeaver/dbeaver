/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * DBPEventListener
 */
public interface DBPEventListener
{
    void handleDataSourceEvent(DBPEvent event);
}
