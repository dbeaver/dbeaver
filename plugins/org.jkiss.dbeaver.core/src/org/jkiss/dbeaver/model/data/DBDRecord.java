package org.jkiss.dbeaver.model.data;

/**
 * Data record.
 * Consists of primitive values or other records
 */
public interface DBDRecord extends DBDValue {

    int getFieldCount();

    DBDRecordField getField(int index);

    Object getFieldValue(int index);

}
