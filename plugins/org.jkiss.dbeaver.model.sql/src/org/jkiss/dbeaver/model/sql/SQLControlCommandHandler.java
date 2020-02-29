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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.dbeaver.DBException;

/**
 * Control command handler
 */
public interface SQLControlCommandHandler
{
    /**
     *
     * @param command       command
     * @param scriptContext script context
     * @return false if command failed and execution has to be stopped
     */
    boolean handleCommand(SQLControlCommand command, SQLScriptContext scriptContext)
        throws DBException;

}
