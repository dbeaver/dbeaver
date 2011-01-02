/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

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

    String getDefaultValue();

    String[] getValidValues();

}