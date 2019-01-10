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

package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;

/**
 * Object with unique name.
 * Generally all objects have unique name (in context of their parent objects) but sometimes the name isn't unique.
 * For example stored procedures can be overridden, as a result multiple procedures have the same name.
 * Such objects may implements this interface to provide really unique name.
 * Unique name used in some operations like object tree refresh.
 */
public interface DBPUniqueObject extends DBPObject
{

    /**
     * Object's unique name
     *
     * @return object unique name
     */
    @NotNull
    String getUniqueName();

}
