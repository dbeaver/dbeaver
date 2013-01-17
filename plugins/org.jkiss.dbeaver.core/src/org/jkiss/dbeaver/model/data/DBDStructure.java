package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Structured data record.
 * Consists of primitive values or other records
 */
public interface DBDStructure extends DBDValue {

    DBSEntity getStructType();

    Object getAttributeValue(DBRProgressMonitor monitor, DBSEntityAttribute attribute)
        throws DBCException;

    void setAttributeValue(DBRProgressMonitor monitor, DBSEntityAttribute attribute, Object value)
        throws DBCException;

}
