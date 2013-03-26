package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

import java.util.Collection;

/**
 * Structured data record.
 * Consists of primitive values or other records
 */
public interface DBDStructure extends DBDComplexType {

    Collection<DBSAttributeBase> getAttributes();

    Object getAttributeValue(DBSAttributeBase attribute)
        throws DBCException;

    void setAttributeValue(DBSAttributeBase attribute, Object value)
        throws DBCException;

}
