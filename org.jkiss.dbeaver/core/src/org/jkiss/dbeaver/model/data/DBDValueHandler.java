/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSColumnBase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;

/**
 * DBD Value Handler.
 * Extract, edit and bind database values.
 */
public interface DBDValueHandler 
{
    /**
     * Extracts object from result set
     * @param monitor progress monitor
     * @param resultSet result set
     * @param column column
     * @param columnIndex column index   @return value or null
     * @return value
     * @throws org.jkiss.dbeaver.model.dbc.DBCException on error
     */
    Object getValueObject(DBRProgressMonitor monitor, DBCResultSet resultSet, DBSColumnBase column, int columnIndex)
        throws DBCException;

    /**
     * Binds specified parameter to statement
     * @param monitor progress monitor
     * @param statement statement
     * @param columnType column type
     * @param paramIndex parameter index (starts from 0)
     * @param value parameter value (can be null). Value is get from getValueObject function or from
     * object set by editor (editValue function).
     * @throws org.jkiss.dbeaver.model.dbc.DBCException on error
     */
    void bindValueObject(DBRProgressMonitor monitor, DBCStatement statement, DBSTypedObject columnType, int paramIndex, Object value)
        throws DBCException;

    /**
     * Makes value copy. For Non-mutable objects (like numbers and string) may return the same value as passed in.
     * If copy operation is not supported for some values then may return null.
     * @param monitor progress monitor
     * @param value original value  @return copied value or null
     * @return value copy
     * @throws org.jkiss.dbeaver.model.dbc.DBCException on error
     */
    Object copyValueObject(DBRProgressMonitor monitor, Object value)
        throws DBCException;

    /**
     * Release any internal resources associated with this value.
     * This method is called after value binding and statement execution/close.
     * @param value value
     */
    void releaseValueObject(Object value);

    /**
     * Converts value to human readable format
     * @param column column
     * @param value value  @return string representation
     * @return formatted string
     */
    String getValueDisplayString(DBSTypedObject column, Object value);

    /**
     * Returns any additional annotations of value
     * @param column column info
     * @return annotations array or null
     * @throws DBCException on error
     */
    DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column)
        throws DBCException;

    /**
     * Fills context menu for certain value
     * @param menuManager context menu manager
     * @param controller value controller
     * @throws DBCException on error
     */
    void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException;

    /**
     * Fills value's custom properties
     * @param propertySource proprty source
     * @param controller value controller
     */
    void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller);

    /**
     * Shows value editor.
     * Value editor could be:
     * <li>inline editor (control created withing inline placeholder)</li>
     * <li>dialog (modal or modeless)</li>
     * <li>workbench editor</li>
     * Modeless dialogs and editors must implement DBDValueEditor and
     * must register themselves within value controller. On close they must unregister themselves within
     * value controller.
     * @param controller value controller  @return true if editor was successfully opened.
     * makes sence only for inline editors, otherwise return value is ignored.
     * @return true on success
     * @throws org.jkiss.dbeaver.DBException on error
     */
    boolean editValue(DBDValueController controller)
        throws DBException;

}
