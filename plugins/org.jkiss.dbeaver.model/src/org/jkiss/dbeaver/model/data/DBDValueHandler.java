/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public interface DBDValueHandler extends DBDValueRenderer
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
     * @param validateValue if true then input value will be validated and exception will be thrown on error (e.g. when number cannot be parsed). Otherwise invalid object value will be converted to null.
     * @return initial object value
     * @throws org.jkiss.dbeaver.model.exec.DBCException on error
     */
    @Nullable
    Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue)
        throws DBCException;

    /**
     * Creates new value object.
     * Can be used to create new arrays, structs and other composite objects.
     */
    @Nullable
    Object createNewValueObject(@NotNull DBCSession session, @NotNull DBSTypedObject type)
        throws DBCException;

    /**
     * Release any internal resources associated with this value.
     * This method is called after value binding and statement execution/close.
     * @param value value
     */
    void releaseValueObject(@Nullable Object value);

    /**
     * List of logical operation supported by underlying values
     * @param attribute    attribute
     * @return operations
     */
    @NotNull
    DBCLogicalOperator[] getSupportedOperators(@NotNull DBSTypedObject attribute);

}
