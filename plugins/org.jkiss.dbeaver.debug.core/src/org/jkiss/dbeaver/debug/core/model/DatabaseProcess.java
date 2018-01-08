/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug.core.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.jkiss.dbeaver.debug.core.DebugEvents;

public class DatabaseProcess implements IProcess {

    private final ILaunch launch;
    private final String name;
    private final Map<String, String> attributes = new HashMap<>();

    private boolean terminated = false;

    public DatabaseProcess(ILaunch launch, String name) {
        this.launch = launch;
        this.name = name;
        launch.addProcess(this);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        IAdapterManager adapterManager = Platform.getAdapterManager();
        return adapterManager.getAdapter(this, adapter);
    }

    @Override
    public boolean canTerminate() {
        return !terminated;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void terminate() throws DebugException {
        if (!terminated) {
            terminated = true;
            launch.removeProcess(this);
            DebugEvents.fireTerminate(this);
        }
    }

    @Override
    public String getLabel() {
        return name;
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public IStreamsProxy getStreamsProxy() {
        return null;
    }

    @Override
    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    @Override
    public String getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public int getExitValue() throws DebugException {
        return 0;
    }

}
