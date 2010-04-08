package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * DBD Value Handler.
 * Extract, edit and bind database values.
 */
public interface DBDValueHandler
{
    /**
     * Extract string representation of spcified value
     * @param resultSet result set
     * @param columnIndex column index
     * @return string
     * @throws DBCException on error
     */
    Object getValueObject(DBCResultSet resultSet, int columnIndex)
        throws DBCException;

    /**
     * Binds specified parameter to statement
     * @param statement statement
     * @param columnType column type
     * @param paramIndex parameter index (starts from 0)
     * @param value parameter value (can be null)   @throws DBCException on error
     * @throws DBCException on error
     */
    void bindParameter(DBCStatement statement, DBSTypedObject columnType, int paramIndex, Object value)
        throws DBCException;

    /**
     * Returns any additional annotations of value
     * @param column column info
     * @return annotations array or null
     * @throws DBCException on error
     */
    DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column)
        throws DBCException;

    /**
     * Shows value editor
     * @param controller value controller
     * @return true if editor was successfully opened
     * @throws org.jkiss.dbeaver.DBException on error
     */
    boolean editValue(DBDValueController controller)
        throws DBException;

}