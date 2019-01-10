/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataType;

/**
 * Reference to another object (usually DBDComposite).
 */
public interface DBDReference extends DBDComplexValue {

    DBSDataType getReferencedType();

    /**
     * Retrieves referenced object.
     * Object is retrieved in lazy way because references may point to owner objects in circular way.
     * @return referenced object
     * @throws DBCException
     */
    Object getReferencedObject(DBCSession session)
        throws DBCException;

}
