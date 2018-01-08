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
package org.jkiss.dbeaver.debug.core;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;

public class DebugEvents {

    /**
     * Fires the given debug event.
     *
     * @param event debug event to fire
     */
    public static void fireEvent(DebugEvent event) {
        DebugPlugin manager = DebugPlugin.getDefault();
        if (manager != null) {
            manager.fireDebugEventSet(new DebugEvent[]{event});
        }
    }

    /**
     * Fires a terminate event.
     */
    public static void fireTerminate(Object source) {
        fireEvent(new DebugEvent(source, DebugEvent.TERMINATE));
    }

}
