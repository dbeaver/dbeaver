/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

import java.util.List;

public interface DBGSession {

    DBGSessionInfo getSessionInfo();

    List<? extends DBGBreakpointDescriptor> getBreakpoints();

    void addBreakpoint(DBGBreakpointDescriptor descriptor) throws DBGException;

    void removeBreakpoint(DBGBreakpointDescriptor descriptor) throws DBGException;

    void execContinue() throws DBGException;

    void execStepInto() throws DBGException;

    void execStepOver() throws DBGException;

    void abort() throws DBGException;

    void close();

    List<? extends DBGVariable<?>> getVariables() throws DBGException;

    void setVariableVal(DBGVariable<?> variable, Object value) throws DBGException;

    List<? extends DBGStackFrame> getStack() throws DBGException;

    Object getSessionId();

    // move Stack
}
