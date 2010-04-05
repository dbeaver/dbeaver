package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;

/**
 * DBPDataTypeEditor
 */
public interface DBDValueHandler
{
    /**
     * Extract string representation of spcified value
     * @param value value
     * @return string
     * @throws DBException on error
     */
    Object getValueObject(DBCResultSet resultSet, int columnIndex)
        throws DBCException;

    /**
     * Returns any additional annotations of value
     * @param value value
     * @return annotations array or null
     * @throws DBException on error
     */
    DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column)
        throws DBCException;

    /**
     * Shows value editor
     */
    boolean editValue(DBDValueController controller)
        throws DBException;

}