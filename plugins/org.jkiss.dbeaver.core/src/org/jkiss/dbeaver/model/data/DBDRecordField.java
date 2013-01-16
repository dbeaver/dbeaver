package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.struct.DBSDataType;

/**
 * Data record field.
 * Field meta information
 */
public interface DBDRecordField {

    int getIndex();

    String getName();

    DBSDataType getType();

}
