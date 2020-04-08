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

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * Object reorderer.
 * Provide object's reorder functions
 */
public interface DBEObjectReorderer<OBJECT_TYPE extends DBSObject> extends DBEObjectManager<OBJECT_TYPE> {

    int getMinimumOrdinalPosition(OBJECT_TYPE object);

    int getMaximumOrdinalPosition(OBJECT_TYPE object);

    /**
     * Changes object ordinal position
     *
     * @param commandContext command context. Implementation should add new command to it.
     * @param object object
     * @param siblingObjects
     *@param newPosition new position  @throws DBException on any error
     */
    void setObjectOrdinalPosition(DBECommandContext commandContext, OBJECT_TYPE object, List<OBJECT_TYPE> siblingObjects, int newPosition)
        throws DBException;

}