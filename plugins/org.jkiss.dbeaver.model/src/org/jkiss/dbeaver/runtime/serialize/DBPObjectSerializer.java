/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.runtime.serialize;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;

import java.util.Map;

/**
 * Object serializer
 */
public interface DBPObjectSerializer<CONTEXT_TYPE, OBJECT_TYPE> {

    void serializeObject(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull CONTEXT_TYPE context,
        @NotNull OBJECT_TYPE object,
        @NotNull Map<String, Object> state);

    OBJECT_TYPE deserializeObject(
        @NotNull DBRRunnableContext runnableContext,
        @NotNull CONTEXT_TYPE objectContext,
        @NotNull Map<String, Object> state) throws DBCException;

}
