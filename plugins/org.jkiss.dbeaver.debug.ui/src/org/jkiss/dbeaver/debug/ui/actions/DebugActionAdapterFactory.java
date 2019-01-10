/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.debug.ui.actions;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.ILaunchable;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;

public class DebugActionAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[] { ILaunchable.class, IToggleBreakpointsTarget.class };

    private static final ILaunchable LAUNCHABLE = new ILaunchable() {
    };

    private final IToggleBreakpointsTarget toggleBreakpointTarget = new ToggleProcedureBreakpointTarget();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == ILaunchable.class) {
            return (T) LAUNCHABLE;
        }
        if (adapterType == IToggleBreakpointsTarget.class) {
            return (T) toggleBreakpointTarget;
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
