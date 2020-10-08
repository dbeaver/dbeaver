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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPObject;

/**
 * DBSTypedObject
 */
public interface DBSTypedObject extends DBPObject
{
    // Numbers must be formatted with leading zeroes
    int TYPE_MOD_NUMBER_LEADING_ZEROES      = 1 << 10;

    // String must be formatted with trailing spaces
    int TYPE_MOD_CHAR_TRAILING_SPACES       = 1 << 12;

    /**
     * Database specific type name
     * @return type name
     */
    String getTypeName();

    /**
     * Type name with all qualifiers.
     */
    String getFullTypeName();

    /**
     * Type numeric ID.
     * (may refer on java.sql.Types or other constant depending on implementer)
     * @return value type
     */
    int getTypeID();

    /**
     * Determines kind of data for this typed object.
     * @return data kind
     */
    DBPDataKind getDataKind();

    /**
     * Value scale. Can be null if scale is not applicable/not specified to this data type.
     * @return scale
     */
    Integer getScale();

    /**
     * Value precision. Can be null if precision is not applicable/not specified to this data type.
     * @return precision
     */
    Integer getPrecision();

    /**
     * Maximum length
     * @return max length
     */
    long getMaxLength();

    /**
     * Type-specific modifiers.
     * See TYPE_MOD_ constants.
     */
    long getTypeModifiers();

}