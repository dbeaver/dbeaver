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
package org.jkiss.dbeaver.model.fs.nio;

import java.util.ArrayList;
import java.util.List;

/**
 * NIOContainer
 */
public abstract class NIOMonitor {

    private final static List<NIOListener> listeners = new ArrayList<>();

    public static void addListener(NIOListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public static boolean removeListener(NIOListener listener) {
        synchronized (listeners) {
            return listeners.remove(listener);
        }
    }

    public static void notifyResourceChange(NIOResource resource, NIOListener.Action action) {
        NIOListener[] lc;
        synchronized (listeners) {
            lc = listeners.toArray(new NIOListener[0]);
        }
        for (NIOListener listener : lc) {
            listener.resourceChanged(resource, action);
        }
    }

}
