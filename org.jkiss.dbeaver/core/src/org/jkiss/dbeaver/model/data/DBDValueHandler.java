package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
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
        throws DBException;

    /**
     * Returns any additional annotations of value
     * @param value value
     * @return annotations array or null
     * @throws DBException on error
     */
    DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column)
        throws DBException;

    /**
     * Shows value editor
     * @param value value
     * @param valueLocator value locator
     * @param inlineEdit true if inline editor requested
     * @param valueSite site of callee
     * @param valueWidget widged where inline editor should be placed
     * @throws DBException on error
     */
    void editValue(
        DBCColumnMetaData column,
        Object value,
        DBDValueLocator valueLocator,
        boolean inlineEdit,
        IWorkbenchPartSite valueSite,
        Widget valueWidget)
        throws DBException;

    /**
     * Frees all obtained resources
     */
    void dispose();
}