/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * Abstract model object
 */
public interface DBPNamedObject extends DBPObject
{
    /**
     * Object name
     *
     * @return object name
     */
    String getName();

}