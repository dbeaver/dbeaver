/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.jkiss.dbeaver.debug.internal.core.DebugCoreMessages;

public class ProcedureThread extends DatabaseThread {

    public ProcedureThread(IDatabaseDebugTarget target, Object sessionKey) {
        super(target, sessionKey);
    }

    @Override
    public String getName() throws DebugException {
        String name = DebugCoreMessages.ProcedureThread_name;
        return name;
    }

}
