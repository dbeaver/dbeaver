/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
 * Named object extension
 */
public interface DBPQualifiedObject extends DBPObject
{

    /**
     * Entity full qualified name.
     * Should include all parent objects' names and thus uniquely identify this entity within database.
     * @return full qualified name, never returns null.
     * @param context evaluation context
     */
    @NotNull
    String getFullyQualifiedName(DBPEvaluationContext context);

}
