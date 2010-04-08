package org.jkiss.dbeaver.model.struct;

/**
 * DBSColumnDefinition
 */
public interface DBSColumnDefinition extends DBSStructureObject, DBSTypedObject
{
    int getMaxLength();

    int getScale();

    int getPrecision();

    int getRadix();

    boolean isNullable();
}