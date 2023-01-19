/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.rm;

import org.jkiss.dbeaver.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Resource event manager.
 */
public class RMEventManager {
    private static final Log log = Log.getLog(RMEventManager.class);

    private static final List<RMEventListener> listeners = new CopyOnWriteArrayList<>();

    public static synchronized void addEventListener(RMEventListener listener) {
        listeners.add(listener);
    }

    public static synchronized void removeEventListener(RMEventListener listener) {
        listeners.remove(listener);
    }

    public static void fireEvent(RMEvent event) {
        for (var listener : listeners) {
            try {
                listener.handleRMEvent(event);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
