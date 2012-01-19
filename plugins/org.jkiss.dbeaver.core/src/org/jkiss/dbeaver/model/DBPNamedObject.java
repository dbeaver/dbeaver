/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * Abstract model object
 */
public interface DBPNamedObject extends DBPObject
{
    /**
     * Object name.
     * Object name may be not unique.
     *
     * @return object name
     */
    String getName();

}