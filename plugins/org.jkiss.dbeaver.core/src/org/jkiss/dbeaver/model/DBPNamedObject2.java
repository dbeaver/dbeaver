/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * Object which could be renamed
 */
public interface DBPNamedObject2 extends DBPNamedObject
{
    /**
     * Object name
     * @param newName new name
     */
    void setName(String newName);

}