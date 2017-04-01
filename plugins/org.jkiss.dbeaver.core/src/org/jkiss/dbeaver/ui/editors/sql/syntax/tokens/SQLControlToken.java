/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.editors.sql.syntax.tokens;

import org.eclipse.jface.text.rules.Token;

/**
 * SQLControlToken
 *
 * Control tokens are used for local SQL script evaluation.
 */
public class SQLControlToken extends Token {

    public static final String DEFAULT_PREFIX = "@";

    public static final String COMMAND_SET = "set";
    public static final String COMMAND_PARAM = "param";
    public static final String COMMAND_INCLUDE = "include";
    public static final String COMMAND_ECHO = "echo";

    public static final String[] COMMANDS = {
            COMMAND_SET,
            COMMAND_PARAM,
            COMMAND_INCLUDE,
            COMMAND_ECHO
    };

    public SQLControlToken(Object data)
    {
        super(data);
    }

}
