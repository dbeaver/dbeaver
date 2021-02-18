/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.debug.ui.internal;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class DebugUIActivator extends AbstractUIPlugin {

    private static DebugUIActivator activator;
    private IDebugEventSetListener eventListener;

    public static DebugUIActivator getDefault() {
        return activator;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        activator = this;

        eventListener = new DebugUIEventListener();

        DebugPlugin debugPlugin = DebugPlugin.getDefault();
        if (debugPlugin != null) {
            debugPlugin.addDebugEventListener(eventListener);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        DebugPlugin debugPlugin = DebugPlugin.getDefault();
        if (debugPlugin != null) {
            debugPlugin.removeDebugEventListener(eventListener);
        }
        activator = null;
    }

}
