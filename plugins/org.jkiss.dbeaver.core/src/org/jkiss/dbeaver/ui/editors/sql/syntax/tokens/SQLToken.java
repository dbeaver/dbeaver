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
 * SQLToken
 */
public class SQLToken extends Token {

    public static final int T_UNKNOWN = 1000;

    public static final int T_BLOCK_BEGIN = 1001;
    public static final int T_BLOCK_END = 1002;
    public static final int T_BLOCK_TOGGLE = 1003;
    public static final int T_BLOCK_HEADER = 1004;

    public static final int T_COMMENT = 1005;
    public static final int T_CONTROL = 1006;
    public static final int T_DELIMITER = 1007;
    public static final int T_SET_DELIMITER = 1008;
    public static final int T_PARAMETER = 1009;

    private final int type;
    
    public SQLToken(int type, Object data)
    {
        super(data);
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
