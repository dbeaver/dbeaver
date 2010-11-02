/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.meta.Property;

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
    @Property(name = "Name", viewable = true, order = 1)
    String getName();

}