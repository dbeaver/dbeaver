/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.prop;

/**
 * DBPProperty
 */
public interface DBPProperty
{
    public enum PropertyType
    {
        STRING,
        BOOLEAN,
        INTEGER,
        NUMERIC,
        RESOURCE,
    }

    DBPPropertyGroup getGroup();

    String getId();

    String getName();

    String getDescription();

    PropertyType getType();

    boolean isRequired();

    Object getDefaultValue();

    Object[] getValidValues();

}