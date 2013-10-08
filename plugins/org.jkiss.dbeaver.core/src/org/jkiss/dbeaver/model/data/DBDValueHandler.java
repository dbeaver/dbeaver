/*
 * Copyright (C) 2010-2013 Serge Rieder
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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

/**
 * DBD Value Handler.
 * Extract, edit and bind database values.
 */
public interface DBDValueHandler 
{
    // Default value, means no features are supported
    public static final int FEATURE_NONE = 0;
    // VIEWER is ability to render value editor in separate panel
    public static final int FEATURE_VIEWER = 1;
    // EDITOR is ability to show value editor in separate dialog or standalone editor
    public static final int FEATURE_EDITOR = 2;
    // INLINE_EDITOR is ability to show editor in grid cell
    public static final int FEATURE_INLINE_EDITOR = 4;
    // SHOW_ICON means grid should render type icon before cell value
    public static final int FEATURE_SHOW_ICON = 8;

    /**
     * Handler features. Bit set.
     * See constants FEATURE_*
     * @return features bits
     */
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
     *
     * @param session
     * @param resultSet result set
     * @param type
     *@param index @return value or null
     * @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    Object fetchValueObject(DBCSession session, DBCResultSet resultSet, DBSTypedObject type, int index)
        throws DBCException;

    /**
     * Binds specified parameter to statement
     *
     * @param session execution context
     * @param statement statement
     * @param type attribute type
     * @param index parameter index
     * @param value parameter value (can be null). Value is get from fetchValueObject function or from
     * object set by editor (editValue function).  @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    void bindValueObject(DBCSession session, DBCStatement statement, DBSTypedObject type, int index, Object value)
        throws DBCException;

    /**
     * Creates new value from object.
     * Must analyse passed object and convert it (if possible) to appropriate handler's type.
     * For null objects returns null of DBDValue marked as null
     *
     *
     * @param session execution context
     * @param type attribute type
     * @param object source object
     * @param copy
     * @return initial object value
     * @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    Object getValueFromObject(DBCSession session, DBSTypedObject type, Object object, boolean copy)
        throws DBCException;

    /**
     * Get value from clipboard. If clipboard doesn't contain data in appropriate format
     * or value cannot be parsed then this function should return null
     *
     *
     *
     * @param session
     * @param column column descriptor
     * @param clipboard clipboard
     * @return value (return null only in case of NULL value in clipboard)
     * @throws org.jkiss.dbeaver.DBException on unexpected error (IO, etc)
     */
    Object getValueFromClipboard(DBCSession session, DBSTypedObject column, Clipboard clipboard) throws DBCException;

    /**
     * Release any internal resources associated with this value.
     * This method is called after value binding and statement execution/close.
     * @param value value
     */
    void releaseValueObject(Object value);

    /**
     * Converts value to human readable format
     *
     * @param column column
     * @param value value
     * @param format
     * @return formatted string
     */
    String getValueDisplayString(DBSTypedObject column, Object value, DBDDisplayFormat format);

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
    DBDValueEditor createEditor(DBDValueController controller)
        throws DBException;

}
