package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.Collection;

/**
 * Structured data record.
 * Consists of primitive values or other records
 */
public interface DBDStructure extends DBDComplexType {

    DBSDataType getStructType();

    Collection<DBSAttributeBase> getAttributes();

    Object getAttributeValue(DBSAttributeBase attribute)
        throws DBCException;

    void setAttributeValue(DBSAttributeBase attribute, Object value)
        throws DBCException;

}
