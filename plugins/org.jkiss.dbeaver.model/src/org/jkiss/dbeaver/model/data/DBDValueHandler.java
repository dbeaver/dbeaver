/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * DBD Value Handler.
 * Extract, edit and bind database values.
 */
public interface DBDValueHandler 
{
    /**
     * Gets value object's type.
     * May return base interface of object's type -
     * it is not required to return exact implementation class
     * (moreover it may be unknown before certain value is extracted)
     * @param attribute value attribute
     * @return value object type
     */
    @NotNull
    Class<?> getValueObjectType(@NotNull DBSTypedObject attribute);

    /**
     * Determine value content type (MIME).
     * Most attributes do not have associated MIME type so handlers returns null.
     * However most CONTENT and DOCUMENT attributes have some content type.
     * @param attribute    value attribute
     * @return content type
     */
    @Nullable
    String getValueContentType(@NotNull DBSTypedObject attribute);

    /**
     * Extracts object from result set
     *
     * @param session session
     * @param resultSet result set
     * @param type attribute type
     * @param index attribute index (zero based)
     * @return value or null
     * @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    @Nullable
    Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index)
        throws DBCException;

    /**
     * Binds specified parameter to statement
     *
     * @param session execution context
     * @param statement statement
     * @param type attribute type
     * @param index parameter index (zero based)
     * @param value parameter value (can be null). Value is get from fetchValueObject function or from
     * object set by editor (editValue function).  @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, @Nullable Object value)
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
     * @param copy copy object
     * @return initial object value
     * @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    @Nullable
    Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy)
        throws DBCException;

    /**
     * Release any internal resources associated with this value.
     * This method is called after value binding and statement execution/close.
     * @param value value
     */
    void releaseValueObject(@Nullable Object value);

    /**
     * Converts value to human readable format
     *
     * @param column column
     * @param value value
     * @param format string format
     * @return formatted string
     */
    @NotNull
    String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format);

    DBCLogicalOperator[] getSupportedOperators(@NotNull DBDAttributeBinding attribute);

}
