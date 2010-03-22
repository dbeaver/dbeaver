package org.jkiss.dbeaver.model;

/**
 * DBPDriver
 */
public interface DBPDriverProperty
{
    public enum PropertyType
    {
        STRING,
        BOOLEAN,
        INTEGER,
        NUMERIC,
    }

    DBPDriverPropertyGroup getGroup();

    String getName();

    String getDescription();

    String getDefaultValue();

    PropertyType getType();
}