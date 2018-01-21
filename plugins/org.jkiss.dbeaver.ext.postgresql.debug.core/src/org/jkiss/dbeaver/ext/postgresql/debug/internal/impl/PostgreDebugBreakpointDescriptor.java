/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.debug.internal.impl;

import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.debug.DBGException;

@SuppressWarnings("nls")
public class PostgreDebugBreakpointDescriptor implements DBGBreakpointDescriptor {

    private final PostgreDebugObjectDescriptor obj;

    private final PostgreDebugBreakpointProperties properties;

    public PostgreDebugBreakpointDescriptor(PostgreDebugObjectDescriptor obj, PostgreDebugBreakpointProperties properties) throws DBGException {
        this.obj = obj;
        this.properties = properties;

    }

    @Override
    public PostgreDebugObjectDescriptor getObjectDescriptor() {
        return obj;
    }

    @Override
    public PostgreDebugBreakpointProperties getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "PostgreDebugBreakpointDescriptor [obj=" + obj + ", properties=" + properties + "]";
    }

}
