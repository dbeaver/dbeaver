/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * Abstract persisted object
 */
public interface DBPPersistedObject extends DBPObject
{

    /**
     * Object's persisted flag
     * @return true if object is persisted in external data source
     */
    boolean isPersisted();

}