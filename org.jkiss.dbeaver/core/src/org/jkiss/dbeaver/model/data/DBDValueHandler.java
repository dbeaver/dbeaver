/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.views.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.eclipse.jface.action.IMenuManager;

/**
 * DBD Value Handler.
 * Extract, edit and bind database values.
 */
public interface DBDValueHandler
{
    /**
     * Extract string representation of spcified value
     * @param resultSet result set
     * @param columnType
     *@param columnIndex column index  @return string
     * @throws DBCException on error
     */
    Object getValueObject(DBCResultSet resultSet, DBSTypedObject columnType, int columnIndex)
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
     * Fills context menu for certain value
     * @param menuManager context menu manager
     * @throws DBCException on error
     */
    void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException;

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
     * @param controller value controller
     * @return true if editor was successfully opened.
     * makes sence only for inline editors, otherwise return value is ignored.
     * @throws org.jkiss.dbeaver.DBException on error
     */
    boolean editValue(DBDValueController controller)
        throws DBException;

}