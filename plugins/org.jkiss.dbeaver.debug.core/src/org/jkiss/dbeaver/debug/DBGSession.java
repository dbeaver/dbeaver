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

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

public interface DBGSession {

    DBGSessionInfo getSessionInfo();

    Object getSessionId();

    List<? extends DBGBreakpointDescriptor> getBreakpoints();

    void addBreakpoint(DBRProgressMonitor monitor, DBGBreakpointDescriptor descriptor) throws DBGException;

    void removeBreakpoint(DBRProgressMonitor monitor, DBGBreakpointDescriptor descriptor) throws DBGException;

    boolean canStepInto();

    boolean canStepOver();

    boolean canStepReturn();

    void execContinue() throws DBGException;

    void execStepInto() throws DBGException;

    void execStepOver() throws DBGException;

    void execStepReturn() throws DBGException;

    void resume() throws DBGException;

    void suspend() throws DBGException;

    List<? extends DBGVariable<?>> getVariables(DBGStackFrame stack) throws DBGException;

    void setVariableVal(DBGVariable<?> variable, Object value) throws DBGException;

    List<? extends DBGStackFrame> getStack() throws DBGException;

    String getSource(DBGStackFrame stack) throws DBGException;

    void closeSession(DBRProgressMonitor monitor) throws DBGException;

}
