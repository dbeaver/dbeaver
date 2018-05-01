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
import java.util.Map;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * This interface is expected to be used in synch manner
 */
public interface DBGController {

    String ATTACH_KIND_LOCAL = "LOCAL"; //$NON-NLS-1$
    String ATTACH_KIND_GLOBAL = "GLOBAL"; //$NON-NLS-1$

    DBPDataSourceContainer getDataSourceContainer();

    Map<String, Object> getDebugConfiguration();

    /**
     * Sets debug context like OID etc.
     */
    void init(Map<String, Object> context);

    /**
     * 
     * @return key to use for <code>detach</code>
     */
    Object attach(DBRProgressMonitor monitor) throws DBGException;

    /**
     * 
     * @param sessionKey
     *            the key obtained as a result of <code>attach</code>
     */
    void detach(DBRProgressMonitor monitor, Object sessionKey) throws DBGException;

    void dispose();

    DBGBreakpointDescriptor describeBreakpoint(Map<String, Object> attributes);

    List<? extends DBGBreakpointDescriptor> getBreakpoints(Object sessionKey) throws DBGException;

    void addBreakpoint(DBRProgressMonitor monitor, Object sessionKey, DBGBreakpointDescriptor descriptor) throws DBGException;

    void removeBreakpoint(DBRProgressMonitor monitor, Object sessionKey, DBGBreakpointDescriptor descriptor) throws DBGException;

    List<? extends DBGStackFrame> getStack(Object sessionKey) throws DBGException;

    List<? extends DBGVariable<?>> getVariables(Object sessionKey, DBGStackFrame stack) throws DBGException;

    String getSource(Object sessionKey, DBGStackFrame stack) throws DBGException;

    /*
     * suspend/resume
     */
    boolean canSuspend(Object sessionKey);

    boolean canResume(Object sessionKey);

    void suspend(Object sessionKey) throws DBGException;

    void resume(Object sessionKey) throws DBGException;

    /*
     * Stepping
     */

    boolean canStepInto(Object sessionKey);

    boolean canStepOver(Object sessionKey);

    boolean canStepReturn(Object sessionKey);

    void stepInto(Object sessionKey) throws DBGException;

    void stepOver(Object sessionKey) throws DBGException;

    void stepReturn(Object sessionKey) throws DBGException;

    /*
     * Events
     */

    void registerEventHandler(DBGEventHandler eventHandler);

    void unregisterEventHandler(DBGEventHandler eventHandler);

}
