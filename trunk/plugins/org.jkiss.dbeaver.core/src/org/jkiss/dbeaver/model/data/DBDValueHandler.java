/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.Clipboard;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

/**
 * DBD Value Handler.
 * Extract, edit and bind database values.
 */
public interface DBDValueHandler 
{

    public static final int FEATURE_NONE = 0;
    public static final int FEATURE_VIEWER = 1;
    public static final int FEATURE_EDITOR = 2;
    public static final int FEATURE_INLINE_EDITOR = 4;
    public static final int FEATURE_SHOW_ICON = 8;

    int getFeatures();

    /**
     * Gets value object's type.
     * May return base interface of object's type -
     * it is not required to return exact implementation class
     * (moreover it may be unknown before certain value is extracted)
     * @return value object type
     */
    Class getValueObjectType();

    /**
     * Extracts object from result set
     * @param context
     * @param resultSet result set
     * @param column column
     * @param columnIndex column index
     * @return value or null
     * @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    Object getValueObject(DBCExecutionContext context, DBCResultSet resultSet, DBSTypedObject column, int columnIndex)
        throws DBCException;

    /**
     * Binds specified parameter to statement
     * @param context
     * @param statement statement
     * @param columnType column type
     * @param paramIndex parameter index (starts from 0)
     * @param value parameter value (can be null). Value is get from getValueObject function or from
     * object set by editor (editValue function).
     * @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    void bindValueObject(DBCExecutionContext context, DBCStatement statement, DBSTypedObject columnType, int paramIndex, Object value)
        throws DBCException;

    /**
     * Creates new value object.
     * For simple types returns null (as initial value). For complex type may return DBDValue.
     *
     * @param context execution context
     * @param column column
     * @return initial object value
     * @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    Object createValueObject(DBCExecutionContext context, DBSTypedObject column)
        throws DBCException;

    /**
     * Makes value copy. For Non-mutable objects (like numbers and string) may return the same value as passed in.
     * If copy operation is not supported for some values then may return null.
     * @param context execution context
     * @param column column descriptor
     * @param value original value  @return copied value or null  @return value copy
     * @throws org.jkiss.dbeaver.model.exec.DBCException on error
     * @return new object copy
     */
    Object copyValueObject(DBCExecutionContext context, DBSTypedObject column, Object value)
        throws DBCException;

    /**
     * Get value from clipboard. If clipboard doesn't contain data in appropriate format
     * or value cannot be parsed then this function should return null
     *
     *
     * @param column column descriptor
     * @param clipboard clipboard
     * @return value (return null only in case of NULL value in clipboard)
     * @throws org.jkiss.dbeaver.DBException on unexpected error (IO, etc)
     */
    Object getValueFromClipboard(DBSTypedObject column, Clipboard clipboard);

    /**
     * Release any internal resources associated with this value.
     * This method is called after value binding and statement execution/close.
     * @param value value
     */
    void releaseValueObject(Object value);

    /**
     * Converts value to human readable format
     * @param column column
     * @param value value
     * @return formatted string
     */
    String getValueDisplayString(DBSTypedObject column, Object value);

    /**
     * Returns any additional annotations of value
     * @param attribute column info
     * @return annotations array or null
     * @throws DBCException on error
     */
    DBDValueAnnotation[] getValueAnnotations(DBCAttributeMetaData attribute)
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
     * @param propertySource property source
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
     * makes since only for inline editors, otherwise return value is ignored.
     * @return true on success
     * @throws org.jkiss.dbeaver.DBException on error
     */
    boolean editValue(DBDValueController controller)
        throws DBException;

}
