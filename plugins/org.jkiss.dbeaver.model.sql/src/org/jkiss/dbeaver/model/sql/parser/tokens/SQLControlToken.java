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

package org.jkiss.dbeaver.model.sql.parser.tokens;

import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;

/**
 * SQLControlToken
 * <p>
 * Control tokens are used for local SQL script evaluation.
 */
public class SQLControlToken extends TPTokenDefault {

    private final String commandId;

    public SQLControlToken() {
        this(null);
    }

    public SQLControlToken(String commandId) {
        super(SQLTokenType.T_CONTROL);
        this.commandId = commandId;
    }

    /**
     * Command ID or null if command id is in the token itself
     */
    public String getCommandId() {
        return commandId;
    }
}
