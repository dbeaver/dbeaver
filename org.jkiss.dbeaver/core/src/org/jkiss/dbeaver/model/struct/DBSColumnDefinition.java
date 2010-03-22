package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSColumnDefinition
 */
public interface DBSColumnDefinition<DATASOURCE extends DBPDataSource> extends DBSStructureObject<DATASOURCE>
{
    DBSDataType getDataType();

    int getValueType();

    int getMaxLength();

    int getScale();

    int getPrecision();

    int getRadix();

    boolean isNullable();
}