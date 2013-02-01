package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.struct.DBSDataType;

/**
 * Complex type
 */
public interface DBDComplexType extends DBDValue {

    DBSDataType getObjectDataType();

}
