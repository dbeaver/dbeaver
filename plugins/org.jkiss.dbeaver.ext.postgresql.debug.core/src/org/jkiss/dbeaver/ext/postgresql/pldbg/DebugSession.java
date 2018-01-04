/*
 * DBeaver - Universal Database Manager
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

package org.jkiss.dbeaver.ext.postgresql.pldbg;

import java.util.List;

public interface DebugSession<SESSIONINFO extends SessionInfo<?>, DEBUGOBJECT extends DebugObject<?>, SESSIONID> {
    SESSIONINFO getSessionInfo();

    String getTitle();

    List<? extends Breakpoint> getBreakpoints();

    Breakpoint setBreakpoint(DEBUGOBJECT obj, BreakpointProperties properties) throws DebugException;

    void removeBreakpoint(Breakpoint bp) throws DebugException;

    void execContinue() throws DebugException;

    void execStepInto() throws DebugException;

    void execStepOver() throws DebugException;

    void abort() throws DebugException;

    void close();

    List<? extends Variable<?>> getVarables() throws DebugException;

    void setVariableVal(Variable<?> variable, Object value) throws DebugException;

    List<? extends StackFrame> getStack() throws DebugException;

    SESSIONID getSessionId();

    // move Stack
}
