/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2017-2018 Andrew Khitrin (ahitrin@gmail.com)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug;

import java.util.EventObject;

public class DBGEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /*
     * The event kind constants
     */
    public static final int RESUME = 0x0001;
    public static final int SUSPEND = 0x0002;
    public static final int CREATE = 0x0004;
    public static final int TERMINATE = 0x0008;
    public static final int CHANGE = 0x0010;
    public static final int MODEL_SPECIFIC = 0x0020;

    /*
     * The event detail constants
     */
    public static final int UNSPECIFIED = 0;
    public static final int STEP_INTO = 0x0001;
    public static final int STEP_OVER = 0x0002;
    public static final int STEP_RETURN = 0x0004;
    public static final int STEP_END = 0x0008;
    public static final int BREAKPOINT = 0x0010;
    public static final int CLIENT_REQUEST = 0x0020;

    private int kind;

    private int details;

    public DBGEvent(Object source, int kind) {
        this(source, kind, UNSPECIFIED);
    }

    public DBGEvent(Object source, int kind, int details) {
        super(source);
        this.kind = kind;
        this.details = details;
    }

    public int getKind() {
        return kind;
    }

    public int getDetails() {
        return details;
    }

}
