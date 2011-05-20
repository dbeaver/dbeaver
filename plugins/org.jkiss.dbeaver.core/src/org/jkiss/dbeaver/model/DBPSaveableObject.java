/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * Abstract saveable object
 */
public interface DBPSaveableObject extends DBPPersistedObject
{

    /**
     * Object's persisted flag
     * @param persisted new persistence state
     * @return true if object is persisted in external data source
     */
    void setPersisted(boolean persisted);

}